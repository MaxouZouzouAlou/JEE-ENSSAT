package fr.enssat.studiophoto.dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import fr.enssat.studiophoto.DBManager;
import fr.enssat.studiophoto.model.*;

public class SeanceDAO {
    public List<Seance> findByQuery(String query) {
        List<Seance> result = new ArrayList<>();
        Connection connection = DBManager.getInstance().getConnection();

        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            while (rs.next()) {
                long id = rs.getLong("id");
                Date date_seance = rs.getDate("date_seance");
                Time heure_seance = rs.getTime("heure_debut");
                int duree_minutes = rs.getInt("duree_minutes");
                String lieu = rs.getString("lieu");
                String statut = rs.getString("statut");
                int type_seance_id = rs.getInt("type_seance_id");
                int photographe_id = rs.getInt("photographe_id");
                int client_id = rs.getInt("client_id");

                Seance seance = new Seance(id, date_seance, heure_seance, duree_minutes, lieu, StatutSeance.valueOf(statut.toUpperCase()));
                result.add(seance);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            DBManager.getInstance().cleanup(connection, statement, rs);
        }

        return result;
    }

    public List<Seance> findByAll() {
        return findByQuery("select * from seance");
    }

    public List<TypeSeance> findTypes() {
        List<TypeSeance> result = new ArrayList<>();
        Connection connection = DBManager.getInstance().getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement(
                    "SELECT id, libelle, tarif_horaire_ht FROM type_seance ORDER BY libelle");
            rs = statement.executeQuery();
            while (rs.next()) {
                result.add(new TypeSeance(
                        rs.getLong("id"),
                        rs.getString("libelle"),
                        rs.getBigDecimal("tarif_horaire_ht")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.getInstance().cleanup(connection, statement, rs);
        }
        return result;
    }

    public List<Photographe> findPhotographesActifs() {
        List<Photographe> result = new ArrayList<>();
        Connection connection = DBManager.getInstance().getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement(
                    "SELECT id, nom, email, actif FROM photographe WHERE actif = TRUE ORDER BY nom");
            rs = statement.executeQuery();
            while (rs.next()) {
                result.add(new Photographe(
                        rs.getLong("id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getBoolean("actif")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.getInstance().cleanup(connection, statement, rs);
        }
        return result;
    }

    public List<Client> findClients() {
        List<Client> result = new ArrayList<>();
        Connection connection = DBManager.getInstance().getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement(
                    "SELECT id, type, adresse_postale, email, nom, prenom, raison_sociale, siren_tva "
                            + "FROM client ORDER BY type, nom, prenom, raison_sociale");
            rs = statement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("id");
                String adresse = rs.getString("adresse_postale");
                String email = rs.getString("email");
                if ("ENTREPRISE".equals(rs.getString("type"))) {
                    result.add(new ClientEntreprise(
                            id, adresse, email, rs.getString("raison_sociale"), rs.getString("siren_tva")));
                } else {
                    result.add(new ClientParticulier(
                            id, adresse, email, rs.getString("nom"), rs.getString("prenom")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.getInstance().cleanup(connection, statement, rs);
        }
        return result;
    }

    public List<Seance> findByFilters(Long id, Long photographeId, Date dateDebut, Date dateFin) {
        StringBuilder query = new StringBuilder(
                "SELECT s.*, ts.libelle AS type_libelle, ts.tarif_horaire_ht AS type_tarif, "
                        + "p.nom AS photographe_nom, p.email AS photographe_email, p.actif AS photographe_actif, "
                        + "c.type AS client_type, c.adresse_postale AS client_adresse, c.email AS client_email, "
                        + "c.nom AS client_nom, c.prenom AS client_prenom, "
                        + "c.raison_sociale AS client_raison_sociale, c.siren_tva AS client_siren_tva "
                        + "FROM seance s "
                        + "JOIN type_seance ts ON ts.id = s.type_seance_id "
                        + "JOIN photographe p ON p.id = s.photographe_id "
                        + "JOIN client c ON c.id = s.client_id WHERE 1 = 1");
        if (id != null) {
            query.append(" AND s.id = ?");
        }
        if (photographeId != null) {
            query.append(" AND s.photographe_id = ?");
        }
        if (dateDebut != null) {
            query.append(" AND s.date_seance >= ? AND s.date_seance <= ?");
        }
        query.append(" ORDER BY s.date_seance, s.heure_debut");

        List<Seance> result = new ArrayList<>();
        Connection connection = DBManager.getInstance().getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement(query.toString());
            int parameterIndex = 1;
            if (id != null) {
                statement.setLong(parameterIndex++, id);
            }
            if (photographeId != null) {
                statement.setLong(parameterIndex++, photographeId);
            }
            if (dateDebut != null) {
                statement.setDate(parameterIndex++, dateDebut);
                statement.setDate(parameterIndex, dateFin);
            }

            rs = statement.executeQuery();
            while (rs.next()) {
                Seance seance = new Seance(
                        rs.getLong("id"),
                        rs.getDate("date_seance"),
                        rs.getTime("heure_debut"),
                        rs.getInt("duree_minutes"),
                        rs.getString("lieu"),
                        StatutSeance.valueOf(rs.getString("statut").toUpperCase()));
                seance.setTypeSeance(new TypeSeance(
                        rs.getLong("type_seance_id"),
                        rs.getString("type_libelle"),
                        rs.getBigDecimal("type_tarif")));
                seance.setPhotographe(new Photographe(
                        rs.getLong("photographe_id"),
                        rs.getString("photographe_nom"),
                        rs.getString("photographe_email"),
                        rs.getBoolean("photographe_actif")));
                if ("ENTREPRISE".equals(rs.getString("client_type"))) {
                    seance.setClient(new ClientEntreprise(
                            rs.getLong("client_id"),
                            rs.getString("client_adresse"),
                            rs.getString("client_email"),
                            rs.getString("client_raison_sociale"),
                            rs.getString("client_siren_tva")));
                } else {
                    seance.setClient(new ClientParticulier(
                            rs.getLong("client_id"),
                            rs.getString("client_adresse"),
                            rs.getString("client_email"),
                            rs.getString("client_nom"),
                            rs.getString("client_prenom")));
                }
                result.add(seance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.getInstance().cleanup(connection, statement, rs);
        }
        return result;
    }

    public Seance create(Date date_seance, Time heure_seance, int duree_minute, String lieu, TypeSeance type_seance, Photographe photographe, Client client) {
        if (!photographe.isActif()) {
            return null;
        }

        Connection connection = DBManager.getInstance().getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement(
                    "INSERT INTO seance (date_seance, heure_debut, duree_minutes, lieu, statut, type_seance_id, photographe_id, client_id) "
                            + "SELECT ?, ?, ?, ?, 'PLANIFIEE', ?, p.id, ? "
                            + "FROM photographe p WHERE p.id = ? AND p.actif = TRUE",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setDate(1, date_seance);
            statement.setTime(2, heure_seance);
            statement.setInt(3, duree_minute);
            statement.setString(4, lieu);
            statement.setLong(5, type_seance.getId());
            statement.setLong(6, client.getId());
            statement.setLong(7, photographe.getId());

            if (statement.executeUpdate() == 0) {
                return null;
            }

            rs = statement.getGeneratedKeys();
            if (rs.next()) {
                return new Seance(rs.getLong(1), date_seance, heure_seance, duree_minute, lieu, StatutSeance.PLANIFIEE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.getInstance().cleanup(connection, statement, rs);
        }
        return null;
    }

    public Seance update(long id, Date date_seance, Time heure_seance, int duree_minute, String lieu,
            StatutSeance statut, TypeSeance type_seance, Photographe photographe, Client client) {
        Connection connection = DBManager.getInstance().getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                "UPDATE seance SET date_seance = ?, heure_debut = ?, duree_minutes = ?, lieu = ?, statut = ?, "
                    + "type_seance_id = ?, photographe_id = ?, client_id = ? "
                            + "WHERE id = ? AND statut NOT IN (?, ?)");
            statement.setDate(1, date_seance);
            statement.setTime(2, heure_seance);
            statement.setInt(3, duree_minute);
            statement.setString(4, lieu);
            statement.setString(5, statut.name());
            statement.setLong(6, type_seance.getId());
            statement.setLong(7, photographe.getId());
            statement.setLong(8, client.getId());
            statement.setLong(9, id);
            statement.setString(10, StatutSeance.TERMINEE.name());
            statement.setString(11, StatutSeance.ANNULEE.name());

            if (statement.executeUpdate() > 0) {
                return new Seance(id, date_seance, heure_seance, duree_minute, lieu, statut);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.getInstance().cleanup(connection, statement, null);
        }
        return null;
    }

    public boolean delete(long id) {
        Connection connection = DBManager.getInstance().getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "DELETE FROM seance WHERE id = ? "
                            + "AND NOT EXISTS (SELECT 1 FROM facture WHERE facture.seance_id = seance.id)");
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBManager.getInstance().cleanup(connection, statement, null);
        }
    }
}
