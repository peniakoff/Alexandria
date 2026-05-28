package pl.tomaszmiller.repository.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.repository.port.BookRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HttpBookRepository implements BookRepository {

    private final HttpJsonClient client;

    public HttpBookRepository(String apiBaseUrl) {
        this.client = new HttpJsonClient(apiBaseUrl);
    }

    @Override
    public List<String> loadBookTitles() throws Exception {
        var resp = client.get("/api/books/titles");
        HttpJsonClient.ensure2xx(resp);
        JsonArray array = JsonParser.parseString(resp.body()).getAsJsonArray();
        List<String> titles = new ArrayList<>();
        for (var el : array) {
            titles.add(el.getAsString());
        }
        return titles;
    }

    @Override
    public Optional<Book> findByTitle(String title) throws Exception {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
        var resp = client.get("/api/books/by-title?title=" + encoded);
        if (resp.statusCode() == 404) {
            return Optional.empty();
        }
        HttpJsonClient.ensure2xx(resp);
        return Optional.of(client.gson.fromJson(resp.body(), Book.class));
    }

    @Override
    public List<Book> findAll() throws Exception {
        var resp = client.get("/api/books");
        HttpJsonClient.ensure2xx(resp);
        Book[] books = client.gson.fromJson(resp.body(), Book[].class);
        return books == null ? List.of() : List.of(books);
    }

    @Override
    public Optional<Book> findById(long id) throws Exception {
        var resp = client.get("/api/books/" + id);
        if (resp.statusCode() == 404) {
            return Optional.empty();
        }
        HttpJsonClient.ensure2xx(resp);
        return Optional.of(client.gson.fromJson(resp.body(), Book.class));
    }

    @Override
    public Book save(Book book) throws Exception {
        var resp = client.post("/api/books", client.gson.toJson(book));
        HttpJsonClient.ensure2xx(resp);
        return client.gson.fromJson(resp.body(), Book.class);
    }

    @Override
    public void update(Book book) throws Exception {
        var resp = client.put("/api/books/" + book.id(), client.gson.toJson(book));
        HttpJsonClient.ensure2xx(resp);
    }

    @Override
    public void deleteById(long id) throws Exception {
        var resp = client.delete("/api/books/" + id);
        HttpJsonClient.ensure2xx(resp);
    }
}

