package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.model.StatutPaiement;

/** body du PATCH /api/factures/{id}/statut : {"statut": "PAYEE"} */
public class StatutRequete {

    private StatutPaiement statut;

    public StatutRequete() {
    }

    public StatutPaiement getStatut() { return statut; }
    public void setStatut(StatutPaiement statut) { this.statut = statut; }
}
