package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** 400 / 404 / 409 : on renvoie le code de l'exception et son message */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException exception) {
        return ErreurJson.reponse(exception.getStatut(), exception.getMessage());
    }
}
