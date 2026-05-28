package pl.tomaszmiller.repository.http;

import pl.tomaszmiller.model.User;
import pl.tomaszmiller.repository.port.UserRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public final class HttpUserRepository implements UserRepository {

    private final HttpJsonClient client;

    public HttpUserRepository(String apiBaseUrl) {
        this.client = new HttpJsonClient(apiBaseUrl);
    }

    @Override
    public List<User> findAll() throws Exception {
        var resp = client.get("/api/users");
        HttpJsonClient.ensure2xx(resp);
        User[] users = client.gson.fromJson(resp.body(), User[].class);
        return users == null ? List.of() : List.of(users);
    }

    @Override
    public Optional<User> findById(long id) throws Exception {
        var resp = client.get("/api/users/" + id);
        if (resp.statusCode() == 404) {
            return Optional.empty();
        }
        HttpJsonClient.ensure2xx(resp);
        return Optional.ofNullable(client.gson.fromJson(resp.body(), User.class));
    }

    @Override
    public Optional<User> findByEmail(String email) throws Exception {
        String encoded = URLEncoder.encode(email, StandardCharsets.UTF_8);
        var resp = client.get("/api/users/by-email?email=" + encoded);
        if (resp.statusCode() == 404) {
            return Optional.empty();
        }
        HttpJsonClient.ensure2xx(resp);
        return Optional.ofNullable(client.gson.fromJson(resp.body(), User.class));
    }

    @Override
    public Optional<String> findPasswordHashByEmail(String email) throws Exception {
        throw new UnsupportedOperationException("Password hash retrieval is not supported over HTTP.");
    }

    @Override
    public User save(User user, String passwordHash) throws Exception {
        var payload = client.gson.toJson(new CreateUserRequest(user, passwordHash));
        var resp = client.post("/api/users", payload);
        HttpJsonClient.ensure2xx(resp);
        return client.gson.fromJson(resp.body(), User.class);
    }

    @Override
    public void update(User user) throws Exception {
        var resp = client.put("/api/users/" + user.id(), client.gson.toJson(user));
        HttpJsonClient.ensure2xx(resp);
    }

    @Override
    public void updatePassword(long userId, String newPasswordHash) throws Exception {
        var payload = client.gson.toJson(new UpdatePasswordRequest(newPasswordHash));
        var resp = client.put("/api/users/" + userId + "/password", payload);
        HttpJsonClient.ensure2xx(resp);
    }

    @Override
    public void deleteById(long id) throws Exception {
        var resp = client.delete("/api/users/" + id);
        HttpJsonClient.ensure2xx(resp);
    }

    @Override
    public boolean existsByEmail(String email) throws Exception {
        String encoded = URLEncoder.encode(email, StandardCharsets.UTF_8);
        var resp = client.get("/api/users/exists?email=" + encoded);
        HttpJsonClient.ensure2xx(resp);
        return Boolean.parseBoolean(resp.body());
    }

    private record CreateUserRequest(User user, String passwordHash) {
    }

    private record UpdatePasswordRequest(String passwordHash) {
    }
}

