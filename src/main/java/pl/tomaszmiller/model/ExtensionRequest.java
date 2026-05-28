package pl.tomaszmiller.model;

import java.time.LocalDate;

/**
 * Represents a request from a user to extend a rental period.
 *
 * @param id          unique identifier
 * @param rentalId    the rental to be extended
 * @param userId      the requesting user
 * @param status      PENDING, APPROVED, or REJECTED
 * @param requestDate date the request was made
 */
public record ExtensionRequest(
        long id,
        long rentalId,
        long userId,
        RequestStatus status,
        LocalDate requestDate
) {
    /**
     * Default extension period in days.
     */
    public static final int EXTENSION_DAYS = 7;
}
