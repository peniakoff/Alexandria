package pl.tomaszmiller.model;

import java.time.LocalDate;

/**
 * Represents a reservation request for a book that is currently borrowed.
 *
 * @param id          unique identifier
 * @param userId      the requesting user
 * @param bookId      the book to be reserved
 * @param status      PENDING, APPROVED, REJECTED, or CANCELLED
 * @param requestDate date the request was made
 */
public record Reservation(
        long id,
        long userId,
        long bookId,
        RequestStatus status,
        LocalDate requestDate
) {
}
