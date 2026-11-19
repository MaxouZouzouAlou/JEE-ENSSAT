package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.StatistiquesDAO;
import fr.enssat.studiophoto.exception.DonneesInvalidesException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Statistiques du studio (lot 5) : deux classements des photographes et le chiffre
 * d'affaires d'une période. Le service contrôle la période avant d'interroger la base.
 */
public class StatistiquesService {
    private final StatistiquesDAO statistiquesDAO;

    public StatistiquesService() {
        this(new StatistiquesDAO());
    }

    public StatistiquesService(StatistiquesDAO statistiquesDAO) {
        this.statistiquesDAO = statistiquesDAO;
    }

    public List<Object[]> listerPhotographesParSeance() {
        return statistiquesDAO.listerPhotographesParSeance();
    }

    public List<Object[]> listerPhotographesParDuree() {
        return statistiquesDAO.listerPhotographesParDuree();
    }

    /**
     * Chiffre d'affaires TTC des factures payées émises entre les deux dates, incluses.
     *
     * @param dateDebut date au format AAAA-MM-JJ
     * @param dateFin   date au format AAAA-MM-JJ, égale ou postérieure à {@code dateDebut}
     * @throws DonneesInvalidesException (400) si une date manque, est mal formée, ou si la période est inversée
     */
    public BigDecimal calculerChiffreAffairesPourPeriode(String dateDebut, String dateFin) {
        LocalDate debut = lireDate(dateDebut, "de début");
        LocalDate fin = lireDate(dateFin, "de fin");
        if (debut.isAfter(fin)) {
            throw new DonneesInvalidesException("La date de début (" + debut + ") doit précéder la date de fin (" + fin + ").");
        }
        return statistiquesDAO.calculerChiffreAffairesPourPeriode(debut, fin);
    }

    private static LocalDate lireDate(String valeur, String laquelle) {
        if (valeur == null || valeur.isBlank()) {
            throw new DonneesInvalidesException("La date " + laquelle + " est obligatoire.");
        }
        try {
            return LocalDate.parse(valeur.trim());
        } catch (DateTimeParseException e) {
            throw new DonneesInvalidesException("La date " + laquelle + " « " + valeur + " » n'est pas valide (format attendu : AAAA-MM-JJ).");
        }
    }
}
