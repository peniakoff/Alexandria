package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookInventory;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;
import pl.tomaszmiller.repository.port.BookRepository;
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
    private final BookRepository bookRepository;

    public RentalService(RentalRepository rentalRepository, BookRepository bookRepository) {
        this.rentalRepository = rentalRepository;
        this.bookRepository = bookRepository;
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
        Book originalBook = null;
        try {
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isEmpty() || !bookOpt.get().isBorrowable()) {
                return Optional.empty();
            }
            originalBook = bookOpt.get();
            Book updatedBook = originalBook.withInventory(originalBook.inventory().borrowCopy());
            bookRepository.update(updatedBook);

            LocalDate today = LocalDate.now();
            Rental rental = new Rental(0L, userId, bookId, today,
                    today.plusDays(Rental.DEFAULT_LOAN_DAYS), null, RentalStatus.ACTIVE);
            return Optional.of(rentalRepository.save(rental));
        } catch (Exception e) {
            rollbackInventory(originalBook);
            LOGGER.error("Unable to create rental for user={} book={}.", userId, bookId, e);
            return Optional.empty();
        }
    }

    public boolean returnBook(long rentalId) {
        Book originalBook = null;
        try {
            Optional<Rental> opt = rentalRepository.findById(rentalId);
            if (opt.isEmpty()) {
                return false;
            }
            Rental r = opt.get();
            Optional<Book> bookOpt = bookRepository.findById(r.bookId());
            if (bookOpt.isEmpty()) {
                return false;
            }
            originalBook = bookOpt.get();

            RentalStatus newStatus = LocalDate.now().isAfter(r.dueDate())
                    ? RentalStatus.RETURNED_LATE : RentalStatus.RETURNED;
            Rental updatedRental = new Rental(r.id(), r.userId(), r.bookId(),
                    r.borrowDate(), r.dueDate(), LocalDate.now(), newStatus);
            Book updatedBook = originalBook.withInventory(originalBook.inventory().returnCopy());

            bookRepository.update(updatedBook);
            rentalRepository.update(updatedRental);
            return true;
        } catch (Exception e) {
            rollbackInventory(originalBook);
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

    private void rollbackInventory(Book originalBook) {
        if (originalBook == null) {
            return;
        }
        try {
            bookRepository.update(originalBook);
        } catch (Exception rollbackError) {
            LOGGER.error("Unable to roll back inventory for book id={}.", originalBook.id(), rollbackError);
        }
    }
}
