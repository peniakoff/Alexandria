package pl.tomaszmiller.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookInventory;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;
import pl.tomaszmiller.repository.port.BookRepository;
import pl.tomaszmiller.repository.port.RentalRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private BookRepository bookRepository;

    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        rentalService = new RentalService(rentalRepository, bookRepository);
    }

    @Test
    void borrow_createsActiveRentalAndDecrementsAvailability() throws Exception {
        LocalDate today = LocalDate.now();
        Book book = new Book(20L, "Author", "Title", 200, null,
                BookStatus.AVAILABLE, 0, null, new BookInventory(3, 2, 0, 0, 0));
        Rental saved = new Rental(1L, 10L, 20L, today, today.plusDays(Rental.DEFAULT_LOAN_DAYS), null, RentalStatus.ACTIVE);
        when(bookRepository.findById(20L)).thenReturn(Optional.of(book));
        when(rentalRepository.save(any(Rental.class))).thenReturn(saved);

        Optional<Rental> result = rentalService.borrow(10L, 20L);

        assertTrue(result.isPresent());
        verify(bookRepository).update(argThat(updated -> updated.inventory().availableCopies() == 1));
    }

    @Test
    void borrow_returnsEmptyWhenBookUnavailable() throws Exception {
        Book book = new Book(20L, "Author", "Title", 200, null,
                BookStatus.BORROWED, 0, null, new BookInventory(2, 0, 0, 0, 0));
        when(bookRepository.findById(20L)).thenReturn(Optional.of(book));

        Optional<Rental> result = rentalService.borrow(10L, 20L);

        assertFalse(result.isPresent());
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void returnBook_setsReturnedStatusAndRestoresAvailability() throws Exception {
        LocalDate today = LocalDate.now();
        Rental active = new Rental(1L, 10L, 20L, today.minusDays(5), today.plusDays(9), null, RentalStatus.ACTIVE);
        Book book = new Book(20L, "Author", "Title", 200, null,
                BookStatus.BORROWED, 0, null, new BookInventory(3, 1, 0, 0, 0));
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(active));
        when(bookRepository.findById(20L)).thenReturn(Optional.of(book));

        boolean result = rentalService.returnBook(1L);

        assertTrue(result);
        verify(bookRepository).update(argThat(updated -> updated.inventory().availableCopies() == 2));
        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).update(captor.capture());
        assertEquals(RentalStatus.RETURNED, captor.getValue().status());
        assertEquals(today, captor.getValue().returnDate());
    }

    @Test
    void returnBook_setsReturnedLateStatusWhenOverdue() throws Exception {
        LocalDate today = LocalDate.now();
        Rental overdue = new Rental(2L, 10L, 20L, today.minusDays(20), today.minusDays(6), null, RentalStatus.ACTIVE);
        Book book = new Book(20L, "Author", "Title", 200, null,
                BookStatus.BORROWED, 0, null, new BookInventory(1, 0, 0, 0, 0));
        when(rentalRepository.findById(2L)).thenReturn(Optional.of(overdue));
        when(bookRepository.findById(20L)).thenReturn(Optional.of(book));

        boolean result = rentalService.returnBook(2L);

        assertTrue(result);
        verify(rentalRepository).update(argThat(rental -> rental.status() == RentalStatus.RETURNED_LATE));
    }

    @Test
    void findByUser_returnsRentalsForUser() throws Exception {
        Rental r1 = new Rental(1L, 5L, 1L, LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.ACTIVE);
        Rental r2 = new Rental(2L, 5L, 2L, LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.ACTIVE);
        when(rentalRepository.findByUserId(5L)).thenReturn(Arrays.asList(r1, r2));
        List<Rental> result = rentalService.findByUser(5L);
        assertEquals(2, result.size());
    }
}
