package fr.enssat.studiophoto.exception;

/** Élément introuvable (identifiant inconnu) : HTTP 404. */
public class RessourceIntrouvableException extends ExceptionMetier {

    public RessourceIntrouvableException(String message) {
        super(message);
    }

    @Override
    public int getStatus() {
        return 404;
    }
}
