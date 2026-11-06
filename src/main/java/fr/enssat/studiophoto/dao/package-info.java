/**
 * Accès aux données en JDBC, sur le modèle de {@code BookDAO} / {@code BookDAOImpl} :
 * une interface (ex. {@code SeanceDAO}) et son implémentation ({@code SeanceDAOImpl}).
 * Les connexions sont obtenues par {@code DBManager}. Utiliser des
 * {@code PreparedStatement} (jamais de concaténation de paramètres dans le SQL).
 */
package fr.enssat.studiophoto.dao;
