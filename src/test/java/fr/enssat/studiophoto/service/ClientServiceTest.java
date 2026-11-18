package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.ClientDAO;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.exception.RessourceIntrouvableException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.ClientParticulier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests de ClientService : le DAO est simulé avec Mockito, aucun accès à la base. */
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientDAO clientDAO;

    private ClientService service;

    @BeforeEach
    void setUp() {
        service = new ClientService(clientDAO);
    }

    private static ClientParticulier particulierValide() {
        return new ClientParticulier(null, "12 rue des Lilas, 22300 Lannion", "julie@mail.example", "Bernard", "Julie");
    }

    private static ClientEntreprise entrepriseValide() {
        return new ClientEntreprise(null, "15 boulevard de la Mer, 22300 Lannion", "contact@horizon.example",
                "Agence Horizon", "901234567");
    }

    @Nested
    class Lecture {

        @Test
        void listerRenvoieLesClientsDuDao() {
            List<Client> clients = List.of(particulierValide(), entrepriseValide());
            when(clientDAO.findAll()).thenReturn(clients);

            assertSame(clients, service.lister());
        }

        @Test
        void trouverClientExistant() {
            Client client = particulierValide();
            when(clientDAO.findById(1L)).thenReturn(Optional.of(client));

            assertSame(client, service.trouver(1L));
        }

        @Test
        void trouverClientInconnuDonne404() {
            when(clientDAO.findById(42L)).thenReturn(Optional.empty());

            RessourceIntrouvableException e = assertThrows(RessourceIntrouvableException.class, () -> service.trouver(42L));
            assertEquals("Client 42 introuvable.", e.getMessage());
        }
    }

    @Nested
    class CreationParticulier {

        @Test
        void particulierValideEstEnregistre() {
            ClientParticulier client = particulierValide();
            when(clientDAO.create(client)).thenReturn(client);

            assertSame(client, service.creer(client));
            verify(clientDAO).create(client);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void nomManquantEstRefuse(String nom) {
            ClientParticulier client = particulierValide();
            client.setNom(nom);

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("Le nom est obligatoire."));
            verify(clientDAO, never()).create(any());
        }

        @Test
        void prenomManquantEstRefuse() {
            ClientParticulier client = particulierValide();
            client.setPrenom(null);

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("Le prénom est obligatoire."));
        }

        @Test
        void particulierSansRaisonSocialeNiSirenEstAccepte() {
            ClientParticulier client = particulierValide();
            when(clientDAO.create(client)).thenReturn(client);

            assertDoesNotThrow(() -> service.creer(client));
        }
    }

    @Nested
    class CreationEntreprise {

        @Test
        void entrepriseValideEstEnregistree() {
            ClientEntreprise client = entrepriseValide();
            when(clientDAO.create(client)).thenReturn(client);

            assertSame(client, service.creer(client));
        }

        @Test
        void raisonSocialeManquanteEstRefusee() {
            ClientEntreprise client = entrepriseValide();
            client.setRaisonSociale(" ");

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("La raison sociale est obligatoire."));
        }

        @Test
        void sirenManquantEstRefuse() {
            ClientEntreprise client = entrepriseValide();
            client.setSirenTva(null);

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("Le numéro SIREN/TVA est obligatoire."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345678", "1234567890", "12345678A", "ABCDEFGHI", "DE123456789", "FR1290234567"})
        void sirenInvalideEstRefuse(String numero) {
            ClientEntreprise client = entrepriseValide();
            client.setSirenTva(numero);

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("Le numéro SIREN/TVA est invalide"));
            verify(clientDAO, never()).create(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"901234567", "FR12902345678", "FRAB902345678"})
        void sirenOuTvaValideEstAccepte(String numero) {
            ClientEntreprise client = entrepriseValide();
            client.setSirenTva(numero);
            when(clientDAO.create(client)).thenReturn(client);

            assertDoesNotThrow(() -> service.creer(client));
        }

        @Test
        void numeroEstNormaliseAvantEnregistrement() {
            ClientEntreprise client = entrepriseValide();
            client.setSirenTva(" fr 12 902 345 678 ");
            when(clientDAO.create(client)).thenReturn(client);

            service.creer(client);

            assertEquals("FR12902345678", client.getSirenTva());
        }
    }

    @Nested
    class ChampsCommuns {

        @Test
        void adresseManquanteEstRefusee() {
            ClientEntreprise client = entrepriseValide();
            client.setAdressePostale("");

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("L'adresse postale est obligatoire."));
        }

        @Test
        void emailManquantEstRefuse() {
            ClientParticulier client = particulierValide();
            client.setEmail(null);

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("L'email est obligatoire."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"julie", "julie@", "julie@mail", "ju lie@mail.example"})
        void emailMalFormeEstRefuse(String email) {
            ClientParticulier client = particulierValide();
            client.setEmail(email);

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("L'email n'est pas valide."));
        }

        @Test
        void champTropLongEstRefuse() {
            ClientParticulier client = particulierValide();
            client.setNom("x".repeat(101));

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertTrue(e.getMessage().contains("Le nom ne doit pas dépasser 100 caractères."));
        }

        @Test
        void toutesLesErreursSontRegroupees() {
            ClientParticulier client = new ClientParticulier(null, null, null, null, null);

            DonneesInvalidesException e = assertThrows(DonneesInvalidesException.class, () -> service.creer(client));
            assertEquals("L'adresse postale est obligatoire. L'email est obligatoire. "
                    + "Le nom est obligatoire. Le prénom est obligatoire.", e.getMessage());
        }

        @Test
        void clientAbsentEstRefuse() {
            assertThrows(DonneesInvalidesException.class, () -> service.creer(null));
        }

        @Test
        void espacesSuperflusSontRetires() {
            ClientParticulier client = new ClientParticulier(null, "  12 rue des Lilas ", " julie@mail.example ", " Bernard ", " Julie");
            when(clientDAO.create(client)).thenReturn(client);

            service.creer(client);

            assertEquals("12 rue des Lilas", client.getAdressePostale());
            assertEquals("julie@mail.example", client.getEmail());
            assertEquals("Bernard", client.getNom());
            assertEquals("Julie", client.getPrenom());
        }

        @Test
        void idEnvoyeALaCreationEstIgnore() {
            ClientParticulier client = particulierValide();
            client.setId(7L);
            when(clientDAO.create(client)).thenReturn(client);

            service.creer(client);

            assertNull(client.getId());
        }
    }

    @Nested
    class Modification {

        @Test
        void modificationValidePrendLIdDeLUrl() {
            ClientParticulier client = particulierValide();
            when(clientDAO.update(client)).thenReturn(true);

            Client resultat = service.modifier(3L, client);

            assertEquals(3L, resultat.getId());
        }

        @Test
        void modificationClientInconnuDonne404() {
            when(clientDAO.update(any())).thenReturn(false);

            assertThrows(RessourceIntrouvableException.class, () -> service.modifier(42L, particulierValide()));
        }

        @Test
        void modificationInvalideNAppellePasLeDao() {
            ClientEntreprise client = entrepriseValide();
            client.setSirenTva("123");

            assertThrows(DonneesInvalidesException.class, () -> service.modifier(3L, client));
            verify(clientDAO, never()).update(any());
        }
    }

    @Nested
    class Suppression {

        @Test
        void clientSansSeanceEstSupprime() {
            when(clientDAO.findById(4L)).thenReturn(Optional.of(particulierValide()));
            when(clientDAO.countSeances(4L)).thenReturn(0);
            when(clientDAO.delete(4L)).thenReturn(true);

            service.supprimer(4L);

            verify(clientDAO).delete(4L);
        }

        @Test
        void clientAvecSeancesEstRefuseEn409() {
            when(clientDAO.findById(5L)).thenReturn(Optional.of(entrepriseValide()));
            when(clientDAO.countSeances(5L)).thenReturn(2);

            ConflitException e = assertThrows(ConflitException.class, () -> service.supprimer(5L));
            assertEquals("Impossible de supprimer ce client : il a 2 séances enregistrées.", e.getMessage());
            verify(clientDAO, never()).delete(anyLong());
        }

        @Test
        void messageAuSingulierPourUneSeule() {
            when(clientDAO.findById(5L)).thenReturn(Optional.of(entrepriseValide()));
            when(clientDAO.countSeances(5L)).thenReturn(1);

            ConflitException e = assertThrows(ConflitException.class, () -> service.supprimer(5L));
            assertTrue(e.getMessage().endsWith("1 séance enregistrée."));
        }

        @Test
        void suppressionClientInconnuDonne404() {
            when(clientDAO.findById(42L)).thenReturn(Optional.empty());

            assertThrows(RessourceIntrouvableException.class, () -> service.supprimer(42L));
            verify(clientDAO, never()).countSeances(anyLong());
            verify(clientDAO, never()).delete(anyLong());
        }
    }
}
