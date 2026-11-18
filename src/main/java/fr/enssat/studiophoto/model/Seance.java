package fr.enssat.studiophoto.model;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.sql.Date;

/**
 * Séance photo. Table {@code seance}.
 *
 * <p>Le DAO renvoie le photographe, le client et le type de séance complets ;
 * à la création, le front n'a besoin d'envoyer que leurs identifiants.</p>
 */
public class Seance {

    private Long id;
    private LocalDate date;
    private LocalTime heureDebut;
    /** Durée en minutes. */
    private int duree;
    private String lieu;
    private StatutSeance statut = StatutSeance.PLANIFIEE;
    private TypeSeance typeSeance;
    private Photographe photographe;
    private Client client;

    public Seance(Long id, Date date, Time heureDebut, int duree, String lieu, StatutSeance statut) {
        this.id = id;
        this.date = date.toLocalDate();
        this.heureDebut = heureDebut.toLocalTime();
        this.duree = duree;
        this.lieu = lieu;
        this.statut = statut;
    }

    /** Date et heure de début. Pas un getter : n'apparaît pas dans le JSON. */
    public LocalDateTime calculerDebut() {
        return LocalDateTime.of(date, heureDebut);
    }

    /** Date et heure de fin (début + durée). Pas un getter : n'apparaît pas dans le JSON. */
    public LocalDateTime calculerFin() {
        return calculerDebut().plusMinutes(duree);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public StatutSeance getStatut() { return statut; }
    public void setStatut(StatutSeance statut) { this.statut = statut; }

    public TypeSeance getTypeSeance() { return typeSeance; }
    public void setTypeSeance(TypeSeance typeSeance) { this.typeSeance = typeSeance; }

    public Photographe getPhotographe() { return photographe; }
    public void setPhotographe(Photographe photographe) { this.photographe = photographe; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    @Override
    public String toString() {
        return "Seance{id=" + id + ", date=" + date + " " + heureDebut + ", duree=" + duree
                + "min, statut=" + statut + "}";
    }
}
