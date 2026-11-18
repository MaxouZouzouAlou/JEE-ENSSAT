package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/**
 * Action impossible dans l'état actuel des données (client ayant des séances,
 * transition de statut interdite, chevauchement...) : 409.
 */
public class ConflitException extends MetierException {

    public ConflitException(String message) {
        super(message);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.CONFLICT;
    }
}
