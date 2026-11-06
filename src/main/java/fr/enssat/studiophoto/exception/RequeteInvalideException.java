package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/** 400 : requête incorrecte ou incomplète */
public class RequeteInvalideException extends ApiException {

    public RequeteInvalideException(String message) {
        super(Response.Status.BAD_REQUEST, message);
    }
}
