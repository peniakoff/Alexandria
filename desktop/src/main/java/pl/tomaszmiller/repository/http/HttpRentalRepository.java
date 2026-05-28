package pl.tomaszmiller.repository.http;

import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;
import pl.tomaszmiller.repository.port.RentalRepository;

import java.util.List;
import java.util.Optional;

public final class HttpRentalRepository implements RentalRepository {

    private final HttpJsonClient client;

    public HttpRentalRepository(String apiBaseUrl) {
        this.client = new HttpJsonClient(apiBaseUrl);
    }

    @Override
    public List<Rental> findAll() throws Exception {
        var resp = client.get("/api/rentals");
        HttpJsonClient.ensure2xx(resp);
        Rental[] rentals = client.gson.fromJson(resp.body(), Rental[].class);
        return rentals == null ? List.of() : List.of(rentals);
    }

    @Override
    public Optional<Rental> findById(long id) throws Exception {
        var resp = client.get("/api/rentals/" + id);
        if (resp.statusCode() == 404) {
            return Optional.empty();
        }
        HttpJsonClient.ensure2xx(resp);
        return Optional.of(client.gson.fromJson(resp.body(), Rental.class));
    }

    @Override
    public List<Rental> findByUserId(long userId) throws Exception {
        var resp = client.get("/api/rentals/by-user/" + userId);
        HttpJsonClient.ensure2xx(resp);
        Rental[] rentals = client.gson.fromJson(resp.body(), Rental[].class);
        return rentals == null ? List.of() : List.of(rentals);
    }

    @Override
    public List<Rental> findByBookId(long bookId) throws Exception {
        var resp = client.get("/api/rentals/by-book/" + bookId);
        HttpJsonClient.ensure2xx(resp);
        Rental[] rentals = client.gson.fromJson(resp.body(), Rental[].class);
        return rentals == null ? List.of() : List.of(rentals);
    }

    @Override
    public List<Rental> findByStatus(RentalStatus status) throws Exception {
        var resp = client.get("/api/rentals/by-status?status=" + status.name());
        HttpJsonClient.ensure2xx(resp);
        Rental[] rentals = client.gson.fromJson(resp.body(), Rental[].class);
        return rentals == null ? List.of() : List.of(rentals);
    }

    @Override
    public Rental save(Rental rental) throws Exception {
        var resp = client.post("/api/rentals", client.gson.toJson(rental));
        HttpJsonClient.ensure2xx(resp);
        return client.gson.fromJson(resp.body(), Rental.class);
    }

    @Override
    public void update(Rental rental) throws Exception {
        var resp = client.put("/api/rentals/" + rental.id(), client.gson.toJson(rental));
        HttpJsonClient.ensure2xx(resp);
    }
}

