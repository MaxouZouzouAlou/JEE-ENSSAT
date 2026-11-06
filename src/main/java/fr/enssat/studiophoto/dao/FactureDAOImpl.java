package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.DBManager;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.DaoException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.ClientParticulier;
import fr.enssat.studiophoto.model.Facture;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.model.Seance;
import fr.enssat.studiophoto.model.StatutPaiement;
import fr.enssat.studiophoto.model.StatutSeance;
import fr.enssat.studiophoto.model.TypeClient;
import fr.enssat.studiophoto.model.TypeSeance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** implémentation JDBC de FactureDAO */
public class FactureDAOImpl implements FactureDAO {

    private static final String COLONNES =
            "id, numero, date_emission, montant_ht, taux_tva, montant_ttc, statut_paiement, seance_id,"
                    + " client_designation, client_adresse, client_email, client_siren_tva,"
                    + " prestation_date, prestation_heure, prestation_duree_minutes, prestation_type,"
                    + " photographe_nom";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLONNES + " FROM facture ORDER BY date_emission DESC, id DESC";
    private static final String SQL_FIND_BY_ID = "SELECT " + COLONNES + " FROM facture WHERE id = ?";
    private static final String SQL_FIND_BY_SEANCE = "SELECT " + COLONNES + " FROM facture WHERE seance_id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO facture (numero, date_emission, montant_ht, taux_tva, montant_ttc, statut_paiement,"
                    + " seance_id, client_designation, client_adresse, client_email, client_siren_tva,"
                    + " prestation_date, prestation_heure, prestation_duree_minutes, prestation_type,"
                    + " photographe_nom) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // le AND statut_paiement = ? évite d'écraser un changement fait entre temps
    // par une autre requete (si le statut a bougé, aucune ligne n'est modifiée)
    private static final String SQL_UPDATE_STATUT =
            "UPDATE facture SET statut_paiement = ? WHERE id = ? AND statut_paiement = ?";
    // numéros sur 4 chiffres avec des 0 devant : le MAX alphabétique = le MAX numérique
    private static final String SQL_DERNIER_NUMERO = "SELECT MAX(numero) FROM facture WHERE numero LIKE ?";
    private static final String SQL_FIND_SEANCE =
            "SELECT s.id, s.date_seance, s.heure_debut, s.duree_minutes, s.lieu, s.statut,"
                    + " t.id AS type_id, t.libelle, t.tarif_horaire_ht,"
                    + " p.id AS photographe_id, p.nom AS photographe_nom, p.email AS photographe_email,"
                    + " p.actif,"
                    + " c.id AS client_id, c.type AS client_type, c.adresse_postale, c.email AS client_email,"
                    + " c.nom, c.prenom, c.raison_sociale, c.siren_tva"
                    + " FROM seance s"
                    + " JOIN type_seance t ON t.id = s.type_seance_id"
                    + " JOIN photographe p ON p.id = s.photographe_id"
                    + " JOIN client c ON c.id = s.client_id"
                    + " WHERE s.id = ?";

    // code MySQL "duplicate entry" (violation d'une contrainte UNIQUE)
    private static final int MYSQL_ENTREE_DUPLIQUEE = 1062;

