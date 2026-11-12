package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Erreurs levées par Jersey lui-même (route inconnue, JSON illisible, méthode non
 * autorisée...) : on garde leur code HTTP mais on renvoie le même format JSON
 * que les erreurs métier.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response.StatusType statut = exception.getResponse().getStatusInfo();
        String message = switch (statut.getStatusCode()) {
            case 400 -> "Requête invalide : vérifiez le format JSON envoyé.";
            case 404 -> "Ressource introuvable.";
            case 405 -> "Méthode HTTP non autorisée sur cette ressource.";
            case 415 -> "Format non pris en charge : envoyez du JSON (Content-Type: application/json).";
            default -> statut.getReasonPhrase();
        };
        return ErreurReponse.creer(statut, message);
    }
}
