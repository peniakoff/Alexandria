package pl.tomaszmiller.model;

import java.time.LocalDate;

/**
 * Immutable representation of a book rental transaction.
 *
 * @param id           unique identifier (0 for unsaved entities)
 * @param userId       identifier of the borrowing user
 * @param bookId       identifier of the borrowed book
 * @param borrowDate   date the book was checked out
 * @param dueDate      date the book must be returned by
 * @param returnDate   actual return date, or {@code null} if not yet returned
 * @param status       current rental status
 */
public record Rental(
        long id,
        long userId,
        long bookId,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        RentalStatus status
) {
    /** Standard loan period in days. */
    public static final int DEFAULT_LOAN_DAYS = 14;

    /** Returns {@code true} if the rental is still open (book not returned). */
    public boolean isActive() {
        return returnDate == null;
    }

    /** Returns {@code true} if the book is overdue as of today. */
    public boolean isOverdue() {
        return isActive() && LocalDate.now().isAfter(dueDate);
    }
}
