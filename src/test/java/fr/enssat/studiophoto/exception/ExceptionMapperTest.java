package fr.enssat.studiophoto.exception;

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExceptionMapperTest {

    @Test
    void donneesInvalidesDonne400() {
        Response reponse = new MetierExceptionMapper().toResponse(new DonneesInvalidesException("Le nom est obligatoire."));

        assertEquals(400, reponse.getStatus());
        ErreurReponse corps = (ErreurReponse) reponse.getEntity();
        assertEquals(400, corps.getStatus());
        assertEquals("Bad Request", corps.getErreur());
        assertEquals("Le nom est obligatoire.", corps.getMessage());
    }

    @Test
    void ressourceIntrouvableDonne404AvecMessageStandard() {
        Response reponse = new MetierExceptionMapper().toResponse(new RessourceIntrouvableException("Client", 42));

        assertEquals(404, reponse.getStatus());
        assertEquals("Client 42 introuvable.", ((ErreurReponse) reponse.getEntity()).getMessage());
    }

    @Test
    void conflitDonne409() {
        Response reponse = new MetierExceptionMapper().toResponse(new ConflitException("Le client a des séances."));

        assertEquals(409, reponse.getStatus());
    }

    @Test
    void erreurBaseDonne500SansDetailTechnique() {
        Response reponse = new DaoExceptionMapper().toResponse(
                new DaoException("Lecture impossible", new java.sql.SQLException("Communications link failure")));

        assertEquals(500, reponse.getStatus());
        assertFalse(((ErreurReponse) reponse.getEntity()).getMessage().contains("Communications link failure"));
    }

    @Test
    void erreurJerseyGardeSonCode() {
        WebApplicationExceptionMapper mapper = new WebApplicationExceptionMapper();

        assertEquals(404, mapper.toResponse(new NotFoundException()).getStatus());
        assertEquals(405, mapper.toResponse(new NotAllowedException("GET")).getStatus());
    }
}
