package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/** 409 : action impossible vu l'état actuel des données */
public class ConflitException extends ApiException {

    public ConflitException(String message) {
        super(Response.Status.CONFLICT, message);
    }
}
