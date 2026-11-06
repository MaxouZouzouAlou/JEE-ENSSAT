package fr.enssat.studiophoto.model;

/** Spécialité d'un photographe (Reportage, Mariage et fêtes, Famille). Table {@code specialite}. */
public class Specialite {

    private Long id;
    private String libelle;

    public Specialite() {
    }

    public Specialite(Long id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    @Override
    public String toString() {
        return "Specialite{id=" + id + ", libelle='" + libelle + "'}";
    }
}
