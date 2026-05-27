package pl.tomaszmiller.controllers;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.config.AppConfig;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.service.BookService;
import pl.tomaszmiller.service.RentalService;
import pl.tomaszmiller.service.UserService;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the administrator dashboard.
 * Provides user management, rental overview and book catalog management.
 */
public class AdminController implements Initializable {

    private final BookService bookService = AppConfig.getInstance().getBookService();
    private final UserService userService = AppConfig.getInstance().getUserService();
    private final RentalService rentalService = AppConfig.getInstance().getRentalService();

    @FXML private ListView<String> theList;
    @FXML private TextField bookAuthor;
    @FXML private TextField bookTitle;
    @FXML private TextField pages;
    @FXML private TextField bookIsbn;
    @FXML private TextField searchField;
    @FXML private Button addBookBtn;
    @FXML private Button deleteBookBtn;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Number> userIdCol;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userEmailCol;
    @FXML private TableColumn<User, String> userRoleCol;
    @FXML private Button deleteUserBtn;
    @FXML private Label userCountLabel;

    @FXML private TableView<RentalRow> rentalsTable;
    @FXML private TableColumn<RentalRow, Long> rentalIdCol;
    @FXML private TableColumn<RentalRow, String> rentalUserCol;
    @FXML private TableColumn<RentalRow, String> rentalBookCol;
    @FXML private TableColumn<RentalRow, String> rentalBorrowCol;
    @FXML private TableColumn<RentalRow, String> rentalDueCol;
    @FXML private TableColumn<RentalRow, String> rentalStatusCol;
    @FXML private Button returnBookBtn;

