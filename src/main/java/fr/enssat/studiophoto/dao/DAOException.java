package fr.enssat.studiophoto.dao;

/**
 * Erreur technique d'accès à la base (connexion impossible, requête en échec).
 * Non métier : elle donne une réponse 500.
 */
public class DAOException extends RuntimeException {

    public DAOException(String message) {
        super(message);
    }

    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
