package fr.enssat.studiophoto.model;

/** Client particulier, caractérisé par un nom et un prénom. */
public class ClientParticulier extends Client {

    private String nom;
    private String prenom;

    public ClientParticulier() {
    }

    public ClientParticulier(Long id, String adressePostale, String email, String nom, String prenom) {
        super(id, adressePostale, email);
        this.nom = nom;
        this.prenom = prenom;
    }

    @Override
    public TypeClient getType() {
        return TypeClient.PARTICULIER;
    }

    @Override
    public String designation() {
        return prenom + " " + nom;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    @Override
    public String toString() {
        return "ClientParticulier{id=" + getId() + ", " + designation() + "}";
    }
}
