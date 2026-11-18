package fr.enssat.studiophoto.dao;

import java.sql.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import fr.enssat.studiophoto.DBManager;

public class PhotographeDAO {

    public boolean isActif(Long photographeId, Date debut, Date fin) {
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
}