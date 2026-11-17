package fr.enssat.studiophoto.dto;

/**
 * Corps de {@code PATCH /api/photographes/{id}/statut} : <pre>{ "actif": false }</pre>
 */
public class ChangementStatutPhotographe {

    private Boolean actif;

    public ChangementStatutPhotographe() {
    }

    public ChangementStatutPhotographe(Boolean actif) {
        this.actif = actif;
    }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
}
