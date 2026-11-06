package fr.enssat.studiophoto.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Photographe du studio. Table {@code photographe}, spécialités dans {@code photographe_specialite}.
 * Un photographe inactif ne peut pas être affecté à une séance.
 */
public class Photographe {

    private Long id;
    private String nom;
    private String email;
    private boolean actif = true;
    private List<Specialite> specialites = new ArrayList<>();

    public Photographe() {
    }

    public Photographe(Long id, String nom, String email, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.actif = actif;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public List<Specialite> getSpecialites() { return specialites; }
    public void setSpecialites(List<Specialite> specialites) {
        this.specialites = specialites != null ? specialites : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Photographe{id=" + id + ", nom='" + nom + "', actif=" + actif + "}";
    }
}
