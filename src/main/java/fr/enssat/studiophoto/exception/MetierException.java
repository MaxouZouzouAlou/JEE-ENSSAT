package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception métier : règle de gestion non respectée. Chaque sous-classe porte
 * le code HTTP à renvoyer, appliqué par {@link MetierExceptionMapper}.
 */
public abstract class MetierException extends RuntimeException {

    protected MetierException(String message) {
        super(message);
    }

    /** Code HTTP correspondant à l'erreur. */
    public abstract Response.Status getStatus();
}
