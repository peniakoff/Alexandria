package pl.tomaszmiller.model;

/**
 * Bibliographic metadata used for external lookups and form prefilling.
 */
public record BookMetadata(
        String author,
        String title,
        int pages,
        String isbn,
        int publishYear,
        String publisher
) {
}
