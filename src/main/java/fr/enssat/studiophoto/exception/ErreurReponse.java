package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/**
 * Format commun des réponses d'erreur de l'API (phase 0, P0.7) :
 * <pre>{ "status": 409, "erreur": "Conflict", "message": "Cet email est déjà utilisé." }</pre>
 * <ul>
 *   <li>{@code status} : code HTTP ;</li>
 *   <li>{@code erreur} : libellé standard du code HTTP ;</li>
 *   <li>{@code message} : explication en français, affichée telle quelle par le front (api.js).</li>
 * </ul>
 */
public class ErreurReponse {

    private int status;
    private String erreur;
    private String message;

    public ErreurReponse() {
    }

    public ErreurReponse(int status, String message) {
        this.status = status;
        Response.Status statut = Response.Status.fromStatusCode(status);
        this.erreur = statut != null ? statut.getReasonPhrase() : "Erreur";
        this.message = message;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getErreur() { return erreur; }
    public void setErreur(String erreur) { this.erreur = erreur; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
