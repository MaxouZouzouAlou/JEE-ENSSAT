package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.model.Facture;
import fr.enssat.studiophoto.service.FactureService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

/**
 * POST /api/seances/{id}/facture : génère la facture d'une séance.
 * Classe séparé de FactureResource car la route est sous /seances.
 */
@Path("/seances/{id}/facture")
@Produces(MediaType.APPLICATION_JSON)
public class SeanceFactureResource {

    private final FactureService factureService;

    public SeanceFactureResource() {
        this(new FactureService());
    }

    public SeanceFactureResource(FactureService factureService) {
        this.factureService = factureService;
    }

    @POST
    public Response generer(@PathParam("id") long seanceId, @Context UriInfo uriInfo) {
        Facture facture = factureService.generer(seanceId);
        URI location = uriInfo.getBaseUriBuilder().path("factures").path(String.valueOf(facture.getId())).build();
        return Response.created(location).entity(facture).build();
    }
}
