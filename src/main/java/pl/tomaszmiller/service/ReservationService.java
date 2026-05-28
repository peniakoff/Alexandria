package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.model.RequestStatus;
import pl.tomaszmiller.model.Reservation;
import pl.tomaszmiller.repository.port.ReservationRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Application-level operations for book reservations.
 */
public class ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepo;

    public ReservationService(ReservationRepository reservationRepo) {
        this.reservationRepo = reservationRepo;
    }

    public List<Reservation> findAll() {
        try {
            return reservationRepo.findAll();
        } catch (Exception e) {
            LOGGER.error("Unable to load reservations.", e);
            return Collections.emptyList();
        }
    }

    public List<Reservation> findPending() {
        try {
            return reservationRepo.findByStatus(RequestStatus.PENDING);
        } catch (Exception e) {
            LOGGER.error("Unable to load pending reservations.", e);
            return Collections.emptyList();
        }
    }

    public List<Reservation> findByUser(long userId) {
        try {
            return reservationRepo.findByUserId(userId);
        } catch (Exception e) {
            LOGGER.error("Unable to load reservations for user {}.", userId, e);
            return Collections.emptyList();
        }
    }

    public List<Reservation> findByBook(long bookId) {
        try {
            return reservationRepo.findByBookId(bookId);
        } catch (Exception e) {
            LOGGER.error("Unable to load reservations for book {}.", bookId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Creates a reservation for a book that is currently borrowed.
     */
    public Optional<Reservation> reserve(long userId, long bookId) {
        try {
            Reservation reservation = new Reservation(0L, userId, bookId,
                    RequestStatus.PENDING, LocalDate.now());
            return Optional.of(reservationRepo.save(reservation));
        } catch (Exception e) {
            LOGGER.error("Unable to create reservation for book {} by user {}.", bookId, userId, e);
            return Optional.empty();
        }
    }

    public boolean approve(long reservationId) {
        try {
            reservationRepo.updateStatus(reservationId, RequestStatus.APPROVED);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to approve reservation {}.", reservationId, e);
            return false;
        }
    }

    public boolean reject(long reservationId) {
        try {
            reservationRepo.updateStatus(reservationId, RequestStatus.REJECTED);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to reject reservation {}.", reservationId, e);
            return false;
        }
    }
}
