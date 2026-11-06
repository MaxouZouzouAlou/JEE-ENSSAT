package fr.enssat.studiophoto.model;

import java.math.BigDecimal;

/** Type de séance (Portrait, Mariage...) et son tarif horaire HT. Table {@code type_seance}. */
public class TypeSeance {

    private Long id;
    private String libelle;
    private BigDecimal tarifHoraireHT;

    public TypeSeance() {
    }

    public TypeSeance(Long id, String libelle, BigDecimal tarifHoraireHT) {
        this.id = id;
        this.libelle = libelle;
        this.tarifHoraireHT = tarifHoraireHT;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public BigDecimal getTarifHoraireHT() { return tarifHoraireHT; }
    public void setTarifHoraireHT(BigDecimal tarifHoraireHT) { this.tarifHoraireHT = tarifHoraireHT; }

    @Override
    public String toString() {
        return "TypeSeance{id=" + id + ", libelle='" + libelle + "', tarif=" + tarifHoraireHT + "}";
    }
}
