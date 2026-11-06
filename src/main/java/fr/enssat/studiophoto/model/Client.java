package fr.enssat.studiophoto.model;

/**
 * Client du studio : classe abstraite, spécialisée en {@link ClientParticulier}
 * et {@link ClientEntreprise}.
 *
 * <p>En base, les deux types sont dans la même table {@code client} ; la colonne
 * {@code type} indique la sous-classe à instancier dans le DAO.</p>
 */
public abstract class Client {

    private Long id;
    private String adressePostale;
    private String email;

    protected Client() {
    }

    protected Client(Long id, String adressePostale, String email) {
        this.id = id;
        this.adressePostale = adressePostale;
        this.email = email;
    }

    /** Type du client, renvoyé dans le JSON pour que le front distingue les deux cas. */
    public abstract TypeClient getType();

    /** Nom à afficher : "Prénom Nom" pour un particulier, raison sociale pour une entreprise. */
    public abstract String designation();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAdressePostale() { return adressePostale; }
    public void setAdressePostale(String adressePostale) { this.adressePostale = adressePostale; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
