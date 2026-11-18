package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.DBManager;
import fr.enssat.studiophoto.exception.ConflitException;
import fr.enssat.studiophoto.exception.DaoException;
import fr.enssat.studiophoto.model.Photographe;
import fr.enssat.studiophoto.model.Specialite;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implémentation JDBC de {@link PhotographeDAO}. */
public class PhotographeDAOImpl implements PhotographeDAO {

    /** Codes d'erreur MySQL : clé unique en double, ligne encore référencée par une clé étrangère. */
    private static final int MYSQL_DOUBLON = 1062;
    private static final int MYSQL_LIGNE_REFERENCEE = 1451;

    private static final String SELECT = "SELECT id, nom, email, actif FROM photographe";

    private static final String SELECT_SPECIALITES =
            "SELECT ps.photographe_id, s.id, s.libelle FROM photographe_specialite ps"
                    + " JOIN specialite s ON s.id = ps.specialite_id";

    @Override
    public List<Photographe> lister(Boolean actif) {
        String sql = SELECT + (actif != null ? " WHERE actif = ?" : "") + " ORDER BY nom, id";
        try (Connection c = connexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (actif != null) {
                ps.setBoolean(1, actif);
            }
            Map<Long, Photographe> parId = lirePhotographes(ps);
            chargerSpecialites(c, parId, null);
            return new ArrayList<>(parId.values());
        } catch (SQLException e) {
            throw new DaoException("Impossible de lire la liste des photographes.", e);
        }
    }

    @Override
    public Optional<Photographe> trouverParId(long id) {
        return trouverUn(SELECT + " WHERE id = ?", id);
    }

    @Override
    public Optional<Photographe> trouverParEmail(String email) {
        // la collation de la colonne (utf8mb4 par défaut) rend la comparaison insensible à la casse
        return trouverUn(SELECT + " WHERE email = ?", email);
    }

    @Override
    public Photographe creer(Photographe photographe) {
        String sql = "INSERT INTO photographe (nom, email, actif) VALUES (?, ?, ?)";
        try (Connection c = connexion()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, photographe.getNom());
                ps.setString(2, photographe.getEmail());
                ps.setBoolean(3, photographe.isActif());
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    photographe.setId(cles.getLong(1));
                }
                insererSpecialites(c, photographe);
                c.commit();
                return photographe;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw traduire(e, "Impossible de créer le photographe.");
        }
    }

    @Override
    public void modifier(Photographe photographe) {
        try (Connection c = connexion()) {
            c.setAutoCommit(false);
            try (PreparedStatement maj = c.prepareStatement("UPDATE photographe SET nom = ?, email = ? WHERE id = ?");
                 PreparedStatement effacer = c.prepareStatement("DELETE FROM photographe_specialite WHERE photographe_id = ?")) {
                maj.setString(1, photographe.getNom());
                maj.setString(2, photographe.getEmail());
                maj.setLong(3, photographe.getId());
                maj.executeUpdate();
                effacer.setLong(1, photographe.getId());
                effacer.executeUpdate();
                insererSpecialites(c, photographe);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw traduire(e, "Impossible de modifier le photographe " + photographe.getId() + ".");
        }
    }

    @Override
    public void changerActif(long id, boolean actif) {
        try (Connection c = connexion(); PreparedStatement ps = c.prepareStatement("UPDATE photographe SET actif = ? WHERE id = ?")) {
            ps.setBoolean(1, actif);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw traduire(e, "Impossible de changer le statut du photographe " + id + ".");
        }
    }

    @Override
    public void supprimer(long id) {
        try (Connection c = connexion(); PreparedStatement ps = c.prepareStatement("DELETE FROM photographe WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw traduire(e, "Impossible de supprimer le photographe " + id + ".");
        }
    }

    @Override
    public boolean isActif(Long photographeId, Date debut, Date fin) {
        // repris tel quel de la version du lot 3
        Connection connection = DBManager.getInstance().getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM seance WHERE photographe_id = ? AND date_seance BETWEEN ? AND ? AND statut <> ?");
            statement.setLong(1, photographeId);
            statement.setDate(2, debut);
            statement.setDate(3, fin);
            statement.setString(4, "ANNULEE");
            rs = statement.executeQuery();
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBManager.getInstance().cleanup(connection, statement, rs);
        }
    }

    // ------------------------------------------------------------------

    private Optional<Photographe> trouverUn(String sql, Object parametre) {
        try (Connection c = connexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, parametre);
            Map<Long, Photographe> parId = lirePhotographes(ps);
            if (parId.isEmpty()) {
                return Optional.empty();
            }
            Photographe p = parId.values().iterator().next();
            chargerSpecialites(c, parId, p.getId());
            return Optional.of(p);
        } catch (SQLException e) {
            throw new DaoException("Impossible de lire le photographe.", e);
        }
    }

    private static Map<Long, Photographe> lirePhotographes(PreparedStatement ps) throws SQLException {
        Map<Long, Photographe> parId = new LinkedHashMap<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Photographe p = new Photographe(rs.getLong("id"), rs.getString("nom"),
                        rs.getString("email"), rs.getBoolean("actif"));
                parId.put(p.getId(), p);
            }
        }
        return parId;
    }

    /** Remplit les spécialités des photographes donnés (d'un seul si {@code photographeId} est renseigné). */
    private static void chargerSpecialites(Connection c, Map<Long, Photographe> parId, Long photographeId)
            throws SQLException {
        if (parId.isEmpty()) {
            return;
        }
        String sql = SELECT_SPECIALITES + (photographeId != null ? " WHERE ps.photographe_id = ?" : "")
                + " ORDER BY s.libelle";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (photographeId != null) {
                ps.setLong(1, photographeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Photographe p = parId.get(rs.getLong("photographe_id"));
                    if (p != null) {
                        p.getSpecialites().add(new Specialite(rs.getLong("id"), rs.getString("libelle")));
                    }
                }
            }
        }
    }

    private static void insererSpecialites(Connection c, Photographe photographe) throws SQLException {
        if (photographe.getSpecialites().isEmpty()) {
            return;
        }
        String sql = "INSERT INTO photographe_specialite (photographe_id, specialite_id) VALUES (?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Specialite s : photographe.getSpecialites()) {
                ps.setLong(1, photographe.getId());
                ps.setLong(2, s.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Convertit les violations de contraintes en erreurs métier (409), au cas où deux requêtes
     * simultanées passent les contrôles du service ; le reste devient une erreur technique.
     */
    private static RuntimeException traduire(SQLException e, String message) {
        if (e.getErrorCode() == MYSQL_DOUBLON) {
            return new ConflitException("Cet email est déjà utilisé par un autre photographe.");
        }
        if (e.getErrorCode() == MYSQL_LIGNE_REFERENCEE) {
            return new ConflitException("Ce photographe a des séances : il ne peut pas être supprimé. Désactivez-le plutôt.");
        }
        return new DaoException(message, e);
    }

    private static Connection connexion() {
        Connection c = DBManager.getInstance().getConnection();
        if (c == null) {
            throw new DaoException("Connexion à la base de données impossible.");
        }
        return c;
    }
}
