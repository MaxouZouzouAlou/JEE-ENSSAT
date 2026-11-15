package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.FactureDAO;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.RequeteInvalideException;
import fr.enssat.studiophoto.exception.IntrouvableException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.ClientParticulier;
import fr.enssat.studiophoto.model.Facture;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.StatutPaiement;
import fr.enssat.studiophoto.model.StatutSeance;
import fr.enssat.studiophoto.model.TypeSeance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FactureService avec un DAO mocké et une date fixe */
@ExtendWith(MockitoExtension.class)
class FactureServiceTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 15);

    @Mock
    private FactureDAO factureDAO;

    private FactureService service;

    @BeforeEach
    void setUp() {
        service = serviceLeJour(AUJOURD_HUI);
    }

    private FactureService serviceLeJour(LocalDate jour) {
        Clock horloge = Clock.fixed(jour.atTime(10, 0).atZone(PARIS).toInstant(), PARIS);
        return new FactureService(factureDAO, horloge);
    }

    private static ClientParticulier particulier() {
        return new ClientParticulier(3L, "12 rue des Lilas, 22300 Lannion", "julie@mail.example", "Bernard", "Julie");
    }

    private static ClientEntreprise entreprise() {
        return new ClientEntreprise(7L, "15 boulevard de la Mer, 22300 Lannion", "contact@horizon.example",
                "Agence Horizon", "901234567");
    }

    // séance de 90 min à 80 euros/h : 120,00 HT / 144,00 TTC
    private static Seance seance(StatutSeance statut, Client client) {
        Seance seance = new Seance();
        seance.setId(9L);
        seance.setDate(LocalDate.of(2026, 11, 5));
        seance.setHeureDebut(LocalTime.of(16, 0));
        seance.setDuree(90);
        seance.setStatut(statut);
        seance.setTypeSeance(new TypeSeance(1L, "Portrait", new BigDecimal("80.00")));
        seance.setPhotographe(new Photographe(2L, "Alice Martin", "alice@studio.example", true));
        seance.setClient(client);
        return seance;
    }

    private static Facture facture(StatutPaiement statut) {
        Facture facture = new Facture();
        facture.setId(5L);
        facture.setNumero("FAC-2026-0005");
        facture.setStatutPaiement(statut);
        return facture;
    }

    // séance TERMINEE pas encore facturée, create renvoie la facture qu'on lui donne
    private void preparerGeneration(Client client) {
        when(factureDAO.findSeance(9L)).thenReturn(Optional.of(seance(StatutSeance.TERMINEE, client)));
        when(factureDAO.findBySeanceId(9L)).thenReturn(Optional.empty());
        when(factureDAO.create(any(Facture.class))).then(returnsFirstArg());
    }

    @Nested
    class Generation {

        @Test
        void genereLaFactureD_uneSeanceTerminee() {
            preparerGeneration(particulier());
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.empty());

            Facture facture = service.generer(9L);

            assertEquals(9L, facture.getSeanceId());
            assertEquals(StatutPaiement.EN_ATTENTE, facture.getStatutPaiement());
            assertEquals(AUJOURD_HUI, facture.getDateEmission());
            assertEquals(new BigDecimal("120.00"), facture.getMontantHT());
            assertEquals(new BigDecimal("20.00"), facture.getTauxTVA());
            assertEquals(new BigDecimal("144.00"), facture.getMontantTTC());
        }

        @Test
        void refuseUneSeanceInconnue() {
            when(factureDAO.findSeance(404L)).thenReturn(Optional.empty());

            assertThrows(IntrouvableException.class, () -> service.generer(404L));
            verify(factureDAO, never()).create(any());
        }

        @ParameterizedTest
        @EnumSource(value = StatutSeance.class, names = {"PLANIFIEE", "CONFIRMEE", "ANNULEE"})
        void refuseUneSeanceNonTerminee(StatutSeance statut) {
            when(factureDAO.findSeance(9L)).thenReturn(Optional.of(seance(statut, particulier())));

            ConflitException e = assertThrows(ConflitException.class, () -> service.generer(9L));

            assertEquals(true, e.getMessage().contains(statut.name()));
            verify(factureDAO, never()).create(any());
        }

        @Test
        void refuseUneSeanceDejaFacturee() {
            when(factureDAO.findSeance(9L)).thenReturn(Optional.of(seance(StatutSeance.TERMINEE, particulier())));
            when(factureDAO.findBySeanceId(9L)).thenReturn(Optional.of(facture(StatutPaiement.PAYEE)));

            assertThrows(ConflitException.class, () -> service.generer(9L));
            verify(factureDAO, never()).create(any());
        }
    }

    @Nested
    class Numerotation {

        @Test
        void premiereFactureDeL_anneeCommenceAQuatreZerosUn() {
            preparerGeneration(particulier());
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.empty());

            assertEquals("FAC-2026-0001", service.generer(9L).getNumero());
        }

        @Test
        void factureSuivanteIncrementeLeDernierNumero() {
            preparerGeneration(particulier());
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.of("FAC-2026-0007"));

            assertEquals("FAC-2026-0008", service.generer(9L).getNumero());
        }

        @Test
        void leCompteurRepartAUnChaqueAnnee() {
            preparerGeneration(particulier());
            // 2026 est déjà bien entamé mais en 2027 on repart à 0001
            when(factureDAO.findDernierNumero(2027)).thenReturn(Optional.empty());

            Facture facture = serviceLeJour(LocalDate.of(2027, 1, 2)).generer(9L);

            assertEquals("FAC-2027-0001", facture.getNumero());
            verify(factureDAO, never()).findDernierNumero(2026);
        }

        @Test
        void remplitLesZerosAGauche() {
            preparerGeneration(particulier());
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.of("FAC-2026-0099"));

            assertEquals("FAC-2026-0100", service.generer(9L).getNumero());
        }

        @Test
        void refuseQuandLesNeufMilleNeufCentQuatreVingtDixNeufNumerosSontPris() {
            when(factureDAO.findSeance(9L)).thenReturn(Optional.of(seance(StatutSeance.TERMINEE, particulier())));
            when(factureDAO.findBySeanceId(9L)).thenReturn(Optional.empty());
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.of("FAC-2026-9999"));

            assertThrows(ConflitException.class, () -> service.generer(9L));
            verify(factureDAO, never()).create(any());
        }

        @Test
        void rejoueAvecLeNumeroSuivantSiUneAutreFactureAPrisLeMien() {
            when(factureDAO.findSeance(9L)).thenReturn(Optional.of(seance(StatutSeance.TERMINEE, particulier())));
            when(factureDAO.findBySeanceId(9L)).thenReturn(Optional.empty());
            when(factureDAO.findDernierNumero(2026))
                    .thenReturn(Optional.of("FAC-2026-0003"), Optional.of("FAC-2026-0004"));
            when(factureDAO.create(any(Facture.class)))
                    .thenThrow(new ConflitException("numéro déjà pris"))
                    .then(returnsFirstArg());

            Facture facture = service.generer(9L);

            assertEquals("FAC-2026-0005", facture.getNumero());
            verify(factureDAO, times(2)).create(any());
        }

        @Test
        void neRejoueQueTroisFois() {
            when(factureDAO.findSeance(9L)).thenReturn(Optional.of(seance(StatutSeance.TERMINEE, particulier())));
            when(factureDAO.findBySeanceId(9L)).thenReturn(Optional.empty());
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.empty());
            when(factureDAO.create(any(Facture.class))).thenThrow(new ConflitException("numéro déjà pris"));

            assertThrows(ConflitException.class, () -> service.generer(9L));
            verify(factureDAO, times(3)).create(any());
        }

        @Test
        void neRejoueAsPasSiLaSeanceAEteFactureeEntreTemps() {
            when(factureDAO.findSeance(9L)).thenReturn(Optional.of(seance(StatutSeance.TERMINEE, particulier())));
            // pas facturée au premier contrôle, mais facturée par quelqu'un d'autre quand l'insert échou
            when(factureDAO.findBySeanceId(9L))
                    .thenReturn(Optional.empty(), Optional.of(facture(StatutPaiement.EN_ATTENTE)));
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.empty());
            when(factureDAO.create(any(Facture.class))).thenThrow(new ConflitException("séance déjà facturée"));

            assertThrows(ConflitException.class, () -> service.generer(9L));
            verify(factureDAO, times(1)).create(any());
        }
    }

    @Nested
    class CoordonneesFigees {

        @Test
        void copieLesCoordonneesDUnParticulier() {
            Client client = particulier();
            preparerGeneration(client);
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.empty());

            Facture facture = service.generer(9L);

            assertEquals(client.designation(), facture.getClientDesignation());
            assertEquals("12 rue des Lilas, 22300 Lannion", facture.getClientAdresse());
            assertEquals("julie@mail.example", facture.getClientEmail());
            assertNull(facture.getClientSirenTva());
        }

        @Test
        void copieLeSirenTvaDUneEntreprise() {
            Client client = entreprise();
            preparerGeneration(client);
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.empty());

            Facture facture = service.generer(9L);

            assertEquals("Agence Horizon", facture.getClientDesignation());
            assertEquals("901234567", facture.getClientSirenTva());
        }

        @Test
        void copieLeDetailDeLaPrestation() {
            preparerGeneration(particulier());
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.empty());

            Facture facture = service.generer(9L);

            assertEquals(LocalDate.of(2026, 11, 5), facture.getPrestationDate());
            assertEquals(LocalTime.of(16, 0), facture.getPrestationHeure());
            assertEquals(90, facture.getPrestationDuree());
            assertEquals("Portrait", facture.getPrestationType());
            assertEquals("Alice Martin", facture.getPhotographeNom());
        }

        @Test
        void modifierLeClientApresCoupNeChangePasLaFacture() {
            ClientParticulier client = particulier();
            preparerGeneration(client);
            when(factureDAO.findDernierNumero(2026)).thenReturn(Optional.empty());

            Facture facture = service.generer(9L);
            client.setAdressePostale("1 avenue Neuve, 35000 Rennes");
            client.setEmail("nouveau@mail.example");

            assertEquals("12 rue des Lilas, 22300 Lannion", facture.getClientAdresse());
            assertEquals("julie@mail.example", facture.getClientEmail());
        }
    }

    @Nested
    class StatutDePaiement {

        @ParameterizedTest
        @EnumSource(value = StatutPaiement.class, names = {"PAYEE", "ANNULEE"})
        void autoriseLePassageDeEnAttenteVersPayeeOuAnnulee(StatutPaiement cible) {
            when(factureDAO.findById(5L)).thenReturn(Optional.of(facture(StatutPaiement.EN_ATTENTE)));
            when(factureDAO.updateStatut(5L, StatutPaiement.EN_ATTENTE, cible)).thenReturn(true);

            Facture resultat = service.changerStatut(5L, cible);

            assertEquals(cible, resultat.getStatutPaiement());
            verify(factureDAO).updateStatut(5L, StatutPaiement.EN_ATTENTE, cible);
        }

        @Test
        void refuseSiLeStatutAChangeEntreLaLectureEtL_ecriture() {
            // une autre requête a déjà payé la facture : l'update conditionnel ne touche aucune ligne
            when(factureDAO.findById(5L)).thenReturn(Optional.of(facture(StatutPaiement.EN_ATTENTE)));
            when(factureDAO.updateStatut(5L, StatutPaiement.EN_ATTENTE, StatutPaiement.ANNULEE)).thenReturn(false);

            ConflitException e = assertThrows(ConflitException.class,
                    () -> service.changerStatut(5L, StatutPaiement.ANNULEE));

            assertEquals(true, e.getMessage().contains("entre-temps"));
        }

        @ParameterizedTest
        @CsvSource({
                "PAYEE,      EN_ATTENTE",
                "PAYEE,      ANNULEE",
                "ANNULEE,    PAYEE",
                "ANNULEE,    EN_ATTENTE",
                "EN_ATTENTE, EN_ATTENTE"
        })
        void refuseLesAutresTransitions(StatutPaiement actuel, StatutPaiement cible) {
            when(factureDAO.findById(5L)).thenReturn(Optional.of(facture(actuel)));

            assertThrows(ConflitException.class, () -> service.changerStatut(5L, cible));
            verify(factureDAO, never()).updateStatut(anyLong(), any(), any());
        }

        @Test
        void refuseUnStatutAbsent() {
            assertThrows(RequeteInvalideException.class, () -> service.changerStatut(5L, null));
            verify(factureDAO, never()).updateStatut(anyLong(), any(), any());
        }

        @Test
        void refuseUneFactureInconnue() {
            when(factureDAO.findById(404L)).thenReturn(Optional.empty());

            assertThrows(IntrouvableException.class,
                    () -> service.changerStatut(404L, StatutPaiement.PAYEE));
        }
    }

    @Nested
    class Lecture {

        @Test
        void listerRenvoieLesFacturesDuDao() {
            List<Facture> factures = List.of(facture(StatutPaiement.PAYEE), facture(StatutPaiement.EN_ATTENTE));
            when(factureDAO.findAll()).thenReturn(factures);

            assertSame(factures, service.lister());
        }

        @Test
        void trouverUneFactureExistante() {
            Facture facture = facture(StatutPaiement.PAYEE);
            when(factureDAO.findById(5L)).thenReturn(Optional.of(facture));

            assertSame(facture, service.trouver(5L));
        }

        @Test
        void trouverUneFactureInconnueRenvoie404() {
            when(factureDAO.findById(404L)).thenReturn(Optional.empty());

            assertThrows(IntrouvableException.class, () -> service.trouver(404L));
        }
    }
}
