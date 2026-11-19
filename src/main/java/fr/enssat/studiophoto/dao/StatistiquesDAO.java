package fr.enssat.studiophoto.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import fr.enssat.studiophoto.DBManager;
import fr.enssat.studiophoto.exception.DaoException;

/**
 * Requêtes des statistiques (lot 5). Seules les séances TERMINEE comptent comme réalisées,
 * et seules les factures PAYEE entrent dans le chiffre d'affaires.
 *
 * <p>Les classements renvoient des lignes {@code [nom, valeur]}, triées par valeur décroissante
 * puis par nom (ordre stable en cas d'égalité), 5 au plus.</p>
 */
public class StatistiquesDAO {

    /** Regroupement par identifiant : deux photographes homonymes restent distincts. */
    private static final String SQL_TOP_SEANCES =
            "SELECT p.nom, COUNT(*) AS valeur FROM seance s JOIN photographe p ON p.id = s.photographe_id "
            + "WHERE s.statut = 'TERMINEE' GROUP BY p.id, p.nom ORDER BY valeur DESC, p.nom LIMIT 5";

    private static final String SQL_TOP_DUREE =
            "SELECT p.nom, SUM(s.duree_minutes) AS valeur FROM seance s JOIN photographe p ON p.id = s.photographe_id "
            + "WHERE s.statut = 'TERMINEE' GROUP BY p.id, p.nom ORDER BY valeur DESC, p.nom LIMIT 5";

    /** COALESCE : 0 et non NULL quand aucune facture payée n'a été émise sur la période. */
    private static final String SQL_CHIFFRE_AFFAIRES =
            "SELECT COALESCE(SUM(montant_ttc), 0) AS total FROM facture "
            + "WHERE statut_paiement = 'PAYEE' AND date_emission BETWEEN ? AND ?";

    /** Top 5 des photographes par nombre de séances réalisées : lignes {@code [nom, nombre]}. */
    public List<Object[]> listerPhotographesParSeance() {
        return classement(SQL_TOP_SEANCES, "Lecture du classement par nombre de séances impossible");
    }

    /** Top 5 des photographes par durée cumulée des séances réalisées : lignes {@code [nom, minutes]}. */
    public List<Object[]> listerPhotographesParDuree() {
        return classement(SQL_TOP_DUREE, "Lecture du classement par durée impossible");
    }

    /** Somme des montants TTC des factures PAYEE émises entre les deux dates, incluses. */
    public BigDecimal calculerChiffreAffairesPourPeriode(LocalDate dateDebut, LocalDate dateFin) {
        try (Connection connection = connexion();
             PreparedStatement statement = connection.prepareStatement(SQL_CHIFFRE_AFFAIRES)) {
            statement.setDate(1, Date.valueOf(dateDebut));
            statement.setDate(2, Date.valueOf(dateFin));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new DaoException("Calcul du chiffre d'affaires impossible", e);
        }
    }

    private List<Object[]> classement(String sql, String messageErreur) {
        List<Object[]> result = new ArrayList<>();
        try (Connection connection = connexion();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Object[] {rs.getString("nom"), rs.getInt("valeur")});
            }
            return result;
        } catch (SQLException e) {
            throw new DaoException(messageErreur, e);
        }
    }

    private static Connection connexion() {
        Connection connection = DBManager.getInstance().getConnection();
        if (connection == null) {
            throw new DaoException("Connexion à la base de données impossible.");
        }
        return connection;
    }
}
