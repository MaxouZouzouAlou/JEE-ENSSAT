package org.tutorial;

import java.util.List;

public interface BookDAO {

    public List<Book> findAll();

    List<Book> findByTitle(String searchText);
}