    private long selectedBookId = 0L;
    private long selectedRentalId = 0L;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshBookList();
        setupUsersTable();
        setupRentalsTable();
        if (theList != null) {
            theList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, newVal) -> onBookSelected(newVal));
        }
    }

    private void refreshBookList() {
        if (theList != null) {
            theList.setItems(FXCollections.observableArrayList(bookService.getAllTitles()));
        }
    }

    private void onBookSelected(String selectedTitle) {
        if (selectedTitle == null || selectedTitle.isBlank()) {
            clearBookDetails();
            return;
        }
        Optional<Book> book = bookService.findByTitle(selectedTitle);
        if (book.isEmpty()) {
            clearBookDetails();
            return;
        }
        Book b = book.get();
        selectedBookId = b.id();
        bookAuthor.setText(b.author());
        bookTitle.setText(b.title());
        pages.setText(String.valueOf(b.pages()));
        bookIsbn.setText(b.isbn() != null ? b.isbn() : "");
    }

    @FXML
    private void onAddBook() {
        String author = bookAuthor.getText() == null ? "" : bookAuthor.getText().trim();
        String title = bookTitle.getText() == null ? "" : bookTitle.getText().trim();
        String pagesStr = pages.getText() == null ? "" : pages.getText().trim();
        String isbn = bookIsbn.getText() == null ? null : bookIsbn.getText().trim();

        if (author.isEmpty() || title.isEmpty() || pagesStr.isEmpty()) {
            Utils.openDialog("Dodaj książkę", "Uzupełnij pola: autor, tytuł i liczba stron.");
            return;
        }
        int pageCount;
        try {
            pageCount = Integer.parseInt(pagesStr);
        } catch (NumberFormatException e) {
            Utils.openDialog("Dodaj książkę", "Liczba stron musi być liczbą całkowitą.");
            return;
        }
        Book newBook = new Book(0L, author, title, pageCount, isbn == null || isbn.isEmpty() ? null : isbn, BookStatus.AVAILABLE);
        Optional<Book> saved = bookService.addBook(newBook);
        if (saved.isPresent()) {
            Utils.confirmDialog("Dodaj książkę", "Książka \"" + title + "\" została dodana.");
            refreshBookList();
            clearBookDetails();
        } else {
            Utils.openDialog("Dodaj książkę", "Nie udało się dodać książki.");
        }
    }

    @FXML
    private void onDeleteBook() {
        if (selectedBookId <= 0) {
            Utils.openDialog("Usuń książkę", "Najpierw wybierz książkę z listy.");
            return;
        }
        boolean deleted = bookService.deleteBook(selectedBookId);
        if (deleted) {
            Utils.confirmDialog("Usuń książkę", "Książka została usunięta.");
            refreshBookList();
            clearBookDetails();
            selectedBookId = 0L;
        } else {
            Utils.openDialog("Usuń książkę", "Nie udało się usunąć książki.");
        }
    }

    @FXML
    private void onSearchBook() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            refreshBookList();
            return;
        }
        String normalizedQuery = query.toLowerCase();
        List<String> filtered = bookService.findAll().stream()
                .filter(b -> b.title().toLowerCase().contains(normalizedQuery)
                        || b.author().toLowerCase().contains(normalizedQuery))
                .map(Book::title)
                .sorted()
                .toList();
        theList.setItems(FXCollections.observableArrayList(filtered));
    }

    private void clearBookDetails() {
        selectedBookId = 0L;
        if (bookAuthor != null) {
            bookAuthor.clear();
        }
        if (bookTitle != null) {
            bookTitle.clear();
        }
        if (pages != null) {
            pages.clear();
        }
        if (bookIsbn != null) {
            bookIsbn.clear();
        }
    }

    private void setupUsersTable() {
        if (usersTable == null) {
            return;
        }
        userIdCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().id()));
        userNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullName()));
        userEmailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email()));
        userRoleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().role().name()));
        refreshUsers();
    }

    private void refreshUsers() {
        if (usersTable == null) {
            return;
        }
        List<User> users = userService.findAll();
        ObservableList<User> data = FXCollections.observableArrayList(users);
        usersTable.setItems(data);
        if (userCountLabel != null) {
            userCountLabel.setText("Użytkownicy: " + users.size());
        }
    }

    @FXML
    private void onDeleteUser() {
        if (usersTable == null) {
            return;
        }
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.openDialog("Usuń użytkownika", "Wybierz użytkownika z listy.");
            return;
        }
        boolean deleted = userService.deleteUser(selected.id());
        if (deleted) {
            Utils.confirmDialog("Usuń użytkownika", "Użytkownik " + selected.fullName() + " został usunięty.");
            refreshUsers();
        } else {
            Utils.openDialog("Usuń użytkownika", "Nie udało się usunąć użytkownika.");
        }
    }

    private void setupRentalsTable() {
        if (rentalsTable == null) {
            return;
        }
        rentalIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        rentalUserCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        rentalBookCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        rentalBorrowCol.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        rentalDueCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        rentalStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        refreshRentals();
        rentalsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> selectedRentalId = selected != null ? selected.getId() : 0L);
    }

    private void refreshRentals() {
        if (rentalsTable == null) {
            return;
        }
        List<RentalRow> rows = rentalService.findAll().stream()
                .map(r -> {
                    String userName = userService.findById(r.userId())
                            .map(User::fullName).orElse("(id=" + r.userId() + ")");
                    String bookTitleStr = bookService.findById(r.bookId())
                            .map(Book::title).orElse("(id=" + r.bookId() + ")");
                    return new RentalRow(r.id(), userName, bookTitleStr,
                            r.borrowDate().toString(), r.dueDate().toString(), r.status().name());
                })
                .toList();
        rentalsTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void onReturnBook() {
        if (selectedRentalId <= 0) {
            Utils.openDialog("Zwrot książki", "Wybierz wypożyczenie z tabeli.");
            return;
        }
        boolean done = rentalService.returnBook(selectedRentalId);
        if (done) {
            Utils.confirmDialog("Zwrot książki", "Zwrot zarejestrowany pomyślnie.");
            refreshRentals();
            selectedRentalId = 0L;
        } else {
            Utils.openDialog("Zwrot książki", "Nie udało się zarejestrować zwrotu.");
        }
    }

    /**
     * Simple DTO for displaying rental data in a TableView.
     * PropertyValueFactory requires public getters with JavaFX naming.
     */
    public static final class RentalRow {
        private final long id;
        private final String userName;
        private final String bookTitle;
        private final String borrowDate;
        private final String dueDate;
        private final String status;

        public RentalRow(long id, String userName, String bookTitle, String borrowDate, String dueDate, String status) {
            this.id = id;
            this.userName = userName;
            this.bookTitle = bookTitle;
            this.borrowDate = borrowDate;
            this.dueDate = dueDate;
            this.status = status;
        }

        public long getId() {
            return id;
        }

        public String getUserName() {
            return userName;
        }

        public String getBookTitle() {
            return bookTitle;
        }

        public String getBorrowDate() {
            return borrowDate;
        }

        public String getDueDate() {
            return dueDate;
        }

        public String getStatus() {
            return status;
        }
    }
}
