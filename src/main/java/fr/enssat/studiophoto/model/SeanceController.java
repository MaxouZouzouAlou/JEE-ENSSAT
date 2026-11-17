package fr.enssat.studiophoto.model;

import fr.enssat.studiophoto.dao.SeanceDAO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Path("/")
public class SeanceController {
    private SeanceDAO seanceDAO = new SeanceDAO();

    @GET
    @Path("seance")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Seance> getSeances(@QueryParam("id") Long id,
            @QueryParam("photographeId") Long photographeId,
            @QueryParam("dateDebut") String dateDebutParam,
            @QueryParam("dateFin") String dateFinParam) {
        if ((dateDebutParam == null) != (dateFinParam == null)) {
            throw new BadRequestException("Les paramètres dateDebut et dateFin doivent être fournis ensemble.");
        }

        Date dateDebut = null;
        Date dateFin = null;
        try {
            if (dateDebutParam != null) {
                dateDebut = Date.valueOf(dateDebutParam);
                dateFin = Date.valueOf(dateFinParam);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Les dates doivent être au format YYYY-MM-DD.", e);
        }

        return seanceDAO.findByFilters(id, photographeId, dateDebut, dateFin);
    }

    @GET
    @Path("seances/options")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getOptionsCreation() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (TypeSeance type : seanceDAO.findTypes()) {
            types.add(Map.of("id", type.getId(), "libelle", type.getLibelle()));
        }

        List<Map<String, Object>> photographes = new ArrayList<>();
        for (Photographe photographe : seanceDAO.findPhotographesActifs()) {
            photographes.add(Map.of("id", photographe.getId(), "nom", photographe.getNom()));
        }

        List<Map<String, Object>> clients = new ArrayList<>();
        for (Client client : seanceDAO.findClients()) {
            clients.add(Map.of(
                    "id", client.getId(),
                    "type", client.getType(),
                    "designation", client.designation()));
        }

        return Map.of(
                "typesSeance", types,
                "photographes", photographes,
                "clients", clients);
    }

