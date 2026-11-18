package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.dto.ChangementStatutPhotographe;
import fr.enssat.studiophoto.dto.ResultatChangementStatut;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.service.PhotographeService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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

/**
 * Photographes (lot 1). Contrat détaillé dans {@code openapi.yaml}.
 *
 * <pre>
 * GET    /api/photographes[?actif=true|false]          200 liste
 * GET    /api/photographes/{id}                        200 | 404
 * POST   /api/photographes                             201 | 400 | 409 (email déjà utilisé)
 * PUT    /api/photographes/{id}                        200 | 400 | 404 | 409
 * PATCH  /api/photographes/{id}/statut[?confirmer=true] 200 | 400 | 404 | 409 (séances à venir, avec nombreSeances)
 * DELETE /api/photographes/{id}                        204 | 404 | 409 (photographe avec séances)
 * </pre>
 */
@Path("/photographes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PhotographeResource {

    private final PhotographeService service = new PhotographeService();

    @GET
    public List<Photographe> lister(@QueryParam("actif") Boolean actif) {
        return service.lister(actif);
    }

    @GET
    @Path("{id}")
    public Photographe trouver(@PathParam("id") long id) {
        return service.trouver(id);
    }

    @POST
    public Response creer(Photographe donnees, @Context UriInfo uriInfo) {
        Photographe cree = service.creer(donnees);
        URI emplacement = uriInfo.getAbsolutePathBuilder().path(String.valueOf(cree.getId())).build();
        return Response.created(emplacement).entity(cree).build();
    }

    @PUT
    @Path("{id}")
    public Photographe modifier(@PathParam("id") long id, Photographe donnees) {
        return service.modifier(id, donnees);
    }

    @PATCH
    @Path("{id}/statut")
    public ResultatChangementStatut changerStatut(@PathParam("id") long id,
                                                  @QueryParam("confirmer") @DefaultValue("false") boolean confirmer,
                                                  ChangementStatutPhotographe corps) {
        return service.changerStatut(id, corps != null ? corps.getActif() : null, confirmer);
    }

    @DELETE
    @Path("{id}")
    public Response supprimer(@PathParam("id") long id) {
        service.supprimer(id);
        return Response.noContent().build();
    }
}
