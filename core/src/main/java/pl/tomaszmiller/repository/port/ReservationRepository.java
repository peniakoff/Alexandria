package pl.tomaszmiller.repository.port;

import pl.tomaszmiller.model.RequestStatus;
import pl.tomaszmiller.model.Reservation;

import java.util.List;
import java.util.Optional;

/**
 * Port for reservation persistence.
 */
public interface ReservationRepository {
    List<Reservation> findAll() throws Exception;

    List<Reservation> findByStatus(RequestStatus status) throws Exception;

    List<Reservation> findByUserId(long userId) throws Exception;

    List<Reservation> findByBookId(long bookId) throws Exception;

    Optional<Reservation> findById(long id) throws Exception;

    Reservation save(Reservation reservation) throws Exception;

    void updateStatus(long id, RequestStatus status) throws Exception;
}
