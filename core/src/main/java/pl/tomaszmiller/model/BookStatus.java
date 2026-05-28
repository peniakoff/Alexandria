package pl.tomaszmiller.model;

/**
 * Availability status of a book in the library catalog.
 */
public enum BookStatus {
    /**
     * The book is on the shelf and available for borrowing.
     */
    AVAILABLE,
    /**
     * All copies are currently borrowed.
     */
    BORROWED,
    /**
     * The book is reserved by a user.
     */
    RESERVED,
    /**
     * The book is temporarily unavailable (maintenance, lost, etc.).
     */
    UNAVAILABLE,
    /**
     * No copies remain in circulation but archived copies still exist.
     */
    ARCHIVED,
    /**
     * No copies remain because all were removed permanently.
     */
    REMOVED
}
