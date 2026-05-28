package pl.tomaszmiller.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.config.AppConfig;
import pl.tomaszmiller.i18n.I18n;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.service.BookService;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public abstract class AbstractBookCatalogController implements Initializable {

    protected final BookService bookService = AppConfig.getInstance().getBookService();

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
        theList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> showBookDetails(newValue));
    }

    protected void refreshTitles() {
        theList.setItems(FXCollections.observableArrayList(bookService.getAllTitles()));
    }

    protected void showBookDetails(String selectedTitle) {
        if (selectedTitle == null || selectedTitle.isBlank()) {
            clearBookDetails();
            return;
        }
        Optional<Book> book = bookService.findByTitle(selectedTitle);
        if (book.isEmpty()) {
            clearBookDetails();
            Utils.openDialog(I18n.get("dialog.bookcatalog"), I18n.get("book.selected.notfound"));
            return;
        }
        Book selectedBook = book.get();
        bookAuthor.setText(selectedBook.author());
        bookTitle.setText(selectedBook.title());
        pages.setText(String.valueOf(selectedBook.pages()));
    }

    protected void clearBookDetails() {
        bookAuthor.clear();
        bookTitle.clear();
        pages.clear();
    }
}
