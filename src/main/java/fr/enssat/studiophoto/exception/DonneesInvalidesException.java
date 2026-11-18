package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/** Données reçues incomplètes ou mal formées (champ obligatoire absent, SIREN invalide...) : 400. */
public class DonneesInvalidesException extends MetierException {

    public DonneesInvalidesException(String message) {
        super(message);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
