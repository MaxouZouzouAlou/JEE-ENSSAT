package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.DBManager;
import fr.enssat.studiophoto.exception.DaoException;
import fr.enssat.studiophoto.model.Specialite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Implémentation JDBC de {@link SpecialiteDAO}. */
public class SpecialiteDAOImpl implements SpecialiteDAO {

    @Override
    public List<Specialite> lister() {
        String sql = "SELECT id, libelle FROM specialite ORDER BY libelle";
        Connection c = DBManager.getInstance().getConnection();
        if (c == null) {
            throw new DaoException("Connexion à la base de données impossible.");
        }
        try (c; PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Specialite> specialites = new ArrayList<>();
            while (rs.next()) {
                specialites.add(new Specialite(rs.getLong("id"), rs.getString("libelle")));
            }
            return specialites;
        } catch (SQLException e) {
            throw new DaoException("Impossible de lire les spécialités.", e);
        }
    }
}
