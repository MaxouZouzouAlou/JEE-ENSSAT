package fr.enssat.studiophoto.exception;

/**
 * Exception métier : refus d'une règle de gestion, à renvoyer au client HTTP.
 *
 * <p>Chaque sous-classe correspond à un code HTTP. {@link ExceptionMetierMapper}
 * la convertit en réponse JSON au format commun {@link ErreurReponse}. Les services
 * lèvent ces exceptions, les ressources REST ne les attrapent pas.</p>
 *
 * <table>
 *   <caption>Correspondance exception métier → code HTTP</caption>
 *   <tr><th>Exception</th><th>Code</th><th>Cas</th></tr>
 *   <tr><td>{@link DonneesInvalidesException}</td><td>400</td><td>champ manquant ou mal formé, période incohérente</td></tr>
 *   <tr><td>{@link RessourceIntrouvableException}</td><td>404</td><td>identifiant inconnu</td></tr>
 *   <tr><td>{@link ConflitException}</td><td>409</td><td>action impossible dans l'état actuel
 *       (doublon, transition interdite, suppression refusée...)</td></tr>
 * </table>
 */
public abstract class ExceptionMetier extends RuntimeException {

    protected ExceptionMetier(String message) {
        super(message);
    }

    /** Code HTTP de la réponse. */
    public abstract int getStatus();

    /**
     * Corps JSON de la réponse. Une sous-classe peut le redéfinir pour ajouter
     * des informations (voir {@link ConfirmationRequiseException}).
     */
    public ErreurReponse versReponse() {
        return new ErreurReponse(getStatus(), getMessage());
    }
}
