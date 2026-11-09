package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.FactureDAO;
import fr.enssat.studiophoto.dao.FactureDAOImpl;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.IntrouvableException;
import fr.enssat.studiophoto.model.Facture;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.StatutPaiement;
import fr.enssat.studiophoto.model.StatutSeance;

import java.time.Clock;
import java.time.LocalDate;

/** logique métier des factures : génération */
public class FactureService {

    private final FactureDAO factureDAO;
    private final Clock horloge;

    public FactureService() {
        this(new FactureDAOImpl(), Clock.systemDefaultZone());
    }

    // pour les tests : DAO mocké et date fixée
    public FactureService(FactureDAO factureDAO, Clock horloge) {
        this.factureDAO = factureDAO;
        this.horloge = horloge;
    }

    /**
     * génère la facture d'une séance TERMINEE.
     * 404 si la séance n'existe pas, 409 si elle n'est pas terminée ou déja facturée.
     */
    public Facture generer(long seanceId) {
        Seance seance = factureDAO.findSeance(seanceId)
                .orElseThrow(() -> new IntrouvableException("Séance", seanceId));
        if (seance.getStatut() != StatutSeance.TERMINEE) {
            throw new ConflitException("La séance " + seanceId + " est " + seance.getStatut()
                    + " : seule une séance TERMINEE peut être facturée");
        }
        if (factureDAO.findBySeanceId(seanceId).isPresent()) {
            throw new ConflitException("La séance " + seanceId + " a déjà une facture");
        }

        Tarification.Montants montants =
                Tarification.calculer(seance.getTypeSeance().getTarifHoraireHT(), seance.getDuree());

        Facture facture = new Facture();
        facture.setSeanceId(seanceId);
        facture.setDateEmission(LocalDate.now(horloge));
        facture.setStatutPaiement(StatutPaiement.EN_ATTENTE);
        facture.setMontantHT(montants.ht());
        facture.setTauxTVA(montants.tauxTva());
        facture.setMontantTTC(montants.ttc());
        return factureDAO.create(facture);
    }
}
