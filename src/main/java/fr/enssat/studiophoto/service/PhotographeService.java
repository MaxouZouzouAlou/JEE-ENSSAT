package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.PhotographeDAO;
import fr.enssat.studiophoto.dao.PhotographeDAOImpl;
import fr.enssat.studiophoto.dao.SeanceDAO;
import fr.enssat.studiophoto.dao.SeanceDAOImpl;
import fr.enssat.studiophoto.dao.SpecialiteDAO;
import fr.enssat.studiophoto.dao.SpecialiteDAOImpl;
import fr.enssat.studiophoto.dto.ResultatChangementStatut;
import fr.enssat.studiophoto.exception.ConfirmationRequiseException;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.exception.RessourceIntrouvableException;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.model.Specialite;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Règles de gestion des photographes et de leurs spécialités (lot 1).
 *
 * <ul>
 *   <li>Nom obligatoire ; email obligatoire, valide et unique (sans tenir compte de la casse).</li>
 *   <li>Spécialités choisies dans la liste fixe (Reportage, Mariage et fêtes, Famille), plusieurs possibles.</li>
 *   <li>Désactivation : si le photographe a des séances à venir, elle doit être confirmée,
 *       et ces séances passent alors en ANNULEE. Les séances passées ne changent pas.</li>
 *   <li>Suppression refusée dès que le photographe a au moins une séance (historique, factures) :
 *       on propose la désactivation à la place.</li>
 * </ul>
 */
public class PhotographeService {

    static final int NOM_MAX = 100;
    static final int EMAIL_MAX = 150;

    /** Format d'email volontairement simple : texte@domaine.extension, sans espace. */
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private final PhotographeDAO photographeDAO;
    private final SpecialiteDAO specialiteDAO;
    private final SeanceDAO seanceDAO;
    private final Clock horloge;

    /** Service branché sur la base de données (utilisé par les ressources REST). */
    public PhotographeService() {
        this(new PhotographeDAOImpl(), new SpecialiteDAOImpl(), new SeanceDAOImpl(), Clock.systemDefaultZone());
    }

