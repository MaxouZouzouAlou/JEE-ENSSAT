package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dto.ResultatChangementStatut;
import fr.enssat.studiophoto.exception.ConfirmationRequiseException;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.exception.RessourceIntrouvableException;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.Specialite;
import fr.enssat.studiophoto.model.StatutSeance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests du service des photographes (lot 1), avec des DAO en mémoire et une horloge fixe. */
class PhotographeServiceTest {

    /** « Maintenant » pour tous les tests : mardi 17 novembre 2026, 10h00. */
    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 17, 10, 0);
    private static final LocalDate AUJOURD_HUI = MAINTENANT.toLocalDate();

    private FauxPhotographeDAO photographeDAO;
    private FauxSeanceDAO seanceDAO;
    private PhotographeService service;

    private Photographe alice;

    @BeforeEach
    void preparer() {
        photographeDAO = new FauxPhotographeDAO();
        seanceDAO = new FauxSeanceDAO();
        ZoneId zone = ZoneId.of("Europe/Paris");
        Clock horloge = Clock.fixed(MAINTENANT.atZone(zone).toInstant(), zone);
        service = new PhotographeService(photographeDAO, new FauxSpecialiteDAO(), seanceDAO, horloge);
        alice = photographeDAO.ajouter("Alice Martin", "alice.martin@studio.example", true);
    }

    private static Photographe donnees(String nom, String email, Long... specialites) {
        Photographe p = new Photographe(null, nom, email, true);
        for (Long id : specialites) {
            p.getSpecialites().add(new Specialite(id, null));
        }
        return p;
    }

    private static List<Long> idsSpecialites(Photographe p) {
        return p.getSpecialites().stream().map(Specialite::getId).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Création et modification
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Création valide : actif, identifiant attribué, données nettoyées, spécialités complètes")
    void creerPhotographeValide() {
        Photographe cree = service.creer(donnees("  Bruno Leroy ", " Bruno.Leroy@Studio.example ", 1L, 3L));

        assertNotNull(cree.getId());
        assertTrue(cree.isActif());
        assertEquals("Bruno Leroy", cree.getNom());
        assertEquals("bruno.leroy@studio.example", cree.getEmail());
        assertEquals(List.of(1L, 3L), idsSpecialites(cree));
        assertEquals("Reportage", cree.getSpecialites().get(0).getLibelle());
    }

    @Test
    @DisplayName("Création toujours active, même si le JSON reçu indique actif = false")
    void creerIgnoreActif() {
        Photographe d = donnees("Bruno Leroy", "bruno@studio.example");
        d.setActif(false);
        assertTrue(service.creer(d).isActif());
    }

    @Test
    @DisplayName("Nom manquant ou vide : refus 400")
    void creerSansNom() {
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees(null, "x@studio.example")));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("   ", "x@studio.example")));
    }

    @Test
    @DisplayName("Email manquant ou mal formé : refus 400")
    void creerEmailInvalide() {
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("Bruno", null)));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("Bruno", "bruno.studio.example")));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("Bruno", "bruno@studio")));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("Bruno", "bru no@studio.example")));
    }

    @Test
    @DisplayName("Corps de requête absent : refus 400")
    void creerSansDonnees() {
        assertThrows(DonneesInvalidesException.class, () -> service.creer(null));
    }

    @Test
    @DisplayName("Email en double (casse différente) : refus 409 et rien n'est créé")
    void creerEmailEnDouble() {
        ConflitException e = assertThrows(ConflitException.class,
                () -> service.creer(donnees("Autre Alice", "ALICE.Martin@studio.example")));

        assertTrue(e.getMessage().contains("Alice Martin"));
        assertEquals(1, photographeDAO.photographes.size());
    }

    @Test
    @DisplayName("Spécialité inconnue ou sans identifiant : refus 400")
    void creerSpecialiteInconnue() {
        assertThrows(DonneesInvalidesException.class, () -> service.creer(donnees("Bruno", "b@studio.example", 99L)));
        Photographe sansId = donnees("Bruno", "b@studio.example");
        sansId.getSpecialites().add(new Specialite(null, "Reportage"));
        assertThrows(DonneesInvalidesException.class, () -> service.creer(sansId));
    }

    @Test
    @DisplayName("Spécialités en double : gardées une seule fois")
    void creerSpecialitesEnDouble() {
        Photographe cree = service.creer(donnees("Bruno", "b@studio.example", 2L, 2L, 1L));
        assertEquals(List.of(2L, 1L), idsSpecialites(cree));
    }

    @Test
    @DisplayName("Modification : un photographe garde son propre email et change ses spécialités")
    void modifierGardeSonEmail() {
        Photographe modifie = service.modifier(alice.getId(), donnees("Alice Martin-Durand", "alice.martin@studio.example", 3L));

        assertEquals("Alice Martin-Durand", modifie.getNom());
        assertEquals(List.of(3L), idsSpecialites(modifie));
    }

    @Test
    @DisplayName("Modification avec l'email d'un autre photographe : refus 409")
    void modifierEmailDUnAutre() {
        Photographe bruno = photographeDAO.ajouter("Bruno Leroy", "bruno@studio.example", true);
        assertThrows(ConflitException.class,
                () -> service.modifier(bruno.getId(), donnees("Bruno Leroy", "alice.martin@studio.example")));
    }

    @Test
    @DisplayName("Modification : le statut actif n'est pas modifiable par cette route")
    void modifierNeChangePasLeStatut() {
        Photographe d = donnees("Alice Martin", "alice.martin@studio.example");
        d.setActif(false);
        assertTrue(service.modifier(alice.getId(), d).isActif());
    }

    @Test
    @DisplayName("Modification ou lecture d'un photographe inconnu : 404")
    void photographeInconnu() {
        assertThrows(RessourceIntrouvableException.class, () -> service.modifier(42, donnees("X", "x@studio.example")));
        assertThrows(RessourceIntrouvableException.class, () -> service.trouver(42));
    }

    @Test
    @DisplayName("Liste filtrée par statut")
    void listerParStatut() {
        photographeDAO.ajouter("David Moreau", "david@studio.example", false);

        assertEquals(2, service.lister(null).size());
        assertEquals(List.of("Alice Martin"), service.lister(true).stream().map(Photographe::getNom).collect(Collectors.toList()));
        assertEquals(List.of("David Moreau"), service.lister(false).stream().map(Photographe::getNom).collect(Collectors.toList()));
    }

    // ------------------------------------------------------------------
    // Activation / désactivation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Désactivation sans séance à venir : directe, sans confirmation")
    void desactiverSansSeanceAVenir() {
        ResultatChangementStatut r = service.changerStatut(alice.getId(), false, false);

        assertFalse(r.getPhotographe().isActif());
        assertEquals(0, r.getSeancesAnnulees());
        assertFalse(photographeDAO.photographes.get(alice.getId()).isActif());
    }

    @Test
    @DisplayName("Désactivation avec séances à venir, sans confirmation : 409 avec le nombre, rien ne change")
    void desactiverAvecSeancesAVenirSansConfirmation() {
        Seance demain = seanceDAO.ajouter(alice, AUJOURD_HUI.plusDays(1), LocalTime.of(14, 0), StatutSeance.PLANIFIEE);
        Seance plusTard = seanceDAO.ajouter(alice, AUJOURD_HUI.plusDays(20), LocalTime.of(9, 0), StatutSeance.CONFIRMEE);

        ConfirmationRequiseException e = assertThrows(ConfirmationRequiseException.class,
                () -> service.changerStatut(alice.getId(), false, false));

        assertEquals(409, e.getStatus());
        assertEquals(2, e.getNombreSeances());
        assertTrue(photographeDAO.photographes.get(alice.getId()).isActif());
        assertEquals(StatutSeance.PLANIFIEE, demain.getStatut());
        assertEquals(StatutSeance.CONFIRMEE, plusTard.getStatut());
    }

    @Test
    @DisplayName("Désactivation confirmée : photographe inactif et séances à venir annulées")
    void desactiverAvecConfirmation() {
        Seance demain = seanceDAO.ajouter(alice, AUJOURD_HUI.plusDays(1), LocalTime.of(14, 0), StatutSeance.PLANIFIEE);
        Seance plusTard = seanceDAO.ajouter(alice, AUJOURD_HUI.plusDays(20), LocalTime.of(9, 0), StatutSeance.CONFIRMEE);

        ResultatChangementStatut r = service.changerStatut(alice.getId(), false, true);

        assertFalse(r.getPhotographe().isActif());
        assertEquals(2, r.getSeancesAnnulees());
        assertEquals(StatutSeance.ANNULEE, demain.getStatut());
        assertEquals(StatutSeance.ANNULEE, plusTard.getStatut());
    }

    @Test
    @DisplayName("Désactivation confirmée : les séances passées ne sont pas annulées")
    void seancesPasseesNonAnnulees() {
        Seance terminee = seanceDAO.ajouter(alice, AUJOURD_HUI.minusDays(10), LocalTime.of(10, 0), StatutSeance.TERMINEE);
        Seance passeeConfirmee = seanceDAO.ajouter(alice, AUJOURD_HUI.minusDays(2), LocalTime.of(14, 0), StatutSeance.CONFIRMEE);
        Seance ceMatinPlanifiee = seanceDAO.ajouter(alice, AUJOURD_HUI, LocalTime.of(9, 0), StatutSeance.PLANIFIEE);
        Seance cetApresMidi = seanceDAO.ajouter(alice, AUJOURD_HUI, LocalTime.of(14, 0), StatutSeance.PLANIFIEE);

        ResultatChangementStatut r = service.changerStatut(alice.getId(), false, true);

        assertEquals(1, r.getSeancesAnnulees());
        assertEquals(StatutSeance.TERMINEE, terminee.getStatut());
        assertEquals(StatutSeance.CONFIRMEE, passeeConfirmee.getStatut());
        assertEquals(StatutSeance.PLANIFIEE, ceMatinPlanifiee.getStatut());
        assertEquals(StatutSeance.ANNULEE, cetApresMidi.getStatut());
    }

    @Test
    @DisplayName("Seules les séances passées ou déjà annulées : pas de confirmation demandée")
    void seancesPasseesOuAnnuleesNeBloquentPas() {
        seanceDAO.ajouter(alice, AUJOURD_HUI.minusDays(3), LocalTime.of(10, 0), StatutSeance.PLANIFIEE);
        seanceDAO.ajouter(alice, AUJOURD_HUI.plusDays(3), LocalTime.of(10, 0), StatutSeance.ANNULEE);
        seanceDAO.ajouter(alice, MAINTENANT, 60, StatutSeance.CONFIRMEE); // commence pile maintenant : déjà en cours

        ResultatChangementStatut r = service.changerStatut(alice.getId(), false, false);

        assertFalse(r.getPhotographe().isActif());
        assertEquals(0, r.getSeancesAnnulees());
    }

    @Test
    @DisplayName("Les séances des autres photographes ne sont ni comptées ni annulées")
    void seancesDesAutresPhotographesIntactes() {
        Photographe bruno = photographeDAO.ajouter("Bruno Leroy", "bruno@studio.example", true);
        Seance deBruno = seanceDAO.ajouter(bruno, AUJOURD_HUI.plusDays(2), LocalTime.of(10, 0), StatutSeance.PLANIFIEE);

        ResultatChangementStatut r = service.changerStatut(alice.getId(), false, false);

        assertEquals(0, r.getSeancesAnnulees());
        assertEquals(StatutSeance.PLANIFIEE, deBruno.getStatut());
    }

    @Test
    @DisplayName("Réactivation d'un photographe inactif")
    void reactiver() {
        Photographe david = photographeDAO.ajouter("David Moreau", "david@studio.example", false);

        ResultatChangementStatut r = service.changerStatut(david.getId(), true, false);

        assertTrue(r.getPhotographe().isActif());
        assertEquals(0, r.getSeancesAnnulees());
    }

    @Test
    @DisplayName("Changement de statut sans valeur « actif » : 400 ; photographe inconnu : 404")
    void changerStatutInvalide() {
        assertThrows(DonneesInvalidesException.class, () -> service.changerStatut(alice.getId(), null, false));
        assertThrows(RessourceIntrouvableException.class, () -> service.changerStatut(42, false, false));
    }

    // ------------------------------------------------------------------
    // Suppression
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Suppression d'un photographe sans séance")
    void supprimerSansSeance() {
        service.supprimer(alice.getId());
        assertTrue(photographeDAO.photographes.isEmpty());
    }

    @Test
    @DisplayName("Suppression refusée (409) dès qu'une séance existe, même passée ou annulée")
    void supprimerAvecSeances() {
        seanceDAO.ajouter(alice, AUJOURD_HUI.minusDays(30), LocalTime.of(10, 0), StatutSeance.ANNULEE);

        ConflitException e = assertThrows(ConflitException.class, () -> service.supprimer(alice.getId()));

        assertTrue(e.getMessage().contains("Désactivez-le"));
        assertEquals(1, photographeDAO.photographes.size());
    }

    @Test
    @DisplayName("Suppression d'un photographe inconnu : 404")
    void supprimerInconnu() {
        assertThrows(RessourceIntrouvableException.class, () -> service.supprimer(42));
    }
}