    @POST
    @Path("seances")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Seance createSeance(Map<String, String> request) {
        if (request == null) {
            throw new BadRequestException("Les données de la séance sont obligatoires.");
        }

        Date date;
        Time heureDebut;
        int duree;
        Long typeSeanceId;
        Long photographeId;
        Long clientId;
        TypeClient typeClient;
        try {
            date = Date.valueOf(champObligatoire(request, "date"));
            heureDebut = Time.valueOf(champObligatoire(request, "heureDebut"));
            duree = Integer.parseInt(champObligatoire(request, "duree"));
            typeSeanceId = Long.valueOf(champObligatoire(request, "typeSeanceId"));
            photographeId = Long.valueOf(champObligatoire(request, "photographeId"));
            clientId = Long.valueOf(champObligatoire(request, "clientId"));
            typeClient = TypeClient.valueOf(champObligatoire(request, "clientType").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Les champs de la séance sont invalides.", e);
        }
        String lieu = champObligatoire(request, "lieu");
        if (duree <= 0 || typeSeanceId <= 0 || photographeId <= 0 || clientId <= 0) {
            throw new BadRequestException("La durée et les identifiants doivent être positifs.");
        }

        Client client = typeClient == TypeClient.ENTREPRISE
                ? new ClientEntreprise(clientId, null, null, null, null)
                : new ClientParticulier(clientId, null, null, null, null);
        Seance seance = seanceDAO.create(
                date,
                heureDebut,
                duree,
                lieu,
                new TypeSeance(typeSeanceId, null, null),
                new Photographe(photographeId, null, null, true),
                client);
        if (seance == null) {
            throw new WebApplicationException(
                    "La séance n'a pas pu être créée. Vérifiez le photographe et les références sélectionnées.",
                    Response.Status.CONFLICT);
        }
        return seance;
    }

    @PUT
    @Path("seances/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Seance updateSeance(@PathParam("id") long id, Map<String, String> request) {
        if (request == null) {
            throw new BadRequestException("Les données de la séance sont obligatoires.");
        }

        Seance seance = trouverSeance(id);
        Date date;
        Time heureDebut;
        int duree;
        Long typeSeanceId;
        Long photographeId;
        Long clientId;
        StatutSeance statut;
        try {
            date = Date.valueOf(champObligatoire(request, "date"));
            heureDebut = Time.valueOf(champObligatoire(request, "heureDebut"));
            duree = Integer.parseInt(champObligatoire(request, "duree"));
            typeSeanceId = Long.valueOf(champObligatoire(request, "typeSeanceId"));
            photographeId = Long.valueOf(champObligatoire(request, "photographeId"));
            clientId = Long.valueOf(champObligatoire(request, "clientId"));
            statut = StatutSeance.valueOf(champObligatoire(request, "statut").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Les champs de la séance sont invalides.", e);
        }
        String lieu = champObligatoire(request, "lieu");
        String clientType = champObligatoire(request, "clientType");
        if (duree <= 0 || typeSeanceId <= 0 || photographeId <= 0 || clientId <= 0) {
            throw new BadRequestException("La durée et les identifiants doivent être positifs.");
        }

        if (statut != seance.getStatut()
                && !seance.getStatut().peutPasserA(statut, seance.calculerFin())) {
            throw new BadRequestException("Cette transition de statut n'est pas autorisée.");
        }

        TypeClient typeClient;
        try {
            typeClient = TypeClient.valueOf(clientType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Le type de client fourni est invalide.", e);
        }
        Client client = typeClient == TypeClient.ENTREPRISE
                ? new ClientEntreprise(clientId, null, null, null, null)
                : new ClientParticulier(clientId, null, null, null, null);

        Seance miseAJour = seanceDAO.update(
                id,
                date,
                heureDebut,
                duree,
                lieu,
                statut,
                new TypeSeance(typeSeanceId, null, null),
                new Photographe(photographeId, null, null, true),
                client);
        if (miseAJour == null) {
            throw new WebApplicationException(
                    "La séance n'a pas pu être mise à jour.", Response.Status.CONFLICT);
        }
        return trouverSeance(id);
    }

    @PATCH
    @Path("seances/{id}/statut")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStatut(@PathParam("id") long id, Map<String, String> request) {
        if (request == null || request.get("statut") == null) {
            throw new BadRequestException("Le champ statut est obligatoire.");
        }

        StatutSeance nouveauStatut;
        try {
            nouveauStatut = StatutSeance.valueOf(request.get("statut").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Le statut fourni est invalide.", e);
        }

        Seance seance = trouverSeance(id);
        if (!seance.getStatut().peutPasserA(nouveauStatut, seance.calculerFin())) {
            throw new BadRequestException("Cette transition de statut n'est pas autorisée.");
        }

        Seance seanceMiseAJour = seanceDAO.update(
                id,
                Date.valueOf(seance.getDate()),
                Time.valueOf(seance.getHeureDebut()),
                seance.getDuree(),
                seance.getLieu(),
                nouveauStatut,
                seance.getTypeSeance(),
                seance.getPhotographe(),
                seance.getClient());
        if (seanceMiseAJour == null) {
            throw new WebApplicationException(
                    "La séance n'a pas pu être mise à jour.", Response.Status.CONFLICT);
        }

        seance.setStatut(nouveauStatut);
        return Response.ok(seance).build();
    }

    private Seance trouverSeance(long id) {
        List<Seance> seances = seanceDAO.findByFilters(id, null, null, null);
        if (seances.isEmpty()) {
            throw new NotFoundException("Aucune séance trouvée pour l'identifiant " + id + ".");
        }
        return seances.get(0);
    }

    private String champObligatoire(Map<String, String> valeurs, String nom) {
        String valeur = valeurs.get(nom);
        if (valeur == null || valeur.isBlank()) {
            throw new BadRequestException("Le champ " + nom + " est obligatoire.");
        }
        return valeur;
    }
}
