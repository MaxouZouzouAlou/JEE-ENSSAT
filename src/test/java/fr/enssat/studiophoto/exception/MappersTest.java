package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** codes et corps JSON des mappers */
class MappersTest {

    static Stream<ApiException> exceptions() {
        return Stream.of(
                new RequeteInvalideException("statut absent"),
                new IntrouvableException("Facture", 42),
                new ConflitException("déjà facturée"));
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void uneExceptionDeLApiDonneSonCodeEtSonMessage(ApiException exception) {
        Response reponse = new ApiExceptionMapper().toResponse(exception);

        ErreurJson corps = assertInstanceOf(ErreurJson.class, reponse.getEntity());
        assertEquals(exception.getStatut().getStatusCode(), reponse.getStatus());
        assertEquals(reponse.getStatus(), corps.status());
        assertEquals(exception.getStatut().getReasonPhrase(), corps.erreur());
        assertEquals(exception.getMessage(), corps.message());
    }

    @Test
    void lesTroisExceptionsPortentLesCodes400_404_409() {
        assertEquals(400, new RequeteInvalideException("x").getStatut().getStatusCode());
        assertEquals(404, new IntrouvableException("x").getStatut().getStatusCode());
        assertEquals(409, new ConflitException("x").getStatut().getStatusCode());
    }

    @Test
    void introuvableNommeLElementEtSonIdentifiant() {
        assertEquals("Facture 42 introuvable.", new IntrouvableException("Facture", 42).getMessage());
    }

    @Test
    void uneErreurDeBaseDonne500SansDetailTechnique() {
        DaoException erreur = new DaoException("Lecture impossible", new RuntimeException("Access denied for user"));

        Response reponse = new DaoExceptionMapper().toResponse(erreur);

        ErreurJson corps = assertInstanceOf(ErreurJson.class, reponse.getEntity());
        assertEquals(500, reponse.getStatus());
        assertFalse(corps.message().contains("Access denied"));
        assertFalse(corps.message().contains("Lecture impossible"));
    }
}
