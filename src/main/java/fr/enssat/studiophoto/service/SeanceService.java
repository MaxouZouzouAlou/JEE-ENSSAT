package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.SeanceDAO;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.exception.RessourceIntrouvableException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.ClientParticulier;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.StatutSeance;
import fr.enssat.studiophoto.model.TypeClient;
import fr.enssat.studiophoto.model.TypeSeance;

import java.sql.Date;
import java.sql.Time;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Règles de gestion des séances (lot 3).
 *
 * <ul>
 *   <li>Une séance se planifie dans le futur, avec un photographe actif, un type et un client existants.</li>
 *   <li>Un photographe ne peut pas avoir deux séances qui se chevauchent (séances annulées ignorées,
 *       séances bout à bout autorisées).</li>
 *   <li>Transitions de statut : celles de {@link StatutSeance#peutPasserA}, TERMINEE seulement
 *       une fois l'heure de fin passée ; TERMINEE et ANNULEE sont définitifs.</li>
 *   <li>Une séance qui a une facture ne peut pas être supprimée.</li>
 * </ul>
 *
 * <p>Les données reçues sont des chaînes (format envoyé par le front) ; elles sont contrôlées ici,
 * et toute erreur lève une exception métier convertie en réponse 400, 404 ou 409.</p>
 */
public class SeanceService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH'h'mm");
    private static final int LIEU_MAX = 255;

    private final SeanceDAO seanceDAO;
    private final Clock horloge;

    /** Service branché sur la base de données (utilisé par la ressource REST). */
    public SeanceService() {
        this(new SeanceDAO(), Clock.systemDefaultZone());
    }

    /** Service avec un DAO et une horloge au choix (utilisé par les tests). */
    public SeanceService(SeanceDAO seanceDAO, Clock horloge) {
        this.seanceDAO = seanceDAO;
        this.horloge = horloge;
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    /**
     * Planning : séances triées par date et heure, filtrées si besoin.
     * {@code dateDebut} et {@code dateFin} (AAAA-MM-JJ, incluses) vont ensemble.
     */
    public List<Seance> lister(Long id, Long photographeId, String dateDebut, String dateFin) {
        if ((dateDebut == null) != (dateFin == null)) {
            throw new DonneesInvalidesException("Les paramètres dateDebut et dateFin doivent être fournis ensemble.");
        }
        Date debut = null;
        Date fin = null;
        if (dateDebut != null) {
            LocalDate d = lireDate(dateDebut, "dateDebut");
            LocalDate f = lireDate(dateFin, "dateFin");
            if (d.isAfter(f)) {
                throw new DonneesInvalidesException("La date de début doit précéder la date de fin.");
            }
            debut = Date.valueOf(d);
            fin = Date.valueOf(f);
        }
        return seanceDAO.findByFilters(id, photographeId, debut, fin);
    }

    public Seance trouver(long id) {
        List<Seance> seances = seanceDAO.findByFilters(id, null, null, null);
        if (seances.isEmpty()) {
            throw new RessourceIntrouvableException("Séance", id);
        }
        return seances.get(0);
    }

    /** Listes des formulaires : types de séance, photographes actifs et clients. */
    public Map<String, Object> options() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (TypeSeance type : seanceDAO.findTypes()) {
            types.add(Map.of("id", type.getId(), "libelle", type.getLibelle()));
        }
        List<Map<String, Object>> photographes = new ArrayList<>();
        for (Photographe photographe : seanceDAO.findPhotographesActifs()) {
            photographes.add(Map.of("id", photographe.getId(), "nom", photographe.getNom()));
        }
        List<Map<String, Object>> clients = new ArrayList<>();
        for (Client client : seanceDAO.findClients()) {
            clients.add(Map.of("id", client.getId(), "type", client.getType(), "designation", client.designation()));
        }
        return Map.of("typesSeance", types, "photographes", photographes, "clients", clients);
    }

    // ------------------------------------------------------------------
    // Création et modification
    // ------------------------------------------------------------------

    /** Crée une séance au statut PLANIFIEE et la renvoie complète. */
    public Seance creer(Map<String, String> donnees) {
        Saisie saisie = lire(donnees, false);
        TypeSeance type = typeExistant(saisie.typeSeanceId);
        Client client = clientExistant(saisie.clientId);
        photographeActif(saisie.photographeId);
        verifierDansLeFutur(saisie.debut());
        verifierDisponibilite(saisie, null);

        Seance creee = seanceDAO.create(Date.valueOf(saisie.date), Time.valueOf(saisie.heure), saisie.duree,
                saisie.lieu, type, new Photographe(saisie.photographeId, null, null, true), client);
        if (creee == null) {
            throw new ConflitException("La séance n'a pas pu être créée. Vérifiez le photographe et les références sélectionnées.");
        }
        return trouver(creee.getId());
    }

    /**
     * Remplace tous les champs d'une séance. Refusé pour une séance TERMINEE ou ANNULEE.
     * Si la date ou l'heure change, le nouveau créneau doit être dans le futur ; si le créneau
     * ou le photographe change, celui-ci doit être disponible.
     */
    public Seance modifier(long id, Map<String, String> donnees) {
        Seance actuelle = trouver(id);
        if (estDefinitive(actuelle.getStatut())) {
            throw new ConflitException("Une séance " + libelle(actuelle.getStatut()) + " ne peut plus être modifiée.");
        }
        Saisie saisie = lire(donnees, true);
        TypeSeance type = typeExistant(saisie.typeSeanceId);
        Client client = clientExistant(saisie.clientId);

        boolean horaireChange = !saisie.debut().equals(actuelle.calculerDebut());
        boolean creneauChange = horaireChange || saisie.duree != actuelle.getDuree();
        boolean photographeChange = !saisie.photographeId.equals(actuelle.getPhotographe().getId());

        if (saisie.statut != actuelle.getStatut()) {
            verifierTransition(actuelle.getStatut(), saisie.statut, saisie.fin());
        }
        if (photographeChange) {
            photographeActif(saisie.photographeId);
        }
        if (horaireChange) {
            verifierDansLeFutur(saisie.debut());
        }
        if (saisie.statut != StatutSeance.ANNULEE && (creneauChange || photographeChange)) {
            verifierDisponibilite(saisie, id);
        }

        Seance miseAJour = seanceDAO.update(id, Date.valueOf(saisie.date), Time.valueOf(saisie.heure), saisie.duree,
                saisie.lieu, saisie.statut, type, new Photographe(saisie.photographeId, null, null, true), client);
        if (miseAJour == null) {
            throw new ConflitException("La séance n'a pas pu être mise à jour.");
        }
        return trouver(id);
    }

    // ------------------------------------------------------------------
    // Statut et suppression
    // ------------------------------------------------------------------

    /** Change le statut d'une séance en respectant les transitions autorisées. */
    public Seance changerStatut(long id, String statut) {
        if (statut == null || statut.isBlank()) {
            throw new DonneesInvalidesException("Le champ statut est obligatoire.");
        }
        StatutSeance cible = lireStatut(statut);
        Seance seance = trouver(id);
        verifierTransition(seance.getStatut(), cible, seance.calculerFin());

        Seance miseAJour = seanceDAO.update(id, Date.valueOf(seance.getDate()), Time.valueOf(seance.getHeureDebut()),
                seance.getDuree(), seance.getLieu(), cible, seance.getTypeSeance(), seance.getPhotographe(), seance.getClient());
        if (miseAJour == null) {
            throw new ConflitException("La séance n'a pas pu être mise à jour.");
        }
        return trouver(id);
    }

    /** Supprime une séance, sauf si elle a une facture (la facture doit rester rattachée à sa prestation). */
    public void supprimer(long id) {
        trouver(id);
        if (!seanceDAO.delete(id)) {
            throw new ConflitException("La séance " + id + " a une facture : elle ne peut pas être supprimée.");
        }
    }

    // ------------------------------------------------------------------
    // Règles
    // ------------------------------------------------------------------

    private void verifierDansLeFutur(LocalDateTime debut) {
        if (!debut.isAfter(maintenant())) {
            throw new DonneesInvalidesException("Impossible de planifier une séance dans le passé (" + debut.format(JOUR)
                    + " à " + debut.format(HEURE) + ").");
        }
    }

    private void verifierDisponibilite(Saisie saisie, Long seanceExclue) {
        if (seanceDAO.existeChevauchement(saisie.photographeId, saisie.debut(), saisie.fin(), seanceExclue)) {
            throw new ConflitException("Le photographe a déjà une séance sur ce créneau (" + saisie.debut().format(JOUR)
                    + ", " + saisie.debut().format(HEURE) + " – " + saisie.fin().format(HEURE) + ").");
        }
    }

    private void verifierTransition(StatutSeance depuis, StatutSeance vers, LocalDateTime finSeance) {
        if (depuis.peutPasserA(vers, finSeance, maintenant())) {
            return;
        }
        if (depuis == StatutSeance.CONFIRMEE && vers == StatutSeance.TERMINEE) {
            throw new ConflitException("La séance ne pourra être terminée qu'après sa fin, le " + finSeance.format(JOUR)
                    + " à " + finSeance.format(HEURE) + ".");
        }
        throw new ConflitException("Une séance " + libelle(depuis) + " ne peut pas passer à " + libelle(vers) + ".");
    }

    private TypeSeance typeExistant(long id) {
        return seanceDAO.findTypes().stream().filter(t -> t.getId() == id).findFirst()
                .orElseThrow(() -> new DonneesInvalidesException("Type de séance " + id + " inconnu."));
    }

    /** Client tel qu'enregistré : son type réel prime sur le champ clientType reçu. */
    private Client clientExistant(long id) {
        Optional<Client> client = seanceDAO.findClients().stream().filter(c -> c.getId() == id).findFirst();
        Client trouve = client.orElseThrow(() -> new DonneesInvalidesException("Client " + id + " inconnu."));
        return trouve.getType() == TypeClient.ENTREPRISE
                ? new ClientEntreprise(id, null, null, null, null)
                : new ClientParticulier(id, null, null, null, null);
    }

    private void photographeActif(long id) {
        boolean actif = seanceDAO.findPhotographesActifs().stream().anyMatch(p -> p.getId() == id);
        if (!actif) {
            throw new ConflitException("Le photographe " + id + " est inactif ou inconnu : il ne peut pas être affecté à une séance.");
        }
    }

    private static boolean estDefinitive(StatutSeance statut) {
        return statut == StatutSeance.TERMINEE || statut == StatutSeance.ANNULEE;
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }

    // ------------------------------------------------------------------
    // Lecture des données reçues
    // ------------------------------------------------------------------

    /** Champs d'une séance, contrôlés. */
    private record Saisie(LocalDate date, LocalTime heure, int duree, String lieu, Long typeSeanceId,
                          Long photographeId, Long clientId, StatutSeance statut) {
        LocalDateTime debut() {
            return LocalDateTime.of(date, heure);
        }

        LocalDateTime fin() {
            return debut().plusMinutes(duree);
        }
    }

    private static Saisie lire(Map<String, String> donnees, boolean avecStatut) {
        if (donnees == null) {
            throw new DonneesInvalidesException("Les données de la séance sont obligatoires.");
        }
        LocalDate date = lireDate(obligatoire(donnees, "date"), "date");
        LocalTime heure;
        try {
            heure = LocalTime.parse(obligatoire(donnees, "heureDebut").trim());
        } catch (DateTimeParseException e) {
            throw new DonneesInvalidesException("L'heure de début n'est pas valide (format attendu : HH:MM).");
        }
        int duree = (int) entierPositif(donnees, "duree");
        String lieu = obligatoire(donnees, "lieu").trim();
        if (lieu.length() > LIEU_MAX) {
            throw new DonneesInvalidesException("Le lieu ne doit pas dépasser " + LIEU_MAX + " caractères.");
        }
        long typeSeanceId = entierPositif(donnees, "typeSeanceId");
        long photographeId = entierPositif(donnees, "photographeId");
        long clientId = entierPositif(donnees, "clientId");
        StatutSeance statut = avecStatut ? lireStatut(obligatoire(donnees, "statut")) : StatutSeance.PLANIFIEE;
        return new Saisie(date, heure, duree, lieu, typeSeanceId, photographeId, clientId, statut);
    }

    private static String obligatoire(Map<String, String> donnees, String champ) {
        String valeur = donnees.get(champ);
        if (valeur == null || valeur.isBlank()) {
            throw new DonneesInvalidesException("Le champ « " + champ + " » est obligatoire.");
        }
        return valeur;
    }

    private static long entierPositif(Map<String, String> donnees, String champ) {
        String valeur = obligatoire(donnees, champ).trim();
        long nombre;
        try {
            nombre = Long.parseLong(valeur);
        } catch (NumberFormatException e) {
            throw new DonneesInvalidesException("Le champ « " + champ + " » doit être un nombre entier.");
        }
        if (nombre <= 0 || nombre > Integer.MAX_VALUE) {
            throw new DonneesInvalidesException("Le champ « " + champ + " » doit être positif.");
        }
        return nombre;
    }

    private static LocalDate lireDate(String valeur, String champ) {
        try {
            return LocalDate.parse(valeur.trim());
        } catch (DateTimeParseException e) {
            throw new DonneesInvalidesException("Le champ « " + champ + " » n'est pas une date valide (format attendu : AAAA-MM-JJ).");
        }
    }

    private static StatutSeance lireStatut(String valeur) {
        try {
            return StatutSeance.valueOf(valeur.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DonneesInvalidesException("Le statut « " + valeur + " » est inconnu.");
        }
    }

    private static String libelle(StatutSeance statut) {
        return switch (statut) {
            case PLANIFIEE -> "planifiée";
            case CONFIRMEE -> "confirmée";
            case TERMINEE -> "terminée";
            case ANNULEE -> "annulée";
        };
    }
}
