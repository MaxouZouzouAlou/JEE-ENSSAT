package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.exception.RequeteInvalideException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** calcul HT / TVA 20 % / TTC */
class TarificationTest {

    @ParameterizedTest(name = "{0} euros/h pendant {1} min -> HT {2}, TTC {3}")
    @CsvSource({
            "80.00,  90, 120.00, 144.00",   // 1h30
            "60.00,  60,  60.00,  72.00",   // 1h pile
            "45.50,  50,  37.92,  45.50",   // 37,9166 -> arrondi vers le haut
            "100.00, 20,  33.33,  40.00",   // 33,333 -> arrondi vers le bas ; 33,33 x 1,2 = 39,996
            "60.00,   1,   1.00,   1.20",   // 1 minute
            "0.00,   60,   0.00,   0.00"    // tarif gratuit
    })
    void calculeHtEtTtc(String tarif, int minutes, String ht, String ttc) {
        Tarification.Montants montants = Tarification.calculer(new BigDecimal(tarif), minutes);

        assertEquals(new BigDecimal(ht), montants.ht());
        assertEquals(new BigDecimal(ttc), montants.ttc());
    }

    @Test
    void appliqueUneTvaDeVingtPourCent() {
        Tarification.Montants montants = Tarification.calculer(new BigDecimal("50.00"), 60);

        assertEquals(new BigDecimal("20.00"), montants.tauxTva());
    }

    @Test
    void lesMontantsOntToujoursDeuxDecimales() {
        // tarif sans décimales : on veux quand même 2 décimales
        Tarification.Montants montants = Tarification.calculer(new BigDecimal("60"), 120);

        assertEquals(2, montants.ht().scale());
        assertEquals(2, montants.ttc().scale());
        assertEquals(new BigDecimal("120.00"), montants.ht());
    }

    @Test
    void refuseUnTarifAbsent() {
        assertThrows(RequeteInvalideException.class, () -> Tarification.calculer(null, 60));
    }

    @Test
    void refuseUnTarifNegatif() {
        assertThrows(RequeteInvalideException.class, () -> Tarification.calculer(new BigDecimal("-1.00"), 60));
    }

    @ParameterizedTest
    @CsvSource({"0", "-30"})
    void refuseUneDureeNulleOuNegative(int minutes) {
        assertThrows(RequeteInvalideException.class,
                () -> Tarification.calculer(new BigDecimal("50.00"), minutes));
    }
}
