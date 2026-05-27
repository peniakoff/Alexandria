package pl.tomaszmiller.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.repository.port.BookRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        assertEquals(2, titles.size());
        verify(bookRepository, times(1)).loadBookTitles();
    }

    @Test
    void getAllTitles_returnsEmptyListOnException() throws Exception {
        when(bookRepository.loadBookTitles()).thenThrow(new RuntimeException("DB down"));
        List<String> titles = bookService.getAllTitles();
        assertTrue(titles.isEmpty());
    }

    @Test
    void findByTitle_returnsPresentOptional() throws Exception {
        Book book = new Book(1L, "Henryk Sienkiewicz", "Ogniem i Mieczem", 700, null, BookStatus.AVAILABLE);
        when(bookRepository.findByTitle("Ogniem i Mieczem")).thenReturn(Optional.of(book));
        Optional<Book> result = bookService.findByTitle("Ogniem i Mieczem");
        assertTrue(result.isPresent());
        assertEquals("Ogniem i Mieczem", result.get().title());
    }

    @Test
    void findByTitle_returnsEmptyOnException() throws Exception {
        when(bookRepository.findByTitle(anyString())).thenThrow(new RuntimeException("error"));
        Optional<Book> result = bookService.findByTitle("anything");
        assertFalse(result.isPresent());
    }

    @Test
    void addBook_savesAndReturnsBook() throws Exception {
        Book input  = new Book(0L, "Adam Mickiewicz", "Pan Tadeusz", 328, "978-83-7081-941-0", BookStatus.AVAILABLE);
        Book saved  = new Book(5L, "Adam Mickiewicz", "Pan Tadeusz", 328, "978-83-7081-941-0", BookStatus.AVAILABLE);
        when(bookRepository.save(input)).thenReturn(saved);
        Optional<Book> result = bookService.addBook(input);
        assertTrue(result.isPresent());
        assertEquals(5L, result.get().id());
    }

    @Test
    void addBook_returnsEmptyOnException() throws Exception {
        Book input = new Book(0L, "Author", "Title", 100, null, BookStatus.AVAILABLE);
        when(bookRepository.save(input)).thenThrow(new RuntimeException("error"));
        Optional<Book> result = bookService.addBook(input);
        assertFalse(result.isPresent());
    }

    @Test
    void deleteBook_returnsTrueOnSuccess() throws Exception {
        doNothing().when(bookRepository).deleteById(3L);
        assertTrue(bookService.deleteBook(3L));
    }

    @Test
    void deleteBook_returnsFalseOnException() throws Exception {
        doThrow(new RuntimeException("error")).when(bookRepository).deleteById(anyLong());
        assertFalse(bookService.deleteBook(1L));
    }

    @Test
    void findAll_returnsAllBooks() throws Exception {
        Book b1 = new Book(1L, "Author1", "Title1", 100, null, BookStatus.AVAILABLE);
        Book b2 = new Book(2L, "Author2", "Title2", 200, null, BookStatus.BORROWED);
        when(bookRepository.findAll()).thenReturn(Arrays.asList(b1, b2));
        List<Book> result = bookService.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void updateBook_returnsTrueOnSuccess() throws Exception {
        Book book = new Book(1L, "Author", "Title", 100, null, BookStatus.AVAILABLE);
        doNothing().when(bookRepository).update(book);
        assertTrue(bookService.updateBook(book));
    }
}
