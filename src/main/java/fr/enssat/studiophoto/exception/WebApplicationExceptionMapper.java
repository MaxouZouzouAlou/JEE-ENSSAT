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
        return ErreurReponse.creer(statut, message(exception, statut.getStatusCode()));
    }

    /**
     * Message écrit par le développeur (ex. {@code new BadRequestException("Les dates doivent...")})
     * s'il y en a un ; sinon message par défaut selon le code. Jersey met "HTTP 404 Not Found"
     * comme message quand aucun n'est fourni.
     */
    static String message(WebApplicationException exception, int code) {
        String message = exception.getMessage();
        if (message != null && !message.isBlank() && !message.startsWith("HTTP " + code)) {
            return message;
        }
        return switch (code) {
            case 400 -> "Requête invalide : vérifiez le format JSON envoyé.";
            case 404 -> "Ressource introuvable.";
            case 405 -> "Méthode HTTP non autorisée sur cette ressource.";
            case 415 -> "Format non pris en charge : envoyez du JSON (Content-Type: application/json).";
            default -> exception.getResponse().getStatusInfo().getReasonPhrase();
        };
    }
}
