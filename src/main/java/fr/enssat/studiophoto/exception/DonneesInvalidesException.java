package fr.enssat.studiophoto.exception;

/** Données reçues invalides (champ obligatoire manquant, format incorrect...) : HTTP 400. */
public class DonneesInvalidesException extends ExceptionMetier {

    public DonneesInvalidesException(String message) {
        super(message);
    }

    @Override
    public int getStatus() {
        return 400;
    }
}
