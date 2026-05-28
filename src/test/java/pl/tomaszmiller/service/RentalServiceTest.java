package pl.tomaszmiller.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;
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

    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        rentalService = new RentalService(rentalRepository);
    }

    @Test
    void borrow_createsActiveRental() throws Exception {
        LocalDate today = LocalDate.now();
        Rental saved = new Rental(1L, 10L, 20L, today, today.plusDays(Rental.DEFAULT_LOAN_DAYS), null, RentalStatus.ACTIVE);
        when(rentalRepository.save(any(Rental.class))).thenReturn(saved);

        Optional<Rental> result = rentalService.borrow(10L, 20L);
        assertTrue(result.isPresent());
        assertEquals(RentalStatus.ACTIVE, result.get().status());
        assertNull(result.get().returnDate());

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().userId());
        assertEquals(20L, captor.getValue().bookId());
        assertEquals(today, captor.getValue().borrowDate());
        assertEquals(today.plusDays(Rental.DEFAULT_LOAN_DAYS), captor.getValue().dueDate());
    }

    @Test
    void borrow_returnsEmptyOnException() throws Exception {
        when(rentalRepository.save(any())).thenThrow(new RuntimeException("DB error"));
        Optional<Rental> result = rentalService.borrow(1L, 2L);
        assertFalse(result.isPresent());
    }

    @Test
    void returnBook_setsReturnedStatus() throws Exception {
        LocalDate today = LocalDate.now();
        Rental active = new Rental(1L, 10L, 20L, today.minusDays(5), today.plusDays(9), null, RentalStatus.ACTIVE);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(active));
        doNothing().when(rentalRepository).update(any(Rental.class));

        boolean result = rentalService.returnBook(1L);
        assertTrue(result);

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).update(captor.capture());
        assertEquals(RentalStatus.RETURNED, captor.getValue().status());
        assertEquals(today, captor.getValue().returnDate());
    }

    @Test
    void returnBook_setsReturnedLateStatusWhenOverdue() throws Exception {
        LocalDate today = LocalDate.now();
        Rental overdue = new Rental(2L, 10L, 20L, today.minusDays(20), today.minusDays(6), null, RentalStatus.ACTIVE);
        when(rentalRepository.findById(2L)).thenReturn(Optional.of(overdue));
        doNothing().when(rentalRepository).update(any(Rental.class));

        boolean result = rentalService.returnBook(2L);
        assertTrue(result);
        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).update(captor.capture());
        assertEquals(RentalStatus.RETURNED_LATE, captor.getValue().status());
    }

    @Test
    void returnBook_returnsFalseWhenNotFound() throws Exception {
        when(rentalRepository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(rentalService.returnBook(99L));
    }

    @Test
    void findByUser_returnsRentalsForUser() throws Exception {
        Rental r1 = new Rental(1L, 5L, 1L, LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.ACTIVE);
        Rental r2 = new Rental(2L, 5L, 2L, LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.ACTIVE);
        when(rentalRepository.findByUserId(5L)).thenReturn(Arrays.asList(r1, r2));
        List<Rental> result = rentalService.findByUser(5L);
        assertEquals(2, result.size());
    }

    @Test
    void findAll_returnsAllRentals() throws Exception {
        when(rentalRepository.findAll()).thenReturn(List.of(
                new Rental(1L, 1L, 1L, LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.ACTIVE)
        ));
        assertEquals(1, rentalService.findAll().size());
    }

    @Test
    void findAll_returnsEmptyOnException() throws Exception {
        when(rentalRepository.findAll()).thenThrow(new RuntimeException("error"));
        assertTrue(rentalService.findAll().isEmpty());
    }
}
