package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.exception.RequeteInvalideException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcul des montants d'une facture :
 * HT = tarif horaire x durée en heure (arrondi à 2 décimales), TTC = HT arrondi x 1,20.
 * BigDecimal + HALF_UP partout, pas de double pour de l'argent.
 */
public final class Tarification {

    // TVA en %
    public static final BigDecimal TAUX_TVA = new BigDecimal("20.00");

    private static final BigDecimal CENT = new BigDecimal("100");
    private static final BigDecimal MINUTES_PAR_HEURE = new BigDecimal("60");
    private static final int DECIMALES = 2;

    private Tarification() {
    }

    /** HT, taux de TVA et TTC */
    public record Montants(BigDecimal ht, BigDecimal tauxTva, BigDecimal ttc) {
    }

    // 400 si le tarif est absent/négatif ou si la durée est <= 0
    public static Montants calculer(BigDecimal tarifHoraireHT, int dureeMinutes) {
        if (tarifHoraireHT == null || tarifHoraireHT.signum() < 0) {
            throw new RequeteInvalideException("Le tarif horaire du type de séance est absent ou négatif");
        }
        if (dureeMinutes <= 0) {
            throw new RequeteInvalideException("La durée de la séance doit être positive");
        }
        BigDecimal ht = tarifHoraireHT
                .multiply(BigDecimal.valueOf(dureeMinutes))
                .divide(MINUTES_PAR_HEURE, DECIMALES, RoundingMode.HALF_UP);
        BigDecimal coefficient = BigDecimal.ONE.add(TAUX_TVA.divide(CENT));
        BigDecimal ttc = ht.multiply(coefficient).setScale(DECIMALES, RoundingMode.HALF_UP);
        return new Montants(ht, TAUX_TVA, ttc);
    }
}
