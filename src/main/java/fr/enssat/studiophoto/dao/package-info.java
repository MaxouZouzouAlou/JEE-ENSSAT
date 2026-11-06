/**
 * Accès aux données en JDBC : une interface (ex. {@code SeanceDAO}) et son
 * implémentation ({@code SeanceDAOImpl}). Les connexions sont obtenues par
 * {@code fr.enssat.studiophoto.DBManager.getInstance().getConnection()}, dans un try-with-resources. Utiliser des
 * {@code PreparedStatement} (jamais de concaténation de paramètres dans le SQL).
 */
package fr.enssat.studiophoto.dao;
