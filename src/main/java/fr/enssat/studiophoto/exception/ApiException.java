package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/** erreur renvoyée au client : un code HTTP + un message (mappée en JSON par ApiExceptionMapper) */
public abstract class ApiException extends RuntimeException {

    private final Response.Status statut;

    protected ApiException(Response.Status statut, String message) {
        super(message);
        this.statut = statut;
    }

    public Response.Status getStatut() {
        return statut;
    }
}
