package pl.tomaszmiller.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookInventory;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.model.InventoryRemovalReason;
import pl.tomaszmiller.repository.port.BookRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository);
    }

    @Test
    void getAllTitles_returnsSortedTitles() throws Exception {
        when(bookRepository.loadBookTitles()).thenReturn(Arrays.asList("The Witcher", "Pan Tadeusz"));
        List<String> titles = bookService.getAllTitles();
        assertEquals(List.of("Pan Tadeusz", "The Witcher"), titles);
    }

    @Test
    void addBook_savesWithRequestedCopies() throws Exception {
        Book input = new Book(0L, "Adam Mickiewicz", "Pan Tadeusz", 328,
                "9788370819410", BookStatus.AVAILABLE);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Book> result = bookService.addBook(input, 3);

        assertTrue(result.isPresent());
        assertEquals(3, result.get().inventory().activeCopies());
        assertEquals(3, result.get().inventory().availableCopies());
    }

    @Test
    void addCopies_increasesAvailableAndActiveCopies() throws Exception {
        Book book = sampleBook(new BookInventory(2, 1, 0, 0, 0));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookService.InventoryOperationResult result = bookService.addCopies(1L, 2);

        assertEquals(BookService.InventoryOperationResult.SUCCESS, result);
        verify(bookRepository).update(argThat(updated ->
                updated.inventory().activeCopies() == 4 && updated.inventory().availableCopies() == 3));
    }

    @Test
    void removeCopies_rejectsWhenTooFewAvailableCopies() throws Exception {
        Book book = sampleBook(new BookInventory(3, 1, 0, 0, 0));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookService.InventoryOperationResult result = bookService.removeCopies(1L, 2);

        assertEquals(BookService.InventoryOperationResult.NOT_ENOUGH_AVAILABLE_COPIES, result);
        verify(bookRepository, never()).update(any());
    }

    @Test
    void archiveCopies_movesAvailableCopiesToArchive() throws Exception {
        Book book = sampleBook(new BookInventory(4, 3, 1, 0, 0));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookService.InventoryOperationResult result = bookService.archiveCopies(1L, 2);

        assertEquals(BookService.InventoryOperationResult.SUCCESS, result);
        verify(bookRepository).update(argThat(updated ->
                updated.inventory().activeCopies() == 2
                        && updated.inventory().availableCopies() == 1
                        && updated.inventory().archivedCopies() == 3));
    }

    @Test
    void withdrawCopies_tracksReasonSpecificCounters() throws Exception {
        Book book = sampleBook(new BookInventory(4, 4, 0, 1, 0));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookService.InventoryOperationResult result =
                bookService.withdrawCopies(1L, 2, InventoryRemovalReason.STOLEN);

        assertEquals(BookService.InventoryOperationResult.SUCCESS, result);
        verify(bookRepository).update(argThat(updated ->
                updated.inventory().activeCopies() == 2
                        && updated.inventory().availableCopies() == 2
                        && updated.inventory().removedStolenCopies() == 2));
    }

    @Test
    void updateBook_preservesInventory() throws Exception {
        Book existing = sampleBook(new BookInventory(5, 2, 1, 0, 0));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean updated = bookService.updateBook(new Book(1L, "New Author", "New Title", 120,
                "9780000000000", BookStatus.AVAILABLE, 2024, "Publisher"));

        assertTrue(updated);
        verify(bookRepository).update(argThat(book ->
                book.inventory().activeCopies() == 5
                        && book.author().equals("New Author")
                        && book.title().equals("New Title")));
    }

    private Book sampleBook(BookInventory inventory) {
        return new Book(1L, "Author", "Title", 100, "9780000000000",
                BookStatus.AVAILABLE, 2020, "Publisher", inventory);
    }
}
