package pl.tomaszmiller.repository.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.model.UserRole;
import pl.tomaszmiller.repository.port.UserRepository;

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
 * Supabase PostgREST-backed implementation of {@link UserRepository}.
 */
public class SupabaseUserRepository implements UserRepository {

    private static final String TABLE = "/rest/v1/users";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public SupabaseUserRepository(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.gson = new GsonBuilder().create();
    }

    @Override
    public List<User> findAll() throws Exception {
        HttpRequest req = buildGetRequest(baseUrl + TABLE + "?select=id,f_name,l_name,email,phone_number,user_rank&order=l_name.asc");
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
        JsonArray array = JsonParser.parseString(resp.body()).getAsJsonArray();
        List<User> users = new ArrayList<>();
        for (var el : array) {
            users.add(mapUser(el.getAsJsonObject()));
        }
        return users;
    }

    @Override
    public Optional<User> findById(long id) throws Exception {
        HttpRequest req = buildGetRequest(baseUrl + TABLE + "?id=eq." + id + "&limit=1");
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
        JsonArray array = JsonParser.parseString(resp.body()).getAsJsonArray();
        return array.isEmpty() ? Optional.empty() : Optional.of(mapUser(array.get(0).getAsJsonObject()));
    }

    @Override
    public Optional<User> findByEmail(String email) throws Exception {
        String url = baseUrl + TABLE + "?email=eq." + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&limit=1";
        HttpRequest req = buildGetRequest(url);
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
        JsonArray array = JsonParser.parseString(resp.body()).getAsJsonArray();
        return array.isEmpty() ? Optional.empty() : Optional.of(mapUser(array.get(0).getAsJsonObject()));
    }

    @Override
    public Optional<String> findPasswordHashByEmail(String email) throws Exception {
        String url = baseUrl + TABLE + "?email=eq." + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&select=password&limit=1";
        HttpRequest req = buildGetRequest(url);
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
        JsonArray array = JsonParser.parseString(resp.body()).getAsJsonArray();
        if (array.isEmpty()) {
            return Optional.empty();
        }
        JsonObject obj = array.get(0).getAsJsonObject();
        return obj.has("password") && !obj.get("password").isJsonNull()
                ? Optional.of(obj.get("password").getAsString())
                : Optional.empty();
    }

    @Override
    public User save(User user, String passwordHash) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("f_name", user.firstName());
        body.addProperty("l_name", user.lastName());
        body.addProperty("email", user.email());
        body.addProperty("password", passwordHash);
        body.addProperty("phone_number", user.phoneNumber());
        body.addProperty("user_rank", user.role().getDbValue());
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
            return mapUser(array.get(0).getAsJsonObject());
        }
        return user;
    }

    @Override
    public void update(User user) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("f_name", user.firstName());
        body.addProperty("l_name", user.lastName());
        body.addProperty("phone_number", user.phoneNumber());
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TABLE + "?id=eq." + user.id()))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
    }

    @Override
    public void updatePassword(long userId, String newPasswordHash) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("password", newPasswordHash);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TABLE + "?id=eq." + userId))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
    }

    @Override
    public void deleteById(long id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TABLE + "?id=eq." + id))
                .timeout(TIMEOUT)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .DELETE()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponse(resp);
    }

    @Override
    public boolean existsByEmail(String email) throws Exception {
        return findByEmail(email).isPresent();
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

    private User mapUser(JsonObject obj) {
        return new User(
                obj.has("id") ? obj.get("id").getAsLong() : 0L,
                obj.has("f_name") ? obj.get("f_name").getAsString() : "",
                obj.has("l_name") ? obj.get("l_name").getAsString() : "",
                obj.has("email") ? obj.get("email").getAsString() : "",
                obj.has("phone_number") && !obj.get("phone_number").isJsonNull() ? obj.get("phone_number").getAsString() : "",
                UserRole.fromDbValue(obj.has("user_rank") ? obj.get("user_rank").getAsInt() : 0)
        );
    }
}
