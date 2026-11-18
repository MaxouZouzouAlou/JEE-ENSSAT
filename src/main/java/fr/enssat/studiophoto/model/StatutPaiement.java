package fr.enssat.studiophoto.model;

/** statut de paiement d'une facture (colonne statut_paiement), seul EN_ATTENTE peu évoluer */
public enum StatutPaiement {
    EN_ATTENTE,
    PAYEE,
    ANNULEE;

    // true si on a le droit de passer de ce statut à cible
    public boolean peutPasserA(StatutPaiement cible) {
        return this == EN_ATTENTE && (cible == PAYEE || cible == ANNULEE);
    }
}
