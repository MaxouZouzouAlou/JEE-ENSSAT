package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Erreur d'accès à la base : 500. Le détail technique est écrit dans les logs
 * du serveur mais n'est pas envoyé au client.
 */
@Provider
public class DaoExceptionMapper implements ExceptionMapper<DaoException> {

    private static final Logger LOGGER = Logger.getLogger(DaoExceptionMapper.class.getName());

    @Override
    public Response toResponse(DaoException exception) {
        LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        return ErreurReponse.creer(Response.Status.INTERNAL_SERVER_ERROR,
                "Erreur d'accès aux données, réessayez plus tard.");
    }
}
