package pl.tomaszmiller.repository.rest;

import com.google.gson.*;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookInventory;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.repository.port.BookRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Supabase PostgREST-backed implementation of {@link BookRepository}.
 * Uses the standard {@code java.net.http.HttpClient} introduced in Java 11.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code alexandria.api.url}  – Supabase project URL, e.g. {@code https://xyz.supabase.co}</li>
 *   <li>{@code alexandria.api.key}  – Supabase anon/service-role key</li>
 * </ul>
 */
public class SupabaseBookRepository implements BookRepository {

    private static final String TABLE = "/rest/v1/books";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public SupabaseBookRepository(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.gson = new GsonBuilder().create();
    }

    @Override
    public List<String> loadBookTitles() throws Exception {
        List<String> titles = new ArrayList<>();
        for (Book b : findAll()) {
            titles.add(b.title());
        }
        titles.sort(String::compareToIgnoreCase);
        return titles;
    }

    @Override
    public Optional<Book> findByTitle(String title) throws Exception {
        String url = baseUrl + TABLE + "?title=eq."
                + URLEncoder.encode(title, StandardCharsets.UTF_8) + "&limit=1";
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
        if (array.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapBook(array.get(0).getAsJsonObject()));
    }

    @Override
    public List<Book> findAll() throws Exception {
        HttpRequest request = buildGetRequest(baseUrl + TABLE + "?order=title.asc");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
        List<Book> books = new ArrayList<>();
        for (var element : array) {
            books.add(mapBook(element.getAsJsonObject()));
        }
        return books;
    }

    @Override
    public Optional<Book> findById(long id) throws Exception {
        HttpRequest request = buildGetRequest(baseUrl + TABLE + "?id=eq." + id + "&limit=1");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
        if (array.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapBook(array.get(0).getAsJsonObject()));
    }

    @Override
    public Book save(Book book) throws Exception {
        JsonObject body = toJson(book);
        body.remove("id");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TABLE))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
        if (!array.isEmpty()) {
            return mapBook(array.get(0).getAsJsonObject());
        }
        return book;
    }

    @Override
    public void update(Book book) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TABLE + "?id=eq." + book.id()))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(toJson(book))))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
    }

    @Override
    public void deleteById(long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TABLE + "?id=eq." + id))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
    }

    private HttpRequest buildGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private void checkResponse(HttpResponse<String> response) throws Exception {
        if (response.statusCode() >= 400) {
            throw new Exception("Supabase API error " + response.statusCode() + ": " + response.body());
        }
    }

    private Book mapBook(JsonObject obj) {
        BookStatus status;
        try {
            status = BookStatus.valueOf(obj.has("status") && !obj.get("status").isJsonNull()
                    ? obj.get("status").getAsString() : "AVAILABLE");
        } catch (IllegalArgumentException e) {
            status = BookStatus.AVAILABLE;
        }
        return new Book(
                obj.has("id") ? obj.get("id").getAsLong() : 0L,
                obj.has("author") ? obj.get("author").getAsString() : "",
                obj.has("title") ? obj.get("title").getAsString() : "",
                obj.has("pages") ? obj.get("pages").getAsInt() : 0,
                obj.has("isbn") && !obj.get("isbn").isJsonNull() ? obj.get("isbn").getAsString() : null,
                status,
                obj.has("publish_year") ? obj.get("publish_year").getAsInt() : 0,
                obj.has("publisher") && !obj.get("publisher").isJsonNull() ? obj.get("publisher").getAsString() : null,
                new BookInventory(
                        obj.has("active_copies") ? obj.get("active_copies").getAsInt() : 1,
                        obj.has("available_copies") ? obj.get("available_copies").getAsInt() : 1,
                        obj.has("archived_copies") ? obj.get("archived_copies").getAsInt() : 0,
                        obj.has("removed_damaged_copies") ? obj.get("removed_damaged_copies").getAsInt() : 0,
                        obj.has("removed_stolen_copies") ? obj.get("removed_stolen_copies").getAsInt() : 0
                )
        );
    }

    private JsonObject toJson(Book book) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", book.id());
        obj.addProperty("author", book.author());
        obj.addProperty("title", book.title());
        obj.addProperty("pages", book.pages());
        if (book.isbn() != null) {
            obj.addProperty("isbn", book.isbn());
        }
        obj.addProperty("status", book.status().name());
        obj.addProperty("publish_year", book.publishYear());
        if (book.publisher() != null) {
            obj.addProperty("publisher", book.publisher());
        }
        obj.addProperty("active_copies", book.inventory().activeCopies());
        obj.addProperty("available_copies", book.inventory().availableCopies());
        obj.addProperty("archived_copies", book.inventory().archivedCopies());
        obj.addProperty("removed_damaged_copies", book.inventory().removedDamagedCopies());
        obj.addProperty("removed_stolen_copies", book.inventory().removedStolenCopies());
        return obj;
    }
}
