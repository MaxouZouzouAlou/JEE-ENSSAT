package fr.enssat.studiophoto.exception;

/**
 * Action à confirmer car elle a des conséquences sur d'autres données : 409.
 * Ex. désactiver un photographe qui a des séances à venir (elles seront annulées).
 * Le client renvoie la même requête avec {@code ?confirmer=true} pour l'exécuter.
 *
 * <p>Convertie par {@link ConfirmationRequiseExceptionMapper} : le corps JSON habituel
 * contient en plus le nombre de séances concernées.</p>
 * <pre>{ "status": 409, "erreur": "Conflict", "message": "...", "nombreSeances": 2 }</pre>
 */
public class ConfirmationRequiseException extends ConflitException {

    private final int nombreSeances;

    public ConfirmationRequiseException(String message, int nombreSeances) {
        super(message);
        this.nombreSeances = nombreSeances;
    }

    public int getNombreSeances() {
        return nombreSeances;
    }
}
