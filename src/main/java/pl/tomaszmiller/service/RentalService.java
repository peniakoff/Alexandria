package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;
import pl.tomaszmiller.repository.port.RentalRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Application-level operations on rental transactions.
 */
public class RentalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RentalService.class);

    private final RentalRepository rentalRepository;

    public RentalService(RentalRepository rentalRepository) {
        this.rentalRepository = rentalRepository;
    }

    public List<Rental> findAll() {
        try {
            return rentalRepository.findAll();
        } catch (Exception e) {
            LOGGER.error("Unable to load rentals.", e);
            return Collections.emptyList();
        }
    }

    public List<Rental> findByUser(long userId) {
        try {
            return rentalRepository.findByUserId(userId);
        } catch (Exception e) {
            LOGGER.error("Unable to load rentals for user id={}.", userId, e);
            return Collections.emptyList();
        }
    }

    public Optional<Rental> borrow(long userId, long bookId) {
        try {
            LocalDate today = LocalDate.now();
            Rental rental = new Rental(0L, userId, bookId, today,
                    today.plusDays(Rental.DEFAULT_LOAN_DAYS), null, RentalStatus.ACTIVE);
            return Optional.of(rentalRepository.save(rental));
        } catch (Exception e) {
            LOGGER.error("Unable to create rental for user={} book={}.", userId, bookId, e);
            return Optional.empty();
        }
    }

    public boolean returnBook(long rentalId) {
        try {
            Optional<Rental> opt = rentalRepository.findById(rentalId);
            if (opt.isEmpty()) {
                return false;
            }
            Rental r = opt.get();
            RentalStatus newStatus = LocalDate.now().isAfter(r.dueDate())
                    ? RentalStatus.RETURNED_LATE : RentalStatus.RETURNED;
            Rental updated = new Rental(r.id(), r.userId(), r.bookId(),
                    r.borrowDate(), r.dueDate(), LocalDate.now(), newStatus);
            rentalRepository.update(updated);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to process return for rental id={}.", rentalId, e);
            return false;
        }
    }

    public List<Rental> findOverdue() {
        try {
            return rentalRepository.findByStatus(RentalStatus.OVERDUE);
        } catch (Exception e) {
            LOGGER.error("Unable to load overdue rentals.", e);
            return Collections.emptyList();
        }
    }
}
