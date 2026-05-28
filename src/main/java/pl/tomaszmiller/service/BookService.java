package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookInventory;
import pl.tomaszmiller.model.InventoryRemovalReason;
import pl.tomaszmiller.repository.port.BookRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Application-level operations on the book catalog.
 */
public class BookService {

    public enum InventoryOperationResult {
        SUCCESS,
        BOOK_NOT_FOUND,
        INVALID_QUANTITY,
        NOT_ENOUGH_AVAILABLE_COPIES
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<String> getAllTitles() {
        try {
            List<String> titles = new ArrayList<>(bookRepository.loadBookTitles());
            Collections.sort(titles);
            return titles;
        } catch (Exception e) {
            LOGGER.error("Unable to load book titles.", e);
            return Collections.emptyList();
        }
    }

    public Optional<Book> findByTitle(String title) {
        try {
            return bookRepository.findByTitle(title);
        } catch (Exception e) {
            LOGGER.error("Unable to find book by title '{}'.", title, e);
            return Optional.empty();
        }
    }

    public List<Book> findAll() {
        try {
            return bookRepository.findAll();
        } catch (Exception e) {
            LOGGER.error("Unable to load all books.", e);
            return Collections.emptyList();
        }
    }

    public Optional<Book> findById(long id) {
        try {
            return bookRepository.findById(id);
        } catch (Exception e) {
            LOGGER.error("Unable to find book by id {}.", id, e);
            return Optional.empty();
        }
    }

    public Optional<Book> addBook(Book book) {
        return addBook(book, 1);
    }

    public Optional<Book> addBook(Book book, int copies) {
        if (copies <= 0) {
            return Optional.empty();
        }
        try {
            Book bookToSave = book.withInventory(new BookInventory(copies, copies, 0, 0, 0));
            return Optional.of(bookRepository.save(bookToSave));
        } catch (Exception e) {
            LOGGER.error("Unable to save book '{}'.", book.title(), e);
            return Optional.empty();
        }
    }

    public boolean updateBook(Book book) {
        try {
            Book current = findById(book.id()).orElse(null);
            Book toUpdate = current != null
                    ? new Book(book.id(), book.author(), book.title(), book.pages(), book.isbn(),
                    current.status(), book.publishYear(), book.publisher(), current.inventory())
                    : book;
            bookRepository.update(toUpdate);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to update book id={}.", book.id(), e);
            return false;
        }
    }

    public boolean deleteBook(long id) {
        try {
            bookRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to delete book id={}.", id, e);
            return false;
        }
    }

    public InventoryOperationResult addCopies(long id, int quantity) {
        return updateInventory(id, quantity, InventoryUpdateMode.ADD, null);
    }

    public InventoryOperationResult removeCopies(long id, int quantity) {
        return updateInventory(id, quantity, InventoryUpdateMode.REMOVE, null);
    }

    public InventoryOperationResult archiveCopies(long id, int quantity) {
        return updateInventory(id, quantity, InventoryUpdateMode.ARCHIVE, null);
    }

    public InventoryOperationResult withdrawCopies(long id, int quantity, InventoryRemovalReason reason) {
        return updateInventory(id, quantity, InventoryUpdateMode.WITHDRAW, reason);
    }

    private InventoryOperationResult updateInventory(long id, int quantity, InventoryUpdateMode mode,
                                                     InventoryRemovalReason reason) {
        if (quantity <= 0) {
            return InventoryOperationResult.INVALID_QUANTITY;
        }
        try {
            Optional<Book> currentOpt = bookRepository.findById(id);
            if (currentOpt.isEmpty()) {
                return InventoryOperationResult.BOOK_NOT_FOUND;
            }
            Book current = currentOpt.get();
            BookInventory updatedInventory = switch (mode) {
                case ADD -> current.inventory().addCopies(quantity);
                case REMOVE -> current.inventory().removeCopies(quantity);
                case ARCHIVE -> current.inventory().archiveCopies(quantity);
                case WITHDRAW -> current.inventory().withdrawCopies(quantity, reason);
            };
            bookRepository.update(current.withInventory(updatedInventory));
            return InventoryOperationResult.SUCCESS;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid inventory operation for book id={}.", id, e);
            return InventoryOperationResult.INVALID_QUANTITY;
        } catch (IllegalStateException e) {
            LOGGER.warn("Insufficient available copies for book id={}.", id, e);
            return InventoryOperationResult.NOT_ENOUGH_AVAILABLE_COPIES;
        } catch (Exception e) {
            LOGGER.error("Unable to update inventory for book id={}.", id, e);
            return InventoryOperationResult.BOOK_NOT_FOUND;
        }
    }

    private enum InventoryUpdateMode {
        ADD,
        REMOVE,
        ARCHIVE,
        WITHDRAW
    }
}
