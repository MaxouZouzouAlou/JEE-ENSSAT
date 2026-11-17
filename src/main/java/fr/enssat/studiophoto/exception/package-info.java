/**
 * Exceptions métier et leur conversion en réponses HTTP.
 *
 * <p>Format commun des erreurs (P0.7) : {@code {status, erreur, message}}, voir {@link ErreurReponse}.
 * Correspondance : {@link DonneesInvalidesException} → 400, {@link RessourceIntrouvableException} → 404,
 * {@link ConflitException} → 409. {@link ExceptionMetierMapper} fait la conversion.</p>
 */
package fr.enssat.studiophoto.exception;
