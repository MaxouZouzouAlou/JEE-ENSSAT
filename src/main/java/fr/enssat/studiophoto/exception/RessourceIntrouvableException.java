package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/** L'élément demandé n'existe pas (ex. client 42 inconnu) : 404. */
public class RessourceIntrouvableException extends MetierException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }

    /** Message standard, ex. {@code new RessourceIntrouvableException("Client", 42)} -> "Client 42 introuvable." */
    public RessourceIntrouvableException(String ressource, long id) {
        this(ressource + " " + id + " introuvable.");
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.NOT_FOUND;
    }
}
