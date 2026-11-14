package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.service.ClientService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

/**
 * Ressource REST des clients.
 *
 * <table>
 *   <caption>Routes</caption>
 *   <tr><td>GET    /api/clients</td><td>200 liste</td></tr>
 *   <tr><td>GET    /api/clients/{id}</td><td>200, 404</td></tr>
 *   <tr><td>POST   /api/clients</td><td>201 + en-tête Location, 400</td></tr>
 *   <tr><td>PUT    /api/clients/{id}</td><td>200, 400, 404</td></tr>
 *   <tr><td>DELETE /api/clients/{id}</td><td>204, 404, 409 si le client a des séances</td></tr>
 * </table>
 *
 * <p>Les erreurs sont levées par le service et converties en JSON par les ExceptionMapper.</p>
 */
@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource {

    private final ClientService clientService;

    public ClientResource() {
        this(new ClientService());
    }

    public ClientResource(ClientService clientService) {
        this.clientService = clientService;
    }

    @GET
    public List<Client> lister() {
        return clientService.lister();
    }

    @GET
    @Path("/{id}")
    public Client trouver(@PathParam("id") long id) {
        return clientService.trouver(id);
    }

    @POST
    public Response creer(ClientRequete requete, @Context UriInfo uriInfo) {
        Client cree = clientService.creer(convertir(requete));
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(cree.getId())).build();
        return Response.created(location).entity(cree).build();
    }

    @PUT
    @Path("/{id}")
    public Client modifier(@PathParam("id") long id, ClientRequete requete) {
        return clientService.modifier(id, convertir(requete));
    }

    @DELETE
    @Path("/{id}")
    public Response supprimer(@PathParam("id") long id) {
        clientService.supprimer(id);
        return Response.noContent().build();
    }

    /** Corps absent -> 400 ; sinon conversion vers la bonne sous-classe selon le champ "type". */
    private static Client convertir(ClientRequete requete) {
        if (requete == null) {
            throw new DonneesInvalidesException("Le corps de la requête est vide : les données du client sont attendues.");
        }
        return requete.versClient();
    }
}
