package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.FactureDAO;
import fr.enssat.studiophoto.dao.FactureDAOImpl;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.IntrouvableException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.Facture;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.StatutPaiement;
import fr.enssat.studiophoto.model.StatutSeance;

import java.time.Clock;
import java.time.LocalDate;

/** logique métier des factures : génération, numérotation, copie des infos client */
public class FactureService {

    // nb de tentatives si 2 générations simultannées veulent le même numéro
    private static final int MAX_ESSAIS_NUMERO = 3;
    // numéros sur 4 chiffres, donc 9999 factures max par an
    private static final int MAX_NUMERO_ANNUEL = 9999;

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
        figer(facture, seance);
        return enregistrer(facture);
    }

    /**
     * copie les infos client + prestation dans la facture, pour qu'elle ne bouge plus
     * si le client / le type / le photographe sont modifiés après.
     */
    private void figer(Facture facture, Seance seance) {
        Client client = seance.getClient();
        facture.setClientDesignation(client.designation());
        facture.setClientAdresse(client.getAdressePostale());
        facture.setClientEmail(client.getEmail());
        // SIREN/TVA seulement pour une entreprise (NULL sinon)
        if (client instanceof ClientEntreprise entreprise) {
            facture.setClientSirenTva(entreprise.getSirenTva());
        }

        facture.setPrestationDate(seance.getDate());
        facture.setPrestationHeure(seance.getHeureDebut());
        facture.setPrestationDuree(seance.getDuree());
        facture.setPrestationType(seance.getTypeSeance().getLibelle());
        facture.setPhotographeNom(seance.getPhotographe().getNom());
    }

    // calcule le numéro puis insère. Si 2 générations prennent le même numéro, l'UNIQUE en base
    // refuse la 2e (409 du DAO) et on recommence avec le numéro suivant
    private Facture enregistrer(Facture facture) {
        for (int essai = 1; ; essai++) {
            facture.setNumero(prochainNumero(facture.getDateEmission().getYear()));
            try {
                return factureDAO.create(facture);
            } catch (ConflitException e) {
                // conflit qui ne vient pas du numéro (séance facturée entre temps) : on ne retente pas
                boolean dejaFacturee = factureDAO.findBySeanceId(facture.getSeanceId()).isPresent();
                if (dejaFacturee || essai >= MAX_ESSAIS_NUMERO) {
                    throw e;
                }
            }
        }
    }

    /** numéro qui suit le dernier de l'année (FAC-2026-0007 -> FAC-2026-0008), 0001 pour la première */
    String prochainNumero(int annee) {
        int suivant = factureDAO.findDernierNumero(annee)
                .map(numero -> Integer.parseInt(numero.substring(numero.lastIndexOf('-') + 1)) + 1)
                .orElse(1);
        if (suivant > MAX_NUMERO_ANNUEL) {
            throw new ConflitException("Les " + MAX_NUMERO_ANNUEL + " numéros de facture de " + annee
                    + " sont épuisés");
        }
        return String.format("FAC-%d-%04d", annee, suivant);
    }
}
