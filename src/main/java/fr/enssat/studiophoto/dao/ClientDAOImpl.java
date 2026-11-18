package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.DBManager;
import fr.enssat.studiophoto.exception.DaoException;
import fr.enssat.studiophoto.model.Client;
import fr.enssat.studiophoto.model.ClientEntreprise;
import fr.enssat.studiophoto.model.ClientParticulier;
import fr.enssat.studiophoto.model.TypeClient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC de {@link ClientDAO}.
 *
 * <p>Héritage en table unique (équivalent de la stratégie JPA {@code SINGLE_TABLE}) :
 * les deux sous-classes partagent la table {@code client}. La colonne {@code type}
 * sert de discriminant pour choisir la classe à instancier, et les colonnes propres
 * à l'autre sous-classe restent à {@code NULL}.</p>
 */
public class ClientDAOImpl implements ClientDAO {

    private static final String COLONNES =
            "id, type, adresse_postale, email, nom, prenom, raison_sociale, siren_tva";

    private static final String SQL_FIND_ALL = "SELECT " + COLONNES + " FROM client ORDER BY id";
    private static final String SQL_FIND_BY_ID = "SELECT " + COLONNES + " FROM client WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO client (type, adresse_postale, email, nom, prenom, raison_sociale, siren_tva)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE client SET type = ?, adresse_postale = ?, email = ?, nom = ?, prenom = ?,"
                    + " raison_sociale = ?, siren_tva = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM client WHERE id = ?";
    private static final String SQL_COUNT_SEANCES = "SELECT COUNT(*) FROM seance WHERE client_id = ?";

    @Override
    public List<Client> findAll() {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<Client> clients = new ArrayList<>();
            while (rs.next()) {
                clients.add(versClient(rs));
            }
            return clients;
        } catch (SQLException e) {
            throw new DaoException("Lecture des clients impossible", e);
        }
    }

    @Override
    public Optional<Client> findById(long id) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(versClient(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Lecture du client " + id + " impossible", e);
        }
    }

    @Override
    public Client create(Client client) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            remplirColonnes(ps, client);
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                if (cles.next()) {
                    client.setId(cles.getLong(1));
                }
            }
            return client;
        } catch (SQLException e) {
            throw new DaoException("Création du client impossible", e);
        }
    }

    @Override
    public boolean update(Client client) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_UPDATE)) {
            remplirColonnes(ps, client);
            ps.setLong(8, client.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Modification du client " + client.getId() + " impossible", e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_DELETE)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Suppression du client " + id + " impossible", e);
        }
    }

    @Override
    public int countSeances(long clientId) {
        try (Connection cnx = connexion();
             PreparedStatement ps = cnx.prepareStatement(SQL_COUNT_SEANCES)) {
            ps.setLong(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Comptage des séances du client " + clientId + " impossible", e);
        }
    }

    /** Ligne de la table -> sous-classe choisie d'après la colonne discriminante {@code type}. */
    private Client versClient(ResultSet rs) throws SQLException {
        TypeClient type = TypeClient.valueOf(rs.getString("type"));
        long id = rs.getLong("id");
        String adresse = rs.getString("adresse_postale");
        String email = rs.getString("email");
        return switch (type) {
            case PARTICULIER -> new ClientParticulier(id, adresse, email,
                    rs.getString("nom"), rs.getString("prenom"));
            case ENTREPRISE -> new ClientEntreprise(id, adresse, email,
                    rs.getString("raison_sociale"), rs.getString("siren_tva"));
        };
    }

    /**
     * Remplit les paramètres 1 à 7 (type, adresse, email, nom, prénom, raison sociale, SIREN/TVA).
     * Les colonnes qui ne concernent pas la sous-classe sont mises à {@code NULL}.
     */
    private void remplirColonnes(PreparedStatement ps, Client client) throws SQLException {
        ps.setString(1, client.getType().name());
        ps.setString(2, client.getAdressePostale());
        ps.setString(3, client.getEmail());
        switch (client) {
            case ClientParticulier p -> {
                ps.setString(4, p.getNom());
                ps.setString(5, p.getPrenom());
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.VARCHAR);
            }
            case ClientEntreprise e -> {
                ps.setNull(4, Types.VARCHAR);
                ps.setNull(5, Types.VARCHAR);
                ps.setString(6, e.getRaisonSociale());
                ps.setString(7, e.getSirenTva());
            }
            default -> throw new IllegalArgumentException("Type de client inconnu : " + client.getClass());
        }
    }

    private Connection connexion() {
        Connection cnx = DBManager.getInstance().getConnection();
        if (cnx == null) {
            throw new DaoException("Connexion à la base de données impossible");
        }
        return cnx;
    }
}
