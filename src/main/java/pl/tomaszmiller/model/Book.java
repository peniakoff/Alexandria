package pl.tomaszmiller.model;

/**
 * Immutable representation of a library book.
 *
 * @param id          unique identifier (0 for unsaved entities)
 * @param author      author's full name
 * @param title       book title
 * @param pages       number of pages
 * @param isbn        ISBN-13 or ISBN-10 string, may be {@code null}
 * @param status      current availability status
 * @param publishYear year of publication (0 if unknown)
 * @param publisher   publisher name, may be {@code null}
 */
public record Book(
        long id,
        String author,
        String title,
        int pages,
        String isbn,
        BookStatus status,
        int publishYear,
        String publisher
) {
    /** Convenience constructor for search results that do not carry an id yet. */
    public Book(String author, String title, int pages) {
        this(0L, author, title, pages, null, BookStatus.AVAILABLE, 0, null);
    }

    /** Legacy constructor without publishYear and publisher for backward compatibility. */
    public Book(long id, String author, String title, int pages, String isbn, BookStatus status) {
        this(id, author, title, pages, isbn, status, 0, null);
    }
}
