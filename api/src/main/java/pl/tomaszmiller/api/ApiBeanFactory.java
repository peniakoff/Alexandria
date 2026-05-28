package pl.tomaszmiller.api;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import pl.tomaszmiller.database.DatabaseConnector;
import pl.tomaszmiller.database.MySqlConnector;
import pl.tomaszmiller.repository.port.BookRepository;
import pl.tomaszmiller.repository.port.RentalRepository;
import pl.tomaszmiller.repository.port.UserRepository;
import pl.tomaszmiller.repository.sql.SqlBookRepository;
import pl.tomaszmiller.repository.sql.SqlRentalRepository;
import pl.tomaszmiller.repository.sql.SqlUserRepository;
import pl.tomaszmiller.service.AuthService;
import pl.tomaszmiller.service.BookService;
import pl.tomaszmiller.service.RentalService;
import pl.tomaszmiller.service.UserService;

@Factory
class ApiBeanFactory {

    @Singleton
    DatabaseConnector databaseConnector() {
        return MySqlConnector.getInstance();
    }

    @Singleton
    BookRepository bookRepository(DatabaseConnector connector) {
        return new SqlBookRepository(connector);
    }

    @Singleton
    UserRepository userRepository(DatabaseConnector connector) {
        return new SqlUserRepository(connector);
    }

    @Singleton
    RentalRepository rentalRepository(DatabaseConnector connector) {
        return new SqlRentalRepository(connector);
    }

    @Singleton
    BookService bookService(BookRepository bookRepository) {
        return new BookService(bookRepository);
    }

    @Singleton
    UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }

    @Singleton
    RentalService rentalService(RentalRepository rentalRepository, BookRepository bookRepository) {
        return new RentalService(rentalRepository, bookRepository);
    }

    @Singleton
    AuthService authService(UserRepository userRepository) {
        return new AuthService(userRepository);
    }
}

