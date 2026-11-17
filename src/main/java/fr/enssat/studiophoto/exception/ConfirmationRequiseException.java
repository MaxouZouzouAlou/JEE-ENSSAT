package fr.enssat.studiophoto.exception;

/**
 * Action à confirmer car elle a des conséquences sur d'autres données : HTTP 409.
 * Ex. désactiver un photographe qui a des séances à venir (elles seront annulées).
 * Le client renvoie la même requête avec {@code ?confirmer=true} pour l'exécuter.
 *
 * <p>La réponse contient en plus le nombre d'éléments concernés :</p>
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

    @Override
    public ErreurReponse versReponse() {
        return new Reponse(getStatus(), getMessage(), nombreSeances);
    }

    /** Format commun complété du nombre de séances concernées. */
    public static class Reponse extends ErreurReponse {

        private int nombreSeances;

        public Reponse() {
        }

        public Reponse(int status, String message, int nombreSeances) {
            super(status, message);
            this.nombreSeances = nombreSeances;
        }

        public int getNombreSeances() { return nombreSeances; }
        public void setNombreSeances(int nombreSeances) { this.nombreSeances = nombreSeances; }
    }
}
