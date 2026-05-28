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
 * @param inventory   stock information for the title
 */
public record Book(
        long id,
        String author,
        String title,
        int pages,
        String isbn,
        BookStatus status,
        int publishYear,
        String publisher,
        BookInventory inventory
) {
    public Book {
        inventory = inventory != null ? inventory : new BookInventory(1);
        status = inventory.resolveStatus(status != null ? status : BookStatus.AVAILABLE);
    }

    /**
     * Convenience constructor for search results that do not carry an id yet.
     */
    public Book(String author, String title, int pages) {
        this(0L, author, title, pages, null, BookStatus.AVAILABLE, 0, null, new BookInventory(1));
    }

    /**
     * Legacy constructor without publishYear and publisher for backward compatibility.
     */
    public Book(long id, String author, String title, int pages, String isbn, BookStatus status) {
        this(id, author, title, pages, isbn, status, 0, null, new BookInventory(1));
    }

    /**
     * Backward-compatible constructor without explicit inventory.
     */
    public Book(long id, String author, String title, int pages, String isbn, BookStatus status,
                int publishYear, String publisher) {
        this(id, author, title, pages, isbn, status, publishYear, publisher, new BookInventory(1));
    }

    public Book withInventory(BookInventory newInventory) {
        return new Book(id, author, title, pages, isbn, status, publishYear, publisher, newInventory);
    }

    public Book withMetadata(String newAuthor, String newTitle, int newPages, String newIsbn,
                             int newPublishYear, String newPublisher) {
        return new Book(id, newAuthor, newTitle, newPages, newIsbn, status, newPublishYear, newPublisher, inventory);
    }

    public boolean isBorrowable() {
        return inventory.availableCopies() > 0 && inventory.activeCopies() > 0;
    }

    public boolean isVisibleInCatalog() {
        return inventory.activeCopies() > 0;
    }
}
