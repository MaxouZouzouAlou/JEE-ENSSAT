package fr.enssat.studiophoto.exception;

/**
 * Action impossible dans l'état actuel des données : HTTP 409.
 * Ex. email déjà utilisé, suppression d'un élément encore référencé, transition de statut interdite.
 */
public class ConflitException extends ExceptionMetier {

    public ConflitException(String message) {
        super(message);
    }

    @Override
    public int getStatus() {
        return 409;
    }
}