    @Override
    public List<Facture> findAll() {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<Facture> factures = new ArrayList<>();
            while (rs.next()) {
                factures.add(versFacture(rs));
            }
            return factures;
        } catch (SQLException e) {
            throw new DaoException("Lecture des factures impossible", e);
        }
    }

    @Override
    public Optional<Facture> findById(long id) {
        return lireUne(SQL_FIND_BY_ID, id, "Lecture de la facture " + id + " impossible");
    }

    @Override
    public Optional<Facture> findBySeanceId(long seanceId) {
        return lireUne(SQL_FIND_BY_SEANCE, seanceId,
                "Lecture de la facture de la séance " + seanceId + " impossible");
    }

    @Override
    public Facture create(Facture f) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getNumero());
            ps.setObject(2, f.getDateEmission());
            ps.setBigDecimal(3, f.getMontantHT());
            ps.setBigDecimal(4, f.getTauxTVA());
            ps.setBigDecimal(5, f.getMontantTTC());
            ps.setString(6, f.getStatutPaiement().name());
            ps.setLong(7, f.getSeanceId());
            ps.setString(8, f.getClientDesignation());
            ps.setString(9, f.getClientAdresse());
            ps.setString(10, f.getClientEmail());
            ps.setString(11, f.getClientSirenTva());
            ps.setObject(12, f.getPrestationDate());
            ps.setObject(13, f.getPrestationHeure());
            ps.setInt(14, f.getPrestationDuree());
            ps.setString(15, f.getPrestationType());
            ps.setString(16, f.getPhotographeNom());
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                if (cles.next()) {
                    f.setId(cles.getLong(1));
                }
            }
            return f;
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == MYSQL_ENTREE_DUPLIQUEE) {
                throw new ConflitException("Le numéro de facture " + f.getNumero()
                        + " ou la séance " + f.getSeanceId() + " est déjà facturé");
            }
            throw new DaoException("Création de la facture impossible", e);
        } catch (SQLException e) {
            throw new DaoException("Création de la facture impossible", e);
        }
    }

    @Override
    public boolean updateStatut(long id, StatutPaiement attendu, StatutPaiement nouveau) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_UPDATE_STATUT)) {
            ps.setString(1, nouveau.name());
            ps.setLong(2, id);
            ps.setString(3, attendu.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Modification du statut de la facture " + id + " impossible", e);
        }
    }

    @Override
    public Optional<String> findDernierNumero(int annee) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_DERNIER_NUMERO)) {
            ps.setString(1, "FAC-" + annee + "-%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return Optional.ofNullable(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new DaoException("Lecture du dernier numéro de facture " + annee + " impossible", e);
        }
    }

    @Override
    public Optional<Seance> findSeance(long seanceId) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_FIND_SEANCE)) {
            ps.setLong(1, seanceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(versSeance(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Lecture de la séance " + seanceId + " impossible", e);
        }
    }

    private Optional<Facture> lireUne(String sql, long parametre, String messageErreur) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, parametre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(versFacture(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException(messageErreur, e);
        }
    }

    private Facture versFacture(ResultSet rs) throws SQLException {
        Facture f = new Facture();
        f.setId(rs.getLong("id"));
        f.setNumero(rs.getString("numero"));
        f.setDateEmission(rs.getObject("date_emission", java.time.LocalDate.class));
        f.setMontantHT(rs.getBigDecimal("montant_ht"));
        f.setTauxTVA(rs.getBigDecimal("taux_tva"));
        f.setMontantTTC(rs.getBigDecimal("montant_ttc"));
        f.setStatutPaiement(StatutPaiement.valueOf(rs.getString("statut_paiement")));
        f.setSeanceId(rs.getLong("seance_id"));
        f.setClientDesignation(rs.getString("client_designation"));
        f.setClientAdresse(rs.getString("client_adresse"));
        f.setClientEmail(rs.getString("client_email"));
        f.setClientSirenTva(rs.getString("client_siren_tva"));
        f.setPrestationDate(rs.getObject("prestation_date", java.time.LocalDate.class));
        Time heure = rs.getTime("prestation_heure");
        f.setPrestationHeure(heure.toLocalTime());
        f.setPrestationDuree(rs.getInt("prestation_duree_minutes"));
        f.setPrestationType(rs.getString("prestation_type"));
        f.setPhotographeNom(rs.getString("photographe_nom"));
        return f;
    }

    private Seance versSeance(ResultSet rs) throws SQLException {
        Seance s = new Seance();
        s.setId(rs.getLong("id"));
        s.setDate(rs.getObject("date_seance", java.time.LocalDate.class));
        s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        s.setDuree(rs.getInt("duree_minutes"));
        s.setLieu(rs.getString("lieu"));
        s.setStatut(StatutSeance.valueOf(rs.getString("statut")));
        s.setTypeSeance(new TypeSeance(rs.getLong("type_id"), rs.getString("libelle"),
                rs.getBigDecimal("tarif_horaire_ht")));
        s.setPhotographe(new Photographe(rs.getLong("photographe_id"), rs.getString("photographe_nom"),
                rs.getString("photographe_email"), rs.getBoolean("actif")));
        s.setClient(versClient(rs));
        return s;
    }

    // colonnes client de la jointure -> bonne sous-classe selon client.type
    private Client versClient(ResultSet rs) throws SQLException {
        long id = rs.getLong("client_id");
        String adresse = rs.getString("adresse_postale");
        String email = rs.getString("client_email");
        return switch (TypeClient.valueOf(rs.getString("client_type"))) {
            case PARTICULIER -> new ClientParticulier(id, adresse, email,
                    rs.getString("nom"), rs.getString("prenom"));
            case ENTREPRISE -> new ClientEntreprise(id, adresse, email,
                    rs.getString("raison_sociale"), rs.getString("siren_tva"));
        };
    }

    private Connection connexion() {
        Connection cnx = DBManager.getInstance().getConnection();
        if (cnx == null) {
            throw new DaoException("Connexion à la base de données impossible");
        }
        return cnx;
    }
}
