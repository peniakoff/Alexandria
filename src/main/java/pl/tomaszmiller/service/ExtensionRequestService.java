package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.model.*;
import pl.tomaszmiller.repository.port.ExtensionRequestRepository;
import pl.tomaszmiller.repository.port.RentalRepository;
import pl.tomaszmiller.repository.port.ReservationRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Application-level operations for extension requests.
 */
public class ExtensionRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionRequestService.class);
    private final ExtensionRequestRepository extensionRepo;
    private final RentalRepository rentalRepo;
    private final ReservationRepository reservationRepo;
    public ExtensionRequestService(ExtensionRequestRepository extensionRepo,
                                   RentalRepository rentalRepo,
                                   ReservationRepository reservationRepo) {
        this.extensionRepo = extensionRepo;
        this.rentalRepo = rentalRepo;
        this.reservationRepo = reservationRepo;
    }

    public List<ExtensionRequest> findAll() {
        try {
            return extensionRepo.findAll();
        } catch (Exception e) {
            LOGGER.error("Unable to load extension requests.", e);
            return Collections.emptyList();
        }
    }

    public List<ExtensionRequest> findPending() {
        try {
            return extensionRepo.findByStatus(RequestStatus.PENDING);
        } catch (Exception e) {
            LOGGER.error("Unable to load pending extension requests.", e);
            return Collections.emptyList();
        }
    }

    public List<ExtensionRequest> findByUser(long userId) {
        try {
            return extensionRepo.findByUserId(userId);
        } catch (Exception e) {
            LOGGER.error("Unable to load extension requests for user {}.", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Creates an extension request for a rental.
     */
    public Optional<ExtensionRequest> requestExtension(long rentalId, long userId) {
        try {
            ExtensionRequest req = new ExtensionRequest(0L, rentalId, userId,
                    RequestStatus.PENDING, LocalDate.now());
            return Optional.of(extensionRepo.save(req));
        } catch (Exception e) {
            LOGGER.error("Unable to create extension request for rental {}.", rentalId, e);
            return Optional.empty();
        }
    }

    /**
     * Approves an extension request: extends the rental due date by 7 days.
     * Will be rejected if the book has a pending/approved reservation.
     */
    public ApprovalResult approve(long requestId) {
        try {
            Optional<ExtensionRequest> reqOpt = extensionRepo.findById(requestId);
            if (reqOpt.isEmpty()) {
                return ApprovalResult.REQUEST_NOT_FOUND;
            }
            ExtensionRequest req = reqOpt.get();
            Optional<Rental> rentalOpt = rentalRepo.findById(req.rentalId());
            if (rentalOpt.isEmpty()) {
                return ApprovalResult.RENTAL_NOT_FOUND;
            }

            // Check if the book has a pending/approved reservation
            Rental rental = rentalOpt.get();
            List<Reservation> bookReservations = reservationRepo.findByBookId(rental.bookId());
            boolean hasActiveReservation = bookReservations.stream()
                    .anyMatch(r -> r.status() == RequestStatus.PENDING || r.status() == RequestStatus.APPROVED);
            if (hasActiveReservation) {
                extensionRepo.updateStatus(requestId, RequestStatus.REJECTED);
                return ApprovalResult.RESERVATION_CONFLICT;
            }

            // Extend the due date
            LocalDate newDueDate = rental.dueDate().plusDays(ExtensionRequest.EXTENSION_DAYS);
            Rental extended = new Rental(rental.id(), rental.userId(), rental.bookId(),
                    rental.borrowDate(), newDueDate, rental.returnDate(), RentalStatus.ACTIVE);
            rentalRepo.update(extended);
            extensionRepo.updateStatus(requestId, RequestStatus.APPROVED);
            return ApprovalResult.APPROVED;
        } catch (Exception e) {
            LOGGER.error("Unable to approve extension request {}.", requestId, e);
            return ApprovalResult.ERROR;
        }
    }

    public boolean reject(long requestId) {
        try {
            extensionRepo.updateStatus(requestId, RequestStatus.REJECTED);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to reject extension request {}.", requestId, e);
            return false;
        }
    }

    /**
     * Detailed result of an extension-approval attempt.
     */
    public enum ApprovalResult {
        APPROVED,
        REQUEST_NOT_FOUND,
        RENTAL_NOT_FOUND,
        RESERVATION_CONFLICT,
        ERROR
    }
}
