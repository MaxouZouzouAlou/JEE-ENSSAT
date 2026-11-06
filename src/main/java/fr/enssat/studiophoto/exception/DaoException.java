package fr.enssat.studiophoto.exception;

/** erreur d'accès à la base (unchecked), renvoyé en 500 */
public class DaoException extends RuntimeException {

    public DaoException(String message) {
        super(message);
    }

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
