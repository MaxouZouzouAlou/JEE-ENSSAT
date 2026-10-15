package org.tutorial;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookDAOImpl implements BookDAO{
    @Override
    public List<Book> findAll() {
        try(
            Connection connection = DBManager.getInstance().getConnection();
            Statement statement = connection.createStatement();
        ) {
            ResultSet rs = statement.executeQuery("SELECT id, title, author FROM books");
            List<Book> books = new ArrayList<>();
            while (rs.next()) {
                books.add(new Book(rs.getInt("id"), rs.getString("title"), rs.getString("author")));
            }
            return books;
        }catch(Exception e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public List<Book> findByTitle(String searchText) {
        try(
                Connection connection = DBManager.getInstance().getConnection();
                Statement statement = connection.createStatement();
        ) {
            ResultSet rs = statement.executeQuery("SELECT id, title, author FROM books WHERE title = "+searchText);
            List<Book> books = new ArrayList<>();
            while (rs.next()) {
                books.add(new Book(rs.getInt("id"), rs.getString("title"), rs.getString("author")));
            }
            return books;
        }catch(Exception e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}