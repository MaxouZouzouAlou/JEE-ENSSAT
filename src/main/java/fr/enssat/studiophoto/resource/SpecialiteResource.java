package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.model.Specialite;
import fr.enssat.studiophoto.service.PhotographeService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Liste fixe des spécialités (lot 1) : {@code GET /api/specialites} → 200. */
@Path("/specialites")
@Produces(MediaType.APPLICATION_JSON)
public class SpecialiteResource {

    private final PhotographeService service = new PhotographeService();

    @GET
    public List<Specialite> lister() {
        return service.listerSpecialites();
    }
}
