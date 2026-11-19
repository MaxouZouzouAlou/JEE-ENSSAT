package fr.enssat.studiophoto;

import java.sql.*;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

public class DBManager {

	private static DBManager instance;

	private ResourceBundle properties;

	private static String resourceBundle = "application";

	private DBManager() {
		properties = ResourceBundle.getBundle(resourceBundle);

		try {
			Class.forName(parametre("database.driver"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Valeur d'un paramètre de connexion, par ordre de priorité :
	 * <ol>
	 *   <li>propriété système, ex. {@code java -Ddatabase.password=... -jar studiophoto.jar} ;</li>
	 *   <li>variable d'environnement, nom en majuscules avec {@code _} : {@code DATABASE_PASSWORD} ;</li>
	 *   <li>{@code application.properties}.</li>
	 * </ol>
	 * Le mot de passe peut ainsi être fourni au lancement plutôt que versionné dans le dépôt.
	 */
	private String parametre(String cle) {
		String valeur = System.getProperty(cle);
		if (valeur == null || valeur.isBlank()) {
			valeur = System.getenv(cle.toUpperCase().replace('.', '_'));
		}
		if (valeur == null || valeur.isBlank()) {
			valeur = properties.containsKey(cle) ? properties.getString(cle) : null;
		}
		return valeur;
	}

	public static DBManager getInstance() {
		if (instance == null) {
			synchronized (DBManager.class) {
				instance = new DBManager();
			}
		}
		return instance;
	}

	public Connection getConnection() {

		Connection connection = null;
		try {
			connection = DriverManager.getConnection(parametre("database.url"), parametre("database.login"),
					parametre("database.password"));

		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return connection;

	}

	public void cleanup(Connection connection, Statement stat, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (stat != null) {
			try {
				stat.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * permet de tester la connexion à la DB
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Connection c = DBManager.getInstance().getConnection();
		if (c != null) {
			try {
				System.out.println("Connection to db : " + c.getCatalog());
				Properties p = c.getClientInfo();
				Enumeration<Object> keys = p.keys();
				while (keys.hasMoreElements()) {
					String key = (String) keys.nextElement();
					System.out.println(key + ":" + p.getProperty(key));
				}

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				DBManager.getInstance().cleanup(c, null, null);
			}
		}
	}
}
