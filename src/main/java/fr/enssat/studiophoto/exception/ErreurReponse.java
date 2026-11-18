package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Corps JSON commun à toutes les réponses en erreur :
 * {@code {"status": 404, "erreur": "Not Found", "message": "Client 42 introuvable."}}.
 * C'est le format lu par {@code lireMessageErreur} dans {@code js/api.js}.
 */
public class ErreurReponse {

    private int status;
    private String erreur;
    private String message;

    public ErreurReponse() {
    }

    public ErreurReponse(int status, String erreur, String message) {
        this.status = status;
        this.erreur = erreur;
        this.message = message;
    }

    /** Construit la réponse HTTP complète (code + corps JSON). */
    public static Response creer(Response.StatusType statut, String message) {
        ErreurReponse corps = new ErreurReponse(statut.getStatusCode(), statut.getReasonPhrase(), message);
        return Response.status(statut).entity(corps).type(MediaType.APPLICATION_JSON).build();
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getErreur() { return erreur; }
    public void setErreur(String erreur) { this.erreur = erreur; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
