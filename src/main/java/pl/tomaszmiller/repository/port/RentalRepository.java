package pl.tomaszmiller.repository.port;

import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for rental transaction persistence.
 */
public interface RentalRepository {
    /** Returns all rentals in the system. */
    List<Rental> findAll() throws Exception;

    /** Finds a rental by primary key. */
    Optional<Rental> findById(long id) throws Exception;

    /** Returns all rentals for a given user. */
    List<Rental> findByUserId(long userId) throws Exception;

    /** Returns all rentals for a given book. */
    List<Rental> findByBookId(long bookId) throws Exception;

    /** Returns all rentals with the given status. */
    List<Rental> findByStatus(RentalStatus status) throws Exception;

    /** Persists a new rental; returns the saved entity with generated id. */
    Rental save(Rental rental) throws Exception;

    /** Updates status and/or return date of an existing rental. */
    void update(Rental rental) throws Exception;
}
