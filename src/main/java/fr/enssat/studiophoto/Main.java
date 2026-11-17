package fr.enssat.studiophoto;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import fr.enssat.studiophoto.model.SeanceController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            final HttpServer httpServer = initServer();
            httpServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    LOGGER.info("Arrêt de l'application...");
                    httpServer.shutdownNow();
                    LOGGER.info("Fermeture terminée.");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, null, e);
                }
            }));    

            LOGGER.info("Application démarrée. (Faites CTRL+C pour arrêter)");
            Thread.currentThread().join();

        } catch (InterruptedException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static HttpServer initServer() {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            props.load(input);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        String baseUri = props.getProperty("server.base.uri");
        String apiUri = baseUri + props.getProperty("server.api.uri");

        final ResourceConfig rc = new ResourceConfig();

        // Ressources du studio photo : toutes les classes @Path du package sont enregistrées
        rc.packages("fr.enssat.studiophoto.resource");
        rc.register(SeanceController.class);

        HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(apiUri), rc);

        CLStaticHttpHandler staticHttpHandler = new CLStaticHttpHandler(Main.class.getClassLoader(), "/static/");
        httpServer.getServerConfiguration().addHttpHandler(staticHttpHandler, "/");

        return httpServer;
    }
}