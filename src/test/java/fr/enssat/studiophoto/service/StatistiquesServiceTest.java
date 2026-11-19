package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.StatistiquesDAO;
import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests du service des statistiques (lot 5), avec un faux DAO qui n'accède pas à la base. */
class StatistiquesServiceTest {

    /** Faux DAO : renvoie des valeurs fixées et retient la période demandée. */
    static class FauxStatistiquesDAO extends StatistiquesDAO {
        final List<Object[]> parSeances = List.<Object[]>of(new Object[] {"Alice Martin", 3}, new Object[] {"Bruno Leroy", 2});
        final List<Object[]> parDuree = List.<Object[]>of(new Object[] {"Emma Petit", 660});
        BigDecimal chiffreAffaires = new BigDecimal("4104.00");
        LocalDate debutDemande;
        LocalDate finDemandee;

        @Override
        public List<Object[]> listerPhotographesParSeance() {
            return parSeances;
        }

        @Override
        public List<Object[]> listerPhotographesParDuree() {
            return parDuree;
        }

        @Override
        public BigDecimal calculerChiffreAffairesPourPeriode(LocalDate dateDebut, LocalDate dateFin) {
            debutDemande = dateDebut;
            finDemandee = dateFin;
            return chiffreAffaires;
        }
    }

    private FauxStatistiquesDAO dao;
    private StatistiquesService service;

    @BeforeEach
    void preparer() {
        dao = new FauxStatistiquesDAO();
        service = new StatistiquesService(dao);
    }

    @Test
    @DisplayName("Classements : renvoyés tels que le DAO les calcule")
    void classements() {
        assertSame(dao.parSeances, service.listerPhotographesParSeance());
        assertSame(dao.parDuree, service.listerPhotographesParDuree());
    }

    @Test
    @DisplayName("Période valide : le DAO reçoit les deux dates et le montant est renvoyé")
    void periodeValide() {
        BigDecimal resultat = service.calculerChiffreAffairesPourPeriode("2026-01-01", "2026-12-31");

        assertEquals(new BigDecimal("4104.00"), resultat);
        assertEquals(LocalDate.of(2026, 1, 1), dao.debutDemande);
        assertEquals(LocalDate.of(2026, 12, 31), dao.finDemandee);
    }

    @Test
    @DisplayName("Période d'un seul jour : acceptée")
    void periodeDUnJour() {
        service.calculerChiffreAffairesPourPeriode("2026-11-18", "2026-11-18");
        assertEquals(dao.debutDemande, dao.finDemandee);
    }

    @Test
    @DisplayName("Période sans facture payée : 0 est un résultat valide")
    void periodeSansFacture() {
        dao.chiffreAffaires = BigDecimal.ZERO;
        assertEquals(0, BigDecimal.ZERO.compareTo(service.calculerChiffreAffairesPourPeriode("2020-01-01", "2020-01-31")));
    }

    @Test
    @DisplayName("Date manquante ou vide : refus 400, la base n'est pas interrogée")
    void dateManquante() {
        assertThrows(DonneesInvalidesException.class, () -> service.calculerChiffreAffairesPourPeriode(null, "2026-12-31"));
        assertThrows(DonneesInvalidesException.class, () -> service.calculerChiffreAffairesPourPeriode("2026-01-01", "  "));
        assertNull(dao.debutDemande);
    }

    @Test
    @DisplayName("Date mal formée ou inexistante : refus 400 avec la valeur reçue dans le message")
    void dateMalFormee() {
        DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class,
                () -> service.calculerChiffreAffairesPourPeriode("18/11/2026", "2026-12-31"));
        assertTrue(e.getMessage().contains("18/11/2026"));
        assertThrows(DonneesInvalidesException.class, () -> service.calculerChiffreAffairesPourPeriode("2026-02-30", "2026-12-31"));
        assertThrows(DonneesInvalidesException.class, () -> service.calculerChiffreAffairesPourPeriode("2026-01-01' OR '1'='1", "2026-12-31"));
        assertNull(dao.debutDemande);
    }

    @Test
    @DisplayName("Date de début après la date de fin : refus 400")
    void periodeInversee() {
        assertThrows(DonneesInvalidesException.class, () -> service.calculerChiffreAffairesPourPeriode("2026-12-31", "2026-01-01"));
        assertNull(dao.debutDemande);
    }
}
