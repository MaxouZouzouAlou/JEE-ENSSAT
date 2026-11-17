package fr.enssat.studiophoto.dto;

import fr.enssat.studiophoto.model.Photographe;

/**
 * Réponse de {@code PATCH /api/photographes/{id}/statut} : le photographe à jour et le nombre
 * de séances à venir annulées (0 pour une activation ou s'il n'y en avait pas).
 */
public class ResultatChangementStatut {

    private Photographe photographe;
    private int seancesAnnulees;

    public ResultatChangementStatut() {
    }

    public ResultatChangementStatut(Photographe photographe, int seancesAnnulees) {
        this.photographe = photographe;
        this.seancesAnnulees = seancesAnnulees;
    }

    public Photographe getPhotographe() { return photographe; }
    public void setPhotographe(Photographe photographe) { this.photographe = photographe; }

    public int getSeancesAnnulees() { return seancesAnnulees; }
    public void setSeancesAnnulees(int seancesAnnulees) { this.seancesAnnulees = seancesAnnulees; }
}
