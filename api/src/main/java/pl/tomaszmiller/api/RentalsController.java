package pl.tomaszmiller.api;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;
import pl.tomaszmiller.repository.port.RentalRepository;

import java.util.List;
import java.util.Optional;

@Controller("/api/rentals")
class RentalsController {

    private final RentalRepository rentalRepository;

    RentalsController(RentalRepository rentalRepository) {
        this.rentalRepository = rentalRepository;
    }

    @Get
    List<Rental> list() throws Exception {
        return rentalRepository.findAll();
    }

    @Get("/{id}")
    HttpResponse<Rental> byId(@PathVariable long id) throws Exception {
        Optional<Rental> rental = rentalRepository.findById(id);
        return rental.map(HttpResponse::ok).orElseGet(HttpResponse::notFound);
    }

    @Get("/by-user/{userId}")
    List<Rental> byUserId(@PathVariable long userId) throws Exception {
        return rentalRepository.findByUserId(userId);
    }

    @Get("/by-book/{bookId}")
    List<Rental> byBookId(@PathVariable long bookId) throws Exception {
        return rentalRepository.findByBookId(bookId);
    }

    @Get("/by-status{?status}")
    List<Rental> byStatus(@QueryValue RentalStatus status) throws Exception {
        return rentalRepository.findByStatus(status);
    }

    @Post
    Rental create(@Body Rental rental) throws Exception {
        return rentalRepository.save(rental);
    }

    @Put("/{id}")
    HttpResponse<?> update(@PathVariable long id, @Body Rental rental) throws Exception {
        Rental toUpdate = new Rental(id, rental.userId(), rental.bookId(), rental.borrowDate(), rental.dueDate(),
                rental.returnDate(), rental.status());
        rentalRepository.update(toUpdate);
        return HttpResponse.noContent();
    }
}

