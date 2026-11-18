package fr.enssat.studiophoto.exception;

/**
 * Erreur technique d'accès à la base (connexion impossible, requête en échec).
 * Exception non vérifiée : les couches supérieures n'ont pas à la déclarer,
 * elle sera convertie en réponse HTTP 500.
 */
public class DaoException extends RuntimeException {

    public DaoException(String message) {
        super(message);
    }

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
