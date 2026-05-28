package pl.tomaszmiller.model;

/**
 * Status of a request (extension request or reservation).
 */
public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    public static RequestStatus fromString(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return PENDING;
        }
    }
}
