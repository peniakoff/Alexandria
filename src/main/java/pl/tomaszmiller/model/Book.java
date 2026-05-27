package pl.tomaszmiller.model;

/**
 * Immutable representation of a library book.
 *
 * @param id     unique identifier (0 for unsaved entities)
 * @param author author's full name
 * @param title  book title
 * @param pages  number of pages
 * @param isbn   ISBN-13 or ISBN-10 string, may be {@code null}
 * @param status current availability status
 */
public record Book(
        long id,
        String author,
        String title,
        int pages,
        String isbn,
        BookStatus status
) {
    /** Convenience constructor for search results that do not carry an id yet. */
    public Book(String author, String title, int pages) {
        this(0L, author, title, pages, null, BookStatus.AVAILABLE);
    }
}
