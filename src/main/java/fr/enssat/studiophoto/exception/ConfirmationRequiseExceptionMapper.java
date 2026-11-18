package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Réponse 409 d'une {@link ConfirmationRequiseException} : format commun {@link ErreurReponse}
 * complété du champ {@code nombreSeances}. JAX-RS choisit ce mapper plutôt que
 * {@link MetierExceptionMapper} car il vise un type d'exception plus précis.
 */
@Provider
public class ConfirmationRequiseExceptionMapper implements ExceptionMapper<ConfirmationRequiseException> {

    @Override
    public Response toResponse(ConfirmationRequiseException exception) {
        Response.Status statut = exception.getStatus();
        Corps corps = new Corps(statut.getStatusCode(), statut.getReasonPhrase(),
                exception.getMessage(), exception.getNombreSeances());
        return Response.status(statut).entity(corps).type(MediaType.APPLICATION_JSON).build();
    }

    /** Corps JSON : {@code {status, erreur, message, nombreSeances}}. */
    public static class Corps extends ErreurReponse {

        private int nombreSeances;

        public Corps() {
        }

        public Corps(int status, String erreur, String message, int nombreSeances) {
            super(status, erreur, message);
            this.nombreSeances = nombreSeances;
        }

        public int getNombreSeances() { return nombreSeances; }
        public void setNombreSeances(int nombreSeances) { this.nombreSeances = nombreSeances; }
    }
}
