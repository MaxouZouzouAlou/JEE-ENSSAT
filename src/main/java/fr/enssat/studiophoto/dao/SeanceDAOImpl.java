package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.DBManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Implémentation JDBC de {@link SeanceDAO}. */
public class SeanceDAOImpl implements SeanceDAO {

    /** Condition « séance à venir » : statut actif et début (date + heure) après l'instant donné. */
    private static final String A_VENIR =
            " AND statut IN ('PLANIFIEE', 'CONFIRMEE') AND TIMESTAMP(date_seance, heure_debut) > ?";

    @Override
    public int compterParPhotographe(long photographeId) {
        String sql = "SELECT COUNT(*) FROM seance WHERE photographe_id = ?";
        try (Connection c = connexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, photographeId);
            return lireNombre(ps);
        } catch (SQLException e) {
            throw new DAOException("Impossible de compter les séances du photographe " + photographeId, e);
        }
    }

    @Override
    public int compterAVenirParPhotographe(long photographeId, LocalDateTime maintenant) {
        String sql = "SELECT COUNT(*) FROM seance WHERE photographe_id = ?" + A_VENIR;
        try (Connection c = connexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, photographeId);
            ps.setObject(2, maintenant);
            return lireNombre(ps);
        } catch (SQLException e) {
            throw new DAOException("Impossible de compter les séances à venir du photographe " + photographeId, e);
        }
    }

    @Override
    public int annulerAVenirParPhotographe(long photographeId, LocalDateTime maintenant) {
        String sql = "UPDATE seance SET statut = 'ANNULEE' WHERE photographe_id = ?" + A_VENIR;
        try (Connection c = connexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, photographeId);
            ps.setObject(2, maintenant);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Impossible d'annuler les séances à venir du photographe " + photographeId, e);
        }
    }

    private static int lireNombre(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static Connection connexion() {
        Connection c = DBManager.getInstance().getConnection();
        if (c == null) {
            throw new DAOException("Connexion à la base de données impossible.");
        }
        return c;
    }
}
