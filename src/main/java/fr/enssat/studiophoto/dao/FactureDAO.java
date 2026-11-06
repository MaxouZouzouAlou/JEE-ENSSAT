package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.model.Facture;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.StatutPaiement;

import java.util.List;
import java.util.Optional;

/** accès à la table facture */
public interface FactureDAO {

    /** toutes les factures, la plus récente en premier */
    List<Facture> findAll();

    Optional<Facture> findById(long id);

    /** facture d'une séance (une seule max, seance_id est UNIQUE) */
    Optional<Facture> findBySeanceId(long seanceId);

    /**
     * insère la facture et remplit son id.
     * ConflitException (409) si le numéro ou la séance existent déjà.
     */
    Facture create(Facture facture);

    /**
     * change le statut seulement si la facture est encore au statut "attendu".
     * false si rien n'a été modifié (id inconnu ou statut changé entre temps).
     */
    boolean updateStatut(long id, StatutPaiement attendu, StatutPaiement nouveau);

    /** plus grand numéro de l'année (ex FAC-2026-0007), vide si aucune facutre */
    Optional<String> findDernierNumero(int annee);

    /** séance avec son type, son photographe et son client, en lecture seule (sert à facturer) */
    Optional<Seance> findSeance(long seanceId);
}
