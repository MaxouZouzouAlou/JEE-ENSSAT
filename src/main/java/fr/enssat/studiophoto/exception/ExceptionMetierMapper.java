package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Convertit toute {@link ExceptionMetier} en réponse HTTP au format {@link ErreurReponse}.
 * Enregistré automatiquement par {@code Main} (package {@code exception} scanné).
 *
 * <p>Les autres erreurs (JSON mal formé, erreur technique inattendue) sont à traiter
 * par le lot 5 (tâche 5.7) avec des mappers supplémentaires, au même format.</p>
 */
@Provider
public class ExceptionMetierMapper implements ExceptionMapper<ExceptionMetier> {

    @Override
    public Response toResponse(ExceptionMetier exception) {
        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(exception.versReponse())
                .build();
    }
}
