package pl.tomaszmiller.repository.rest;

import com.google.gson.*;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;
import pl.tomaszmiller.repository.port.RentalRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Supabase PostgREST-backed implementation of {@link RentalRepository}.
 */
public class SupabaseRentalRepository implements RentalRepository {

    private static final String TABLE = "/rest/v1/rentals";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public SupabaseRentalRepository(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.gson = new GsonBuilder().create();
    }

    @Override
    public List<Rental> findAll() throws Exception {
        return fetch(baseUrl + TABLE + "?order=borrow_date.desc");
    }

    @Override
    public Optional<Rental> findById(long id) throws Exception {
        List<Rental> list = fetch(baseUrl + TABLE + "?id=eq." + id + "&limit=1");
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Rental> findByUserId(long userId) throws Exception {
        return fetch(baseUrl + TABLE + "?user_id=eq." + userId + "&order=borrow_date.desc");
    }

    @Override
    public List<Rental> findByBookId(long bookId) throws Exception {
        return fetch(baseUrl + TABLE + "?book_id=eq." + bookId + "&order=borrow_date.desc");
    }

    @Override
    public List<Rental> findByStatus(RentalStatus status) throws Exception {
        return fetch(baseUrl + TABLE + "?status=eq." + status.name());
    }

    @Override
    public Rental save(Rental rental) throws Exception {
        JsonObject body = toJson(rental);
        body.remove("id");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TABLE))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
        JsonArray array = JsonParser.parseString(resp.body()).getAsJsonArray();
        if (!array.isEmpty()) {
            return mapRental(array.get(0).getAsJsonObject());
        }
        return rental;
    }

    @Override
    public void update(Rental rental) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("status", rental.status().name());
        if (rental.returnDate() != null) {
            body.addProperty("return_date", rental.returnDate().toString());
        }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TABLE + "?id=eq." + rental.id()))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
    }

    private List<Rental> fetch(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
        JsonArray array = JsonParser.parseString(resp.body()).getAsJsonArray();
        List<Rental> list = new ArrayList<>();
        for (var el : array) {
            list.add(mapRental(el.getAsJsonObject()));
        }
        return list;
    }

    private void checkResponse(HttpResponse<String> response) throws Exception {
        if (response.statusCode() >= 400) {
            throw new Exception("Supabase API error " + response.statusCode() + ": " + response.body());
        }
    }

    private Rental mapRental(JsonObject obj) {
        String returnDateStr = obj.has("return_date") && !obj.get("return_date").isJsonNull()
                ? obj.get("return_date").getAsString() : null;
        RentalStatus status;
        try {
            status = RentalStatus.valueOf(obj.has("status") ? obj.get("status").getAsString() : "ACTIVE");
        } catch (IllegalArgumentException e) {
            status = RentalStatus.ACTIVE;
        }
        return new Rental(
                obj.has("id") ? obj.get("id").getAsLong() : 0L,
                obj.has("user_id") ? obj.get("user_id").getAsLong() : 0L,
                obj.has("book_id") ? obj.get("book_id").getAsLong() : 0L,
                LocalDate.parse(obj.get("borrow_date").getAsString()),
                LocalDate.parse(obj.get("due_date").getAsString()),
                returnDateStr != null ? LocalDate.parse(returnDateStr) : null,
                status
        );
    }

    private JsonObject toJson(Rental rental) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", rental.id());
        obj.addProperty("user_id", rental.userId());
        obj.addProperty("book_id", rental.bookId());
        obj.addProperty("borrow_date", rental.borrowDate().toString());
        obj.addProperty("due_date", rental.dueDate().toString());
        if (rental.returnDate() != null) {
            obj.addProperty("return_date", rental.returnDate().toString());
        }
        obj.addProperty("status", rental.status().name());
        return obj;
    }
}
