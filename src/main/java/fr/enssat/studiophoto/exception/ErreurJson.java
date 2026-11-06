package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** coprs JSON des erreurs : {"status": 404, "erreur": "Not Found", "message": "..."}, le front affiche message */
public record ErreurJson(int status, String erreur, String message) {

    public static Response reponse(Response.Status statut, String message) {
        ErreurJson corps = new ErreurJson(statut.getStatusCode(), statut.getReasonPhrase(), message);
        return Response.status(statut).type(MediaType.APPLICATION_JSON).entity(corps).build();
    }
}
