package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/** 500 : le détail va dans les logs, le client reçoit juste un message générique */
@Provider
public class DaoExceptionMapper implements ExceptionMapper<DaoException> {

    private static final Logger LOGGER = Logger.getLogger(DaoExceptionMapper.class.getName());

    @Override
    public Response toResponse(DaoException exception) {
        LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        return ErreurJson.reponse(Response.Status.INTERNAL_SERVER_ERROR,
                "Erreur d'accès aux données, réessayez plus tard.");
    }
}
