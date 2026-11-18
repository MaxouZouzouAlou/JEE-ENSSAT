package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Convertit toute {@link MetierException} en réponse JSON avec le code HTTP qu'elle porte. */
@Provider
public class MetierExceptionMapper implements ExceptionMapper<MetierException> {

    @Override
    public Response toResponse(MetierException exception) {
        return ErreurReponse.creer(exception.getStatus(), exception.getMessage());
    }
}
