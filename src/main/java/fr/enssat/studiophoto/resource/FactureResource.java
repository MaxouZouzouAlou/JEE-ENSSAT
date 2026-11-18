package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.exception.DonneesInvalidesException;
import fr.enssat.studiophoto.model.Facture;
import fr.enssat.studiophoto.service.FacturePdf;
import fr.enssat.studiophoto.service.FactureService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Routes /api/factures : GET liste, GET /{id}, GET /{id}/pdf, PATCH /{id}/statut.
 * La génération d'une facture est dans SeanceFactureResource.
 */
@Path("/factures")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FactureResource {

    private final FactureService factureService;
    private final FacturePdf facturePdf;

    public FactureResource() {
        this(new FactureService(), new FacturePdf());
    }

    public FactureResource(FactureService factureService, FacturePdf facturePdf) {
        this.factureService = factureService;
        this.facturePdf = facturePdf;
    }

    @GET
    public List<Facture> lister() {
        return factureService.lister();
    }

    @GET
    @Path("/{id}")
    public Facture trouver(@PathParam("id") long id) {
        return factureService.trouver(id);
    }

    // téléchargement du PDF, le fichier porte le numéro de la facture
    @GET
    @Path("/{id}/pdf")
    @Produces("application/pdf")
    public Response pdf(@PathParam("id") long id) {
        Facture facture = factureService.trouver(id);
        return Response.ok(facturePdf.generer(facture))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + facture.getNumero() + ".pdf\"")
                .build();
    }

    @PATCH
    @Path("/{id}/statut")
    public Facture changerStatut(@PathParam("id") long id, StatutRequete requete) {
        if (requete == null) {
            throw new DonneesInvalidesException("Le corps de la requête est vide : le statut est attendu.");
        }
        return factureService.changerStatut(id, requete.getStatut());
    }
}
