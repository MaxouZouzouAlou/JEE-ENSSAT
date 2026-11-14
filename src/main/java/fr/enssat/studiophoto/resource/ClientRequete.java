package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.ClientParticulier;
import fr.enssat.studiophoto.model.TypeClient;

/**
 * Corps JSON reçu par {@code POST} et {@code PUT /api/clients} : un format unique
 * pour les deux types de client, le champ {@code type} indiquant lequel.
 *
 * <pre>
 * {"type": "PARTICULIER", "nom": "Bernard", "prenom": "Julie", "adressePostale": "...", "email": "..."}
 * {"type": "ENTREPRISE", "raisonSociale": "Agence Horizon", "sirenTva": "901234567", "adressePostale": "...", "email": "..."}
 * </pre>
 *
 * <p>JSON-B ne sait pas instancier une classe abstraite comme {@link Client} : on lit
 * donc tous les champs possibles ici, puis {@link #versClient()} crée la bonne sous-classe.
 * Les champs propres à l'autre type sont ignorés.</p>
 */
public class ClientRequete {

    private String type;
    private String adressePostale;
    private String email;
    private String nom;
    private String prenom;
    private String raisonSociale;
    private String sirenTva;

    /**
     * Convertit la requête en {@link ClientParticulier} ou {@link ClientEntreprise}.
     * La validation des champs est faite ensuite par le service.
     *
     * @throws DonneesInvalidesException si le type est absent ou inconnu (400)
     */
    public Client versClient() {
        return switch (lireType()) {
            case PARTICULIER -> new ClientParticulier(null, adressePostale, email, nom, prenom);
            case ENTREPRISE -> new ClientEntreprise(null, adressePostale, email, raisonSociale, sirenTva);
        };
    }

    private TypeClient lireType() {
        if (type == null || type.isBlank()) {
            throw new DonneesInvalidesException("Le type de client est obligatoire (PARTICULIER ou ENTREPRISE).");
        }
        try {
            return TypeClient.valueOf(type.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DonneesInvalidesException("Type de client inconnu : " + type
                    + ". Valeurs possibles : PARTICULIER, ENTREPRISE.");
        }
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAdressePostale() { return adressePostale; }
    public void setAdressePostale(String adressePostale) { this.adressePostale = adressePostale; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getSirenTva() { return sirenTva; }
    public void setSirenTva(String sirenTva) { this.sirenTva = sirenTva; }
}
