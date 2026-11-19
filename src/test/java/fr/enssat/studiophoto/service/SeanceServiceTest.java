package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.SeanceDAO;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.exception.RessourceIntrouvableException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.ClientParticulier;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.StatutSeance;
import fr.enssat.studiophoto.model.TypeSeance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests du service des séances (lot 3), avec un DAO en mémoire et une horloge fixe. */
class SeanceServiceTest {

    /** « Maintenant » pour tous les tests : jeudi 19 novembre 2026, 10h00. */
    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 19, 10, 0);

    /**
     * Faux DAO : hérite de SeanceDAO et redéfinit les méthodes appelées par SeanceService,
     * avec la même définition du chevauchement que la requête SQL.
     */
    static class FauxDAO extends SeanceDAO {
        final List<Seance> seances = new ArrayList<>();
        final Set<Long> seancesFacturees = new HashSet<>();
        final List<Photographe> photographes = List.of(new Photographe(1L, "Alice Martin", "a@studio.example", true),
                new Photographe(2L, "Bruno Leroy", "b@studio.example", true), new Photographe(4L, "David Moreau", "d@studio.example", false));
        private long prochainId = 1;

        Seance ajouter(long photographeId, LocalDateTime debut, int duree, StatutSeance statut) {
            Seance s = new Seance(prochainId++, Date.valueOf(debut.toLocalDate()), Time.valueOf(debut.toLocalTime()), duree, "Studio", statut);
            s.setPhotographe(photographes.stream().filter(p -> p.getId() == photographeId).findFirst().orElseThrow());
            s.setTypeSeance(new TypeSeance(1L, "Portrait", new BigDecimal("80.00")));
            s.setClient(new ClientParticulier(1L, "12 rue des Lilas", "julie@mail.example", "Bernard", "Julie"));
            seances.add(s);
            return s;
        }

        @Override
        public List<Seance> findByFilters(Long id, Long photographeId, Date dateDebut, Date dateFin) {
            return seances.stream().filter(s -> id == null || s.getId().equals(id))
                    .filter(s -> photographeId == null || s.getPhotographe().getId().equals(photographeId))
                    .filter(s -> dateDebut == null || !s.getDate().isBefore(dateDebut.toLocalDate()) && !s.getDate().isAfter(dateFin.toLocalDate()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<TypeSeance> findTypes() {
            return List.of(new TypeSeance(1L, "Portrait", new BigDecimal("80.00")), new TypeSeance(2L, "Mariage", new BigDecimal("150.00")));
        }

        @Override
        public List<Photographe> findPhotographesActifs() {
            return photographes.stream().filter(Photographe::isActif).collect(Collectors.toList());
        }

        @Override
        public List<Client> findClients() {
            return List.of(new ClientParticulier(1L, "12 rue des Lilas", "julie@mail.example", "Bernard", "Julie"),
                    new ClientEntreprise(5L, "15 bd de la Mer", "contact@horizon.example", "Agence Horizon", "901234567"));
        }

        @Override
        public boolean existeChevauchement(long photographeId, LocalDateTime debut, LocalDateTime fin, Long seanceExclue) {
            return seances.stream().anyMatch(s -> s.getPhotographe().getId() == photographeId
                    && s.getStatut() != StatutSeance.ANNULEE
                    && (seanceExclue == null || !s.getId().equals(seanceExclue))
                    && s.calculerDebut().isBefore(fin) && s.calculerFin().isAfter(debut));
        }

        @Override
        public Seance create(Date date, Time heure, int duree, String lieu, TypeSeance type, Photographe photographe, Client client) {
            Seance s = ajouter(photographe.getId(), LocalDateTime.of(date.toLocalDate(), heure.toLocalTime()), duree, StatutSeance.PLANIFIEE);
            s.setLieu(lieu);
            return s;
        }

        @Override
        public Seance update(long id, Date date, Time heure, int duree, String lieu, StatutSeance statut,
                             TypeSeance type, Photographe photographe, Client client) {
            Seance s = findByFilters(id, null, null, null).get(0);
            if (s.getStatut() == StatutSeance.TERMINEE || s.getStatut() == StatutSeance.ANNULEE) {
                return null; // comme la condition de la requête UPDATE
            }
            s.setDate(date.toLocalDate());
            s.setHeureDebut(heure.toLocalTime());
            s.setDuree(duree);
            s.setLieu(lieu);
            s.setStatut(statut);
            s.setPhotographe(photographes.stream().filter(p -> p.getId().equals(photographe.getId())).findFirst().orElseThrow());
            return s;
        }

        @Override
        public boolean delete(long id) {
            if (seancesFacturees.contains(id)) {
                return false;
            }
            return seances.removeIf(s -> s.getId() == id);
        }
    }

    private FauxDAO dao;
    private SeanceService service;

    @BeforeEach
    void preparer() {
        dao = new FauxDAO();
        ZoneId zone = ZoneId.of("Europe/Paris");
        service = new SeanceService(dao, Clock.fixed(MAINTENANT.atZone(zone).toInstant(), zone));
    }

    /** Corps de requête tel que l'envoie le front : des chaînes. */
    private static Map<String, String> donnees(String date, String heure, int duree, long photographeId) {
        Map<String, String> d = new HashMap<>();
        d.put("date", date);
        d.put("heureDebut", heure);
        d.put("duree", String.valueOf(duree));
        d.put("lieu", "Studio du centre-ville");
        d.put("typeSeanceId", "1");
        d.put("photographeId", String.valueOf(photographeId));
        d.put("clientId", "1");
        d.put("clientType", "PARTICULIER");
        return d;
    }

    private static Map<String, String> avecStatut(Map<String, String> d, StatutSeance statut) {
        d.put("statut", statut.name());
        return d;
    }

    // ------------------------------------------------------------------
    // Création
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Création valide dans le futur : statut PLANIFIEE, séance complète renvoyée")
    void creerSeanceValide() {
        Seance creee = service.creer(donnees("2026-11-20", "14:00:00", 90, 1));

        assertEquals(StatutSeance.PLANIFIEE, creee.getStatut());
        assertEquals("Alice Martin", creee.getPhotographe().getNom());
        assertEquals(1, dao.seances.size());
    }

    @Test
    @DisplayName("Séance dans le passé, ou commençant maintenant : refus 400")
    void creerDansLePasse() {
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("2026-11-18", "14:00:00", 60, 1)));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("2026-11-19", "09:00:00", 60, 1)));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("2026-11-19", "10:00:00", 60, 1)));
        assertTrue(dao.seances.isEmpty());
    }

    @Test
    @DisplayName("Séance aujourd'hui mais plus tard : acceptée")
    void creerAujourdhuiPlusTard() {
        service.creer(donnees("2026-11-19", "10:30:00", 60, 1));
        assertEquals(1, dao.seances.size());
    }

    @Test
    @DisplayName("Photographe inactif ou inconnu : refus 409")
    void creerAvecPhotographeInactif() {
        assertThrows(ConflitException.class, () -> service.creer(donnees("2026-11-20", "14:00:00", 60, 4)));
        assertThrows(ConflitException.class, () -> service.creer(donnees("2026-11-20", "14:00:00", 60, 99)));
    }

    @Test
    @DisplayName("Champ manquant, durée nulle, type ou client inconnu : refus 400")
    void creerAvecDonneesInvalides() {
        Map<String, String> sansLieu = donnees("2026-11-20", "14:00:00", 60, 1);
        sansLieu.remove("lieu");
        assertThrows(DonneesInvalidesException.class, () -> service.creer(sansLieu));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("2026-11-20", "14:00:00", 0, 1)));
        Map<String, String> typeInconnu = donnees("2026-11-20", "14:00:00", 60, 1);
        typeInconnu.put("typeSeanceId", "42");
        assertThrows(DonneesInvalidesException.class, () -> service.creer(typeInconnu));
        Map<String, String> dateFausse = donnees("2026-02-30", "14:00:00", 60, 1);
        assertThrows(DonneesInvalidesException.class, () -> service.creer(dateFausse));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(null));
    }

    // ------------------------------------------------------------------
    // Chevauchements
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Chevauchement partiel avec une séance du même photographe : refus 409")
    void chevauchementPartiel() {
        dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 120, StatutSeance.CONFIRMEE); // 14h00 - 16h00

        assertThrows(ConflitException.class, () -> service.creer(donnees("2026-11-20", "15:00:00", 120, 1)));
        assertThrows(ConflitException.class, () -> service.creer(donnees("2026-11-20", "13:00:00", 90, 1)));
    }

    @Test
    @DisplayName("Séance incluse dans une autre, ou qui l'englobe : refus 409")
    void chevauchementInclus() {
        dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 240, StatutSeance.PLANIFIEE); // 14h00 - 18h00

        assertThrows(ConflitException.class, () -> service.creer(donnees("2026-11-20", "15:00:00", 30, 1)));
        assertThrows(ConflitException.class, () -> service.creer(donnees("2026-11-20", "13:00:00", 360, 1)));
    }

    @Test
    @DisplayName("Séances bout à bout : acceptées")
    void seancesBoutABout() {
        dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 120, StatutSeance.CONFIRMEE); // 14h00 - 16h00

        service.creer(donnees("2026-11-20", "16:00:00", 60, 1));
        service.creer(donnees("2026-11-20", "12:00:00", 120, 1));
        assertEquals(3, dao.seances.size());
    }

    @Test
    @DisplayName("Séance annulée ou d'un autre photographe sur le créneau : pas de conflit")
    void chevauchementIgnore() {
        dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 120, StatutSeance.ANNULEE);
        dao.ajouter(2, LocalDateTime.of(2026, 11, 20, 14, 0), 120, StatutSeance.CONFIRMEE);

        service.creer(donnees("2026-11-20", "14:00:00", 120, 1));
        assertEquals(3, dao.seances.size());
    }

    // ------------------------------------------------------------------
    // Modification
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Modifier une séance sans changer son créneau : pas de conflit avec elle-même")
    void modifierSansConflitAvecElleMeme() {
        Seance s = dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 120, StatutSeance.PLANIFIEE);
        Map<String, String> d = avecStatut(donnees("2026-11-20", "14:00:00", 150, 1), StatutSeance.PLANIFIEE);

        assertEquals(150, service.modifier(s.getId(), d).getDuree());
    }

    @Test
    @DisplayName("Déplacer une séance sur le créneau d'une autre : refus 409")
    void modifierVersCreneauOccupe() {
        dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 9, 0), 120, StatutSeance.CONFIRMEE);
        Seance s = dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 60, StatutSeance.PLANIFIEE);

        assertThrows(ConflitException.class,
                () -> service.modifier(s.getId(), avecStatut(donnees("2026-11-20", "10:00:00", 60, 1), StatutSeance.PLANIFIEE)));
    }

    @Test
    @DisplayName("Déplacer une séance dans le passé : refus 400")
    void modifierVersLePasse() {
        Seance s = dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 60, StatutSeance.PLANIFIEE);

        assertThrows(DonneesInvalidesException.class,
                () -> service.modifier(s.getId(), avecStatut(donnees("2026-11-18", "14:00:00", 60, 1), StatutSeance.PLANIFIEE)));
    }

    @Test
    @DisplayName("Modifier une séance terminée ou annulée : refus 409")
    void modifierSeanceDefinitive() {
        Seance terminee = dao.ajouter(1, LocalDateTime.of(2026, 11, 10, 14, 0), 60, StatutSeance.TERMINEE);
        Seance annulee = dao.ajouter(1, LocalDateTime.of(2026, 11, 25, 14, 0), 60, StatutSeance.ANNULEE);

        assertThrows(ConflitException.class,
                () -> service.modifier(terminee.getId(), avecStatut(donnees("2026-11-10", "14:00:00", 90, 1), StatutSeance.TERMINEE)));
        assertThrows(ConflitException.class,
                () -> service.modifier(annulee.getId(), avecStatut(donnees("2026-11-25", "14:00:00", 90, 1), StatutSeance.ANNULEE)));
    }

    @Test
    @DisplayName("Terminer par modification une séance passée restée confirmée : accepté")
    void modifierPourTerminerSeancePassee() {
        Seance s = dao.ajouter(1, LocalDateTime.of(2026, 11, 17, 14, 0), 120, StatutSeance.CONFIRMEE);

        Seance modifiee = service.modifier(s.getId(), avecStatut(donnees("2026-11-17", "14:00:00", 120, 1), StatutSeance.TERMINEE));
        assertEquals(StatutSeance.TERMINEE, modifiee.getStatut());
    }

    // ------------------------------------------------------------------
    // Statuts
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Transitions autorisées : planifiée → confirmée → terminée, et annulations")
    void transitionsAutorisees() {
        Seance future = dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 60, StatutSeance.PLANIFIEE);
        assertEquals(StatutSeance.CONFIRMEE, service.changerStatut(future.getId(), "CONFIRMEE").getStatut());
        assertEquals(StatutSeance.ANNULEE, service.changerStatut(future.getId(), "annulee").getStatut());

        Seance aAnnuler = dao.ajouter(1, LocalDateTime.of(2026, 11, 21, 14, 0), 60, StatutSeance.PLANIFIEE);
        assertEquals(StatutSeance.ANNULEE, service.changerStatut(aAnnuler.getId(), "ANNULEE").getStatut());

        Seance passee = dao.ajouter(1, LocalDateTime.of(2026, 11, 18, 14, 0), 60, StatutSeance.CONFIRMEE);
        assertEquals(StatutSeance.TERMINEE, service.changerStatut(passee.getId(), "TERMINEE").getStatut());
    }

    @Test
    @DisplayName("Transitions interdites : refus 409")
    void transitionsInterdites() {
        Seance planifiee = dao.ajouter(1, LocalDateTime.of(2026, 11, 18, 14, 0), 60, StatutSeance.PLANIFIEE);
        Seance terminee = dao.ajouter(1, LocalDateTime.of(2026, 11, 10, 14, 0), 60, StatutSeance.TERMINEE);
        Seance annulee = dao.ajouter(1, LocalDateTime.of(2026, 11, 12, 14, 0), 60, StatutSeance.ANNULEE);

        assertThrows(ConflitException.class, () -> service.changerStatut(planifiee.getId(), "TERMINEE"));
        assertThrows(ConflitException.class, () -> service.changerStatut(terminee.getId(), "ANNULEE"));
        assertThrows(ConflitException.class, () -> service.changerStatut(annulee.getId(), "CONFIRMEE"));
    }

    @Test
    @DisplayName("Terminer une séance pas encore finie, même commencée : refus 409")
    void terminerAvantLaFin() {
        Seance enCours = dao.ajouter(1, LocalDateTime.of(2026, 11, 19, 9, 30), 60, StatutSeance.CONFIRMEE); // fin 10h30
        Seance future = dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 60, StatutSeance.CONFIRMEE);
        Seance finiePile = dao.ajouter(2, LocalDateTime.of(2026, 11, 19, 9, 0), 60, StatutSeance.CONFIRMEE); // fin 10h00

        assertThrows(ConflitException.class, () -> service.changerStatut(enCours.getId(), "TERMINEE"));
        assertThrows(ConflitException.class, () -> service.changerStatut(future.getId(), "TERMINEE"));
        assertEquals(StatutSeance.TERMINEE, service.changerStatut(finiePile.getId(), "TERMINEE").getStatut());
    }

    @Test
    @DisplayName("Statut absent ou inconnu : refus 400 ; séance inconnue : 404")
    void statutInvalide() {
        Seance s = dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 60, StatutSeance.PLANIFIEE);

        assertThrows(DonneesInvalidesException.class, () -> service.changerStatut(s.getId(), null));
        assertThrows(DonneesInvalidesException.class, () -> service.changerStatut(s.getId(), "EN_COURS"));
        assertThrows(RessourceIntrouvableException.class, () -> service.changerStatut(42, "CONFIRMEE"));
    }

    // ------------------------------------------------------------------
    // Suppression et lecture
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Suppression d'une séance sans facture ; refus 409 si elle a une facture ; 404 si inconnue")
    void supprimer() {
        Seance libre = dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 60, StatutSeance.PLANIFIEE);
        Seance facturee = dao.ajouter(1, LocalDateTime.of(2026, 11, 10, 14, 0), 60, StatutSeance.TERMINEE);
        dao.seancesFacturees.add(facturee.getId());

        service.supprimer(libre.getId());
        assertEquals(1, dao.seances.size());
        assertThrows(ConflitException.class, () -> service.supprimer(facturee.getId()));
        assertThrows(RessourceIntrouvableException.class, () -> service.supprimer(42));
    }

    @Test
    @DisplayName("Planning : une seule des deux dates, date invalide ou période inversée : refus 400")
    void listerAvecPeriodeInvalide() {
        assertThrows(DonneesInvalidesException.class, () -> service.lister(null, null, "2026-11-16", null));
        assertThrows(DonneesInvalidesException.class, () -> service.lister(null, null, "16/11/2026", "2026-11-22"));
        assertThrows(DonneesInvalidesException.class, () -> service.lister(null, null, "2026-11-22", "2026-11-16"));
        dao.ajouter(1, LocalDateTime.of(2026, 11, 20, 14, 0), 60, StatutSeance.PLANIFIEE);
        assertEquals(1, service.lister(null, 1L, "2026-11-16", "2026-11-22").size());
    }
}
