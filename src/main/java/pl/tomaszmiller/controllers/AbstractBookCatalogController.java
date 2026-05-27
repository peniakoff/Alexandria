package pl.tomaszmiller.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.repository.BookRepository;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public abstract class AbstractBookCatalogController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBookCatalogController.class);

    private final BookRepository bookRepository = new BookRepository();

    @FXML
    protected ListView<String> theList;

    @FXML
    protected TextField bookAuthor;

    @FXML
    protected TextField bookTitle;

    @FXML
    protected TextField pages;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshTitles();
        theList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> showBookDetails(newValue));
    }

    private void refreshTitles() {
        try {
            theList.setItems(FXCollections.observableArrayList(bookRepository.loadBookTitles()));
        } catch (SQLException exception) {
            LOGGER.error("Unable to load book titles.", exception);
            Utils.openDialog("Baza książek", "Nie udało się pobrać listy książek z bazy danych.");
        }
    }

    private void showBookDetails(String selectedTitle) {
        if (selectedTitle == null || selectedTitle.isBlank()) {
            clearBookDetails();
            return;
        }

        try {
            Optional<Book> book = bookRepository.findByTitle(selectedTitle);
            if (book.isEmpty()) {
                clearBookDetails();
                Utils.openDialog("Baza książek", "Nie udało się odnaleźć wybranej książki.");
                return;
            }
            Book selectedBook = book.get();
            bookAuthor.setText(selectedBook.author());
            bookTitle.setText(selectedBook.title());
            pages.setText(String.valueOf(selectedBook.pages()));
        } catch (SQLException exception) {
            LOGGER.error("Unable to load book details for title {}.", selectedTitle, exception);
            Utils.openDialog("Baza książek", "Nie udało się pobrać szczegółów książki.");
        }
    }

    private void clearBookDetails() {
        bookAuthor.clear();
        bookTitle.clear();
        pages.clear();
    }
}
