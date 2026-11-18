package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Corps de requête illisible (JSON mal formé, valeur du mauvais type...) : JSON-B
 * lève une {@link ProcessingException} que Jersey transformerait sinon en page HTML 500.
 */
@Provider
public class ProcessingExceptionMapper implements ExceptionMapper<ProcessingException> {

    @Override
    public Response toResponse(ProcessingException exception) {
        return ErreurReponse.creer(Response.Status.BAD_REQUEST,
                "JSON invalide : le corps de la requête n'a pas pu être lu.");
    }
}
