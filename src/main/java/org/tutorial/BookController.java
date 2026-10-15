package org.tutorial;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.logging.Logger;

@Path("/books")
public class BookController {
    private BookDAO bookDAO = new BookDAOImpl();

    private static final Logger LOGGER = Logger.getLogger(BookController.class.getName());

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/hello")
    public String hello() {
        LOGGER.info("Hello world !");
        return "Hello World!";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> getBooks(@QueryParam("title") String title){
        List<Book> books;
        if (title == null){
            books = this.bookDAO.findAll();
        }
        else{
            books = this.bookDAO.findByTitle(title);
        }
        return books;
    }
}