package pl.tomaszmiller.model;

/**
 * Current status of a book rental.
 */
public enum RentalStatus {
    /**
     * Book has been borrowed and not yet returned.
     */
    ACTIVE,
    /**
     * Book has been returned on time.
     */
    RETURNED,
    /**
     * Book was not returned by the due date.
     */
    OVERDUE,
    /**
     * Book has been returned after the due date.
     */
    RETURNED_LATE
}
