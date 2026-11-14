package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.model.Facture;
import fr.enssat.studiophoto.service.FacturePdf;
import fr.enssat.studiophoto.service.FactureService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Routes /api/factures : pour l'instant seulement GET /{id}/pdf.
 */
@Path("/factures")
@Produces(MediaType.APPLICATION_JSON)
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
}
