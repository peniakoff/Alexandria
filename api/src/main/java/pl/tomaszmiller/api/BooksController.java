package pl.tomaszmiller.api;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.service.BookService;

import java.util.List;

@Controller("/api/books")
@Secured(SecurityRule.IS_AUTHENTICATED)
class BooksController {

    private final BookService bookService;

    BooksController(BookService bookService) {
        this.bookService = bookService;
    }

    @Get("/titles")
    List<String> titles() {
        return bookService.getAllTitles();
    }

    @Get
    List<Book> list() {
        return bookService.findAll();
    }

    @Get("/by-title{?title}")
    HttpResponse<Book> byTitle(@QueryValue String title) {
        return bookService.findByTitle(title).map(HttpResponse::ok).orElseGet(HttpResponse::notFound);
    }

    @Get("/{id}")
    HttpResponse<Book> byId(@PathVariable long id) {
        return bookService.findById(id).map(HttpResponse::ok).orElseGet(HttpResponse::notFound);
    }

    @Post
    @Secured("ADMIN")
    HttpResponse<Book> create(@Body Book book) {
        return bookService.addBook(book).map(HttpResponse::created).orElseGet(HttpResponse::badRequest);
    }

    @Put("/{id}")
    @Secured("ADMIN")
    HttpResponse<?> update(@PathVariable long id, @Body Book book) {
        Book toUpdate = new Book(id, book.author(), book.title(), book.pages(), book.isbn(), book.status(),
                book.publishYear(), book.publisher(), book.inventory());
        return bookService.updateBook(toUpdate) ? HttpResponse.noContent() : HttpResponse.notFound();
    }

    @Delete("/{id}")
    @Secured("ADMIN")
    HttpResponse<?> delete(@PathVariable long id) {
        return bookService.deleteBook(id) ? HttpResponse.noContent() : HttpResponse.notFound();
    }
}

