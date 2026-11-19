package fr.enssat.studiophoto.model;

import fr.enssat.studiophoto.service.SeanceService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Ressource REST des séances (lot 3). Aucune règle métier ici : tout est dans {@link SeanceService},
 * dont les exceptions deviennent des réponses 400, 404 ou 409 au format commun.
 *
 * <pre>
 * GET    /api/seances[?id=&amp;photographeId=&amp;dateDebut=&amp;dateFin=]  200 | 400
 * GET    /api/seance[?…]                  même chose (ancienne route, gardée pour compatibilité)
 * GET    /api/seances/options             200 listes des formulaires
 * GET    /api/seances/{id}                200 | 404
 * POST   /api/seances                     201 | 400 | 409
 * PUT    /api/seances/{id}                200 | 400 | 404 | 409
 * PATCH  /api/seances/{id}/statut         200 | 400 | 404 | 409
 * DELETE /api/seances/{id}                204 | 404 | 409 (séance facturée)
 * </pre>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class SeanceController {

    private final SeanceService service = new SeanceService();

    @GET
    @Path("seances")
    public List<Seance> lister(@QueryParam("id") Long id,
                               @QueryParam("photographeId") Long photographeId,
                               @QueryParam("dateDebut") String dateDebut,
                               @QueryParam("dateFin") String dateFin) {
        return service.lister(id, photographeId, dateDebut, dateFin);
    }

    /** Ancienne route au singulier, conservée pour les pages et branches qui l'utilisent encore. */
    @GET
    @Path("seance")
    public List<Seance> listerAncienneRoute(@QueryParam("id") Long id,
                                            @QueryParam("photographeId") Long photographeId,
                                            @QueryParam("dateDebut") String dateDebut,
                                            @QueryParam("dateFin") String dateFin) {
        return service.lister(id, photographeId, dateDebut, dateFin);
    }

    @GET
    @Path("seances/options")
    public Map<String, Object> options() {
        return service.options();
    }

    @GET
    @Path("seances/{id: \\d+}")
    public Seance trouver(@PathParam("id") long id) {
        return service.trouver(id);
    }

    @POST
    @Path("seances")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response creer(Map<String, String> donnees, @Context UriInfo uriInfo) {
        Seance creee = service.creer(donnees);
        URI emplacement = uriInfo.getBaseUriBuilder().path("seances").path(String.valueOf(creee.getId())).build();
        return Response.created(emplacement).entity(creee).build();
    }

    @PUT
    @Path("seances/{id: \\d+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Seance modifier(@PathParam("id") long id, Map<String, String> donnees) {
        return service.modifier(id, donnees);
    }

    @PATCH
    @Path("seances/{id: \\d+}/statut")
    @Consumes(MediaType.APPLICATION_JSON)
    public Seance changerStatut(@PathParam("id") long id, Map<String, String> donnees) {
        return service.changerStatut(id, donnees != null ? donnees.get("statut") : null);
    }

    @DELETE
    @Path("seances/{id: \\d+}")
    public Response supprimer(@PathParam("id") long id) {
        service.supprimer(id);
        return Response.noContent().build();
    }
}
