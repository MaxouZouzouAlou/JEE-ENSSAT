package fr.enssat.studiophoto.resource;

import fr.enssat.studiophoto.service.StatistiquesService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Statistiques (lot 5).
 *
 * <pre>
 * GET  /api/statistiques/photographes_par_seances       200 [[nom, nombre], …] (5 au plus)
 * GET  /api/statistiques/photographes_par_duree         200 [[nom, minutes], …] (5 au plus)
 * POST /api/statistiques/chiffre_affaires_pour_periode  200 montant TTC | 400 période invalide
 *      corps : { "dateDebut": "2026-01-01", "dateFin": "2026-12-31" }
 * </pre>
 */
@Path("/statistiques")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StatistiquesResource
{
    private final StatistiquesService service = new StatistiquesService();

    @GET
    @Path("photographes_par_seances")
    public List<Object[]> listerPhotographesParSeance() {
        return service.listerPhotographesParSeance();
    }

    @GET
    @Path("photographes_par_duree")
    public List<Object[]> listerPhotographesParDuree() {
        return service.listerPhotographesParDuree();
    }

    @POST
    @Path("chiffre_affaires_pour_periode")
    public BigDecimal calculerChiffreAffairesPourPeriode(PeriodeRequete requete) {
        // corps absent : les deux dates manquent, le service répond 400
        PeriodeRequete periode = requete != null ? requete : new PeriodeRequete();
        return service.calculerChiffreAffairesPourPeriode(periode.getDateDebut(), periode.getDateFin());
    }
}
