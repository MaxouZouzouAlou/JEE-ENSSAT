package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.ClientDAO;
import fr.enssat.studiophoto.dao.ClientDAOImpl;
import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.exception.RessourceIntrouvableException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.ClientParticulier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Gestion des clients : lecture, création, modification et suppression,
 * avec validation des champs selon le type de client.
 *
 * <ul>
 *   <li>tous les clients : adresse postale et email obligatoires, email bien formé ;</li>
 *   <li>particulier : nom et prénom obligatoires ;</li>
 *   <li>entreprise : raison sociale et numéro SIREN/TVA obligatoires, numéro bien formé.</li>
 * </ul>
 */
public class ClientService {

    /** Forme simple d'un email : texte@domaine.extension, sans espace. */
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    /** SIREN : 9 chiffres. */
    private static final Pattern SIREN = Pattern.compile("^\\d{9}$");
    /** TVA intracommunautaire française : FR + clé de 2 caractères + SIREN. */
    private static final Pattern TVA_FR = Pattern.compile("^FR[0-9A-Z]{2}\\d{9}$");

    /** Tailles maximales des colonnes de la table client. */
    private static final int MAX_ADRESSE = 255;
    private static final int MAX_EMAIL = 150;
    private static final int MAX_NOM = 100;
    private static final int MAX_RAISON_SOCIALE = 150;

    private final ClientDAO clientDAO;

    public ClientService() {
        this(new ClientDAOImpl());
    }

    /** Constructeur utilisé par les tests pour fournir un DAO simulé. */
    public ClientService(ClientDAO clientDAO) {
        this.clientDAO = clientDAO;
    }

    public List<Client> lister() {
        return clientDAO.findAll();
    }

    public Client trouver(long id) {
        return clientDAO.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Client", id));
    }

    public Client creer(Client client) {
        normaliserEtValider(client);
        client.setId(null);
        return clientDAO.create(client);
    }

    /** Remplace le client {@code id} ; le type peut changer (particulier <-> entreprise). */
    public Client modifier(long id, Client client) {
        normaliserEtValider(client);
        client.setId(id);
        if (!clientDAO.update(client)) {
            throw new RessourceIntrouvableException("Client", id);
        }
        return client;
    }

    public void supprimer(long id) {
        if (!clientDAO.delete(id)) {
            throw new RessourceIntrouvableException("Client", id);
        }
    }

    /**
     * Supprime les espaces superflus puis vérifie les champs. Toutes les erreurs
     * sont regroupées dans un seul message pour que le formulaire les affiche d'un coup.
     */
    void normaliserEtValider(Client client) {
        if (client == null) {
            throw new DonneesInvalidesException("Les données du client sont absentes.");
        }
        List<String> erreurs = new ArrayList<>();

        client.setAdressePostale(nettoyer(client.getAdressePostale()));
        client.setEmail(nettoyer(client.getEmail()));
        verifierObligatoire(client.getAdressePostale(), "L'adresse postale", MAX_ADRESSE, erreurs);
        verifierObligatoire(client.getEmail(), "L'email", MAX_EMAIL, erreurs);
        if (client.getEmail() != null && !EMAIL.matcher(client.getEmail()).matches()) {
            erreurs.add("L'email n'est pas valide.");
        }

        switch (client) {
            case ClientParticulier p -> {
                p.setNom(nettoyer(p.getNom()));
                p.setPrenom(nettoyer(p.getPrenom()));
                verifierObligatoire(p.getNom(), "Le nom", MAX_NOM, erreurs);
                verifierObligatoire(p.getPrenom(), "Le prénom", MAX_NOM, erreurs);
            }
            case ClientEntreprise e -> {
                e.setRaisonSociale(nettoyer(e.getRaisonSociale()));
                e.setSirenTva(normaliserSirenTva(e.getSirenTva()));
                verifierObligatoire(e.getRaisonSociale(), "La raison sociale", MAX_RAISON_SOCIALE, erreurs);
                if (e.getSirenTva() == null) {
                    erreurs.add("Le numéro SIREN/TVA est obligatoire.");
                } else if (!estSirenOuTvaValide(e.getSirenTva())) {
                    erreurs.add("Le numéro SIREN/TVA est invalide : 9 chiffres (SIREN) "
                            + "ou FR suivi de 11 caractères (TVA intracommunautaire).");
                }
            }
            default -> erreurs.add("Type de client inconnu.");
        }

        if (!erreurs.isEmpty()) {
            throw new DonneesInvalidesException(String.join(" ", erreurs));
        }
    }

    /** Accepte un SIREN (9 chiffres) ou un numéro de TVA intracommunautaire français. */
    static boolean estSirenOuTvaValide(String numero) {
        return numero != null && (SIREN.matcher(numero).matches() || TVA_FR.matcher(numero).matches());
    }

    /** "fr 12 902 345 678" -> "FR12902345678" ; chaîne vide -> null. */
    private static String normaliserSirenTva(String numero) {
        String sansEspaces = numero == null ? null : numero.replaceAll("[\\s.-]", "").toUpperCase();
        return nettoyer(sansEspaces);
    }

    /** Supprime les espaces au début et à la fin ; une chaîne vide devient null. */
    private static String nettoyer(String valeur) {
        if (valeur == null) {
            return null;
        }
        String resultat = valeur.strip();
        return resultat.isEmpty() ? null : resultat;
    }

    private static void verifierObligatoire(String valeur, String libelle, int tailleMax, List<String> erreurs) {
        if (valeur == null) {
            erreurs.add(libelle + " est obligatoire.");
        } else if (valeur.length() > tailleMax) {
            erreurs.add(libelle + " ne doit pas dépasser " + tailleMax + " caractères.");
        }
    }
}