    /** Service avec des DAO et une horloge au choix (utilisé par les tests). */
    public PhotographeService(PhotographeDAO photographeDAO, SpecialiteDAO specialiteDAO,
                              SeanceDAO seanceDAO, Clock horloge) {
        this.photographeDAO = photographeDAO;
        this.specialiteDAO = specialiteDAO;
        this.seanceDAO = seanceDAO;
        this.horloge = horloge;
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    /** @param actif filtre facultatif : {@code true} actifs, {@code false} inactifs, {@code null} tous */
    public List<Photographe> lister(Boolean actif) {
        return photographeDAO.lister(actif);
    }

    public Photographe trouver(long id) {
        return photographeDAO.trouverParId(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Photographe " + id + " introuvable."));
    }

    public List<Specialite> listerSpecialites() {
        return specialiteDAO.lister();
    }

    // ------------------------------------------------------------------
    // Création et modification (1.1, 1.2)
    // ------------------------------------------------------------------

    /** Crée un photographe, toujours actif. */
    public Photographe creer(Photographe donnees) {
        Photographe photographe = valider(donnees, null);
        photographe.setActif(true);
        return photographeDAO.creer(photographe);
    }

    /** Modifie nom, email et spécialités. Le statut actif ne change que par {@link #changerStatut}. */
    public Photographe modifier(long id, Photographe donnees) {
        Photographe existant = trouver(id);
        Photographe photographe = valider(donnees, id);
        photographe.setId(id);
        photographe.setActif(existant.isActif());
        photographeDAO.modifier(photographe);
        return trouver(id);
    }

    // ------------------------------------------------------------------
    // Activation / désactivation (1.3)
    // ------------------------------------------------------------------

    /**
     * Active ou désactive un photographe.
     *
     * <p>Désactivation d'un photographe qui a des séances à venir : sans confirmation, refus
     * ({@link ConfirmationRequiseException}, 409, avec le nombre de séances) et rien ne change ;
     * avec confirmation, le photographe est désactivé et ses séances à venir passent en ANNULEE.</p>
     *
     * @param actif     nouveau statut (obligatoire)
     * @param confirmer {@code true} si l'utilisateur a accepté l'annulation des séances à venir
     */
    public ResultatChangementStatut changerStatut(long id, Boolean actif, boolean confirmer) {
        if (actif == null) {
            throw new DonneesInvalidesException("Le champ « actif » (true ou false) est obligatoire.");
        }
        Photographe photographe = trouver(id);

        if (actif) {
            if (!photographe.isActif()) {
                photographeDAO.changerActif(id, true);
            }
            return new ResultatChangementStatut(trouver(id), 0);
        }

        LocalDateTime maintenant = LocalDateTime.now(horloge);
        int aVenir = seanceDAO.compterAVenirParPhotographe(id, maintenant);
        if (aVenir > 0 && !confirmer) {
            throw new ConfirmationRequiseException(photographe.getNom() + " a " + aVenir
                    + (aVenir > 1 ? " séances à venir. Elles seront annulées" : " séance à venir. Elle sera annulée")
                    + " si vous confirmez la désactivation.", aVenir);
        }
        // Désactiver d'abord : plus aucune nouvelle séance ne peut lui être affectée pendant l'annulation.
        if (photographe.isActif()) {
            photographeDAO.changerActif(id, false);
        }
        int annulees = aVenir > 0 ? seanceDAO.annulerAVenirParPhotographe(id, maintenant) : 0;
        return new ResultatChangementStatut(trouver(id), annulees);
    }

    // ------------------------------------------------------------------
    // Suppression (1.4)
    // ------------------------------------------------------------------

    /**
     * Supprime un photographe qui n'a aucune séance. S'il en a (même passées ou annulées),
     * la suppression est refusée (409) : elle effacerait l'historique du planning et des factures.
     */
    public void supprimer(long id) {
        Photographe photographe = trouver(id);
        int seances = seanceDAO.compterParPhotographe(id);
        if (seances > 0) {
            throw new ConflitException("Impossible de supprimer " + photographe.getNom() + " : "
                    + (seances > 1 ? seances + " séances lui sont rattachées" : "une séance lui est rattachée")
                    + (photographe.isActif() ? ". Désactivez-le plutôt."
                                             : ". Il reste désactivé pour conserver son historique."));
        }
        photographeDAO.supprimer(id);
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /**
     * Contrôle les données reçues et renvoie un nouveau photographe nettoyé
     * (espaces retirés, email en minuscules, spécialités complètes et sans doublon).
     *
     * @param idExclu photographe en cours de modification (il peut garder son propre email)
     */
    private Photographe valider(Photographe donnees, Long idExclu) {
        if (donnees == null) {
            throw new DonneesInvalidesException("Les données du photographe sont obligatoires.");
        }

        String nom = donnees.getNom() == null ? "" : donnees.getNom().trim();
        if (nom.isEmpty()) {
            throw new DonneesInvalidesException("Le nom est obligatoire.");
        }
        if (nom.length() > NOM_MAX) {
            throw new DonneesInvalidesException("Le nom ne doit pas dépasser " + NOM_MAX + " caractères.");
        }

        String email = donnees.getEmail() == null ? "" : donnees.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            throw new DonneesInvalidesException("L'email est obligatoire.");
        }
        if (email.length() > EMAIL_MAX || !EMAIL.matcher(email).matches()) {
            throw new DonneesInvalidesException("L'email « " + email + " » n'est pas valide.");
        }
        photographeDAO.trouverParEmail(email)
                .filter(autre -> !autre.getId().equals(idExclu))
                .ifPresent(autre -> {
                    throw new ConflitException("L'email " + email + " est déjà utilisé par " + autre.getNom() + ".");
                });

        Photographe photographe = new Photographe(null, nom, email, true);
        photographe.setSpecialites(validerSpecialites(donnees.getSpecialites()));
        return photographe;
    }

    /** Vérifie que chaque spécialité demandée existe (par son id) et retire les doublons. */
    private List<Specialite> validerSpecialites(List<Specialite> demandees) {
        if (demandees == null || demandees.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Specialite> existantes = specialiteDAO.lister().stream()
                .collect(Collectors.toMap(Specialite::getId, Function.identity()));
        Map<Long, Specialite> retenues = new LinkedHashMap<>();
        for (Specialite s : demandees) {
            if (s == null || s.getId() == null) {
                throw new DonneesInvalidesException("Chaque spécialité doit être donnée par son identifiant, ex. {\"id\": 1}.");
            }
            Specialite existante = existantes.get(s.getId());
            if (existante == null) {
                throw new DonneesInvalidesException("Spécialité " + s.getId() + " inconnue.");
            }
            retenues.put(existante.getId(), existante);
        }
        return new ArrayList<>(retenues.values());
    }
}
