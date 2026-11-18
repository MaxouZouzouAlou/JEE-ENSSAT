package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.SeanceDAO;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.StatutSeance;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Faux DAO en mémoire, avec la même définition de « séance à venir » que la requête SQL.
 * Il hérite de SeanceDAO et redéfinit seulement les méthodes utilisées par le lot 1 :
 * aucune autre méthode n'est appelée par PhotographeService, donc la base n'est jamais touchée.
 */
class FauxSeanceDAO extends SeanceDAO {

    final List<Seance> seances = new ArrayList<>();
    private long prochainId = 1;

    Seance ajouter(Photographe photographe, LocalDateTime debut, int dureeMinutes, StatutSeance statut) {
        Seance s = new Seance(prochainId++, Date.valueOf(debut.toLocalDate()), Time.valueOf(debut.toLocalTime()),
                dureeMinutes, "Studio", statut);
        s.setPhotographe(photographe);
        seances.add(s);
        return s;
    }

    Seance ajouter(Photographe photographe, LocalDate date, LocalTime heure, StatutSeance statut) {
        return ajouter(photographe, LocalDateTime.of(date, heure), 60, statut);
    }

    @Override
    public int compterParPhotographe(long photographeId) {
        return (int) seances.stream().filter(duPhotographe(photographeId)).count();
    }

    @Override
    public int compterAVenirParPhotographe(long photographeId, LocalDateTime maintenant) {
        return (int) seances.stream().filter(duPhotographe(photographeId).and(aVenir(maintenant))).count();
    }

    @Override
    public int annulerAVenirParPhotographe(long photographeId, LocalDateTime maintenant) {
        int n = 0;
        for (Seance s : seances) {
            if (duPhotographe(photographeId).and(aVenir(maintenant)).test(s)) {
                s.setStatut(StatutSeance.ANNULEE);
                n++;
            }
        }
        return n;
    }

    private static Predicate<Seance> duPhotographe(long id) {
        return s -> s.getPhotographe().getId() == id;
    }

    private static Predicate<Seance> aVenir(LocalDateTime maintenant) {
        return s -> (s.getStatut() == StatutSeance.PLANIFIEE || s.getStatut() == StatutSeance.CONFIRMEE)
                && s.calculerDebut().isAfter(maintenant);
    }
}
