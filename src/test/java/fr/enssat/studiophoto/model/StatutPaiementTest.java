package fr.enssat.studiophoto.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** les 9 transitions possibles (3 x 3) */
class StatutPaiementTest {

    @ParameterizedTest(name = "{0} -> {1} : {2}")
    @CsvSource({
            "EN_ATTENTE, PAYEE,      true",
            "EN_ATTENTE, ANNULEE,    true",
            "EN_ATTENTE, EN_ATTENTE, false",
            "PAYEE,      EN_ATTENTE, false",
            "PAYEE,      PAYEE,      false",
            "PAYEE,      ANNULEE,    false",
            "ANNULEE,    EN_ATTENTE, false",
            "ANNULEE,    PAYEE,      false",
            "ANNULEE,    ANNULEE,    false"
    })
    void transitions(StatutPaiement depart, StatutPaiement cible, boolean autorisee) {
        assertEquals(autorisee, depart.peutPasserA(cible));
    }
}
