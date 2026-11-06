package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;

/** 404 : l'élément n'existe pas */
public class IntrouvableException extends ApiException {

    public IntrouvableException(String message) {
        super(Response.Status.NOT_FOUND, message);
    }

    // new IntrouvableException("Facture", 42) -> "Facture 42 introuvable."
    public IntrouvableException(String element, long id) {
        this(element + " " + id + " introuvable.");
    }
}
