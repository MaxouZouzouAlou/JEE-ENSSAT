package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** body JSON que Jersey n'arrive pas à lire (JSON cassé, enum inconnue...) : 400 a lieu d'un 500 HTML */
@Provider
public class JsonIllisibleMapper implements ExceptionMapper<ProcessingException> {

    @Override
    public Response toResponse(ProcessingException exception) {
        return ErreurJson.reponse(Response.Status.BAD_REQUEST,
                "Corps de la requête illisible : vérifiez le JSON envoyé et les valeurs autorisées.");
    }
}
