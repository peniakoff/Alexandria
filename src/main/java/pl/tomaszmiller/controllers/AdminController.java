package pl.tomaszmiller.controllers;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.config.AppConfig;
import pl.tomaszmiller.i18n.I18n;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.service.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
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
    private final AuthService authService = AppConfig.getInstance().getAuthService();
    private final ExtensionRequestService extensionRequestService = AppConfig.getInstance().getExtensionRequestService();
    private final ReservationService reservationService = AppConfig.getInstance().getReservationService();

    @FXML
    private Label welcomeLabel;

    @FXML
    private ListView<String> theList;
    @FXML
    private TextField bookAuthor;
    @FXML
    private TextField bookTitle;
    @FXML
    private TextField pages;
    @FXML
    private TextField bookIsbn;
    @FXML
    private TextField bookYear;
    @FXML
    private TextField bookPublisher;
    @FXML
    private TextField searchField;
    @FXML
    private Button addBookBtn;
    @FXML
    private Button deleteBookBtn;

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, Number> userIdCol;
    @FXML
    private TableColumn<User, String> userNameCol;
    @FXML
    private TableColumn<User, String> userEmailCol;
    @FXML
    private TableColumn<User, String> userRoleCol;
    @FXML
    private Button deleteUserBtn;
    @FXML
    private Label userCountLabel;

    @FXML
    private TableView<RentalRow> rentalsTable;
    @FXML
    private TableColumn<RentalRow, Long> rentalIdCol;
    @FXML
    private TableColumn<RentalRow, String> rentalUserCol;
    @FXML
    private TableColumn<RentalRow, String> rentalBookCol;
    @FXML
    private TableColumn<RentalRow, String> rentalBorrowCol;
    @FXML
    private TableColumn<RentalRow, String> rentalDueCol;
    @FXML
    private TableColumn<RentalRow, String> rentalStatusCol;
    @FXML
    private Button returnBookBtn;

    @FXML
    private TableView<ExtRequestRow> extRequestsTable;
    @FXML
    private TableColumn<ExtRequestRow, Long> extReqIdCol;
    @FXML
    private TableColumn<ExtRequestRow, String> extReqUserCol;
    @FXML
    private TableColumn<ExtRequestRow, String> extReqBookCol;
    @FXML
    private TableColumn<ExtRequestRow, String> extReqDateCol;
    @FXML
    private TableColumn<ExtRequestRow, String> extReqStatusCol;

    @FXML
    private TableView<ReservationRow> reservationsTable;
    @FXML
    private TableColumn<ReservationRow, Long> resIdCol;
    @FXML
    private TableColumn<ReservationRow, String> resUserCol;
    @FXML
    private TableColumn<ReservationRow, String> resBookCol;
    @FXML
    private TableColumn<ReservationRow, String> resDateCol;
    @FXML
    private TableColumn<ReservationRow, String> resStatusCol;

    private long selectedBookId = 0L;
    private long selectedRentalId = 0L;
    private PauseTransition searchDebounce;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshBookList();
        setupUsersTable();
        setupRentalsTable();
        setupExtRequestsTable();
        setupReservationsTable();
        if (theList != null) {
            theList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, newVal) -> onBookSelected(newVal));
        }
        if (searchField != null) {
            searchDebounce = new PauseTransition(Duration.millis(500));
            searchDebounce.setOnFinished(e -> onSearchBook());
            searchField.textProperty().addListener((obs, oldVal, newVal) -> searchDebounce.playFromStart());
        }
        if (welcomeLabel != null) {
            User user = pl.tomaszmiller.session.UserSession.getCurrentUser();
            if (user != null) {
                welcomeLabel.setText(I18n.get("nav.admin", user.fullName()));
            }
        }
    }

    @FXML
    private void onLogout() {
        authService.logout();
        try {
            Parent loginView = Utils.loadView("/pl/tomaszmiller/views/loginView.fxml");
            Stage stage = (Stage) theList.getScene().getWindow();
            stage.setTitle(I18n.get("app.title"));
            stage.setScene(new Scene(loginView, 800, 600));
            stage.show();
        } catch (IOException e) {
            Utils.openDialog(I18n.get("dialog.logout"), I18n.get("logout.failed"));
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
        if (bookYear != null) {
            bookYear.setText(b.publishYear() > 0 ? String.valueOf(b.publishYear()) : "");
        }
        if (bookPublisher != null) {
            bookPublisher.setText(b.publisher() != null ? b.publisher() : "");
        }
    }

    @FXML
    private void onAddBook() {
        String author = bookAuthor.getText() == null ? "" : bookAuthor.getText().trim();
        String title = bookTitle.getText() == null ? "" : bookTitle.getText().trim();
        String pagesStr = pages.getText() == null ? "" : pages.getText().trim();
        String isbn = bookIsbn.getText() == null ? null : bookIsbn.getText().trim();
        String yearStr = bookYear != null && bookYear.getText() != null ? bookYear.getText().trim() : "";
        String publisher = bookPublisher != null && bookPublisher.getText() != null ? bookPublisher.getText().trim() : "";

        if (author.isEmpty() || title.isEmpty() || pagesStr.isEmpty()) {
            Utils.openDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.fields"));
            return;
        }
        int pageCount;
        try {
            pageCount = Integer.parseInt(pagesStr);
        } catch (NumberFormatException e) {
            Utils.openDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.pagesinvalid"));
            return;
        }
        int publishYear = 0;
        if (!yearStr.isEmpty()) {
            try {
                publishYear = Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                Utils.openDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.yearinvalid"));
                return;
            }
        }

        if (selectedBookId > 0) {
            Book updatedBook = new Book(selectedBookId, author, title, pageCount,
                    isbn == null || isbn.isEmpty() ? null : isbn, BookStatus.AVAILABLE,
                    publishYear, publisher.isEmpty() ? null : publisher);
            boolean updated = bookService.updateBook(updatedBook);
            if (updated) {
                Utils.confirmDialog(I18n.get("dialog.editbook"), I18n.get("admin.books.updated", title));
                refreshBookList();
                clearBookDetails();
            } else {
                Utils.openDialog(I18n.get("dialog.editbook"), I18n.get("admin.books.updatefailed"));
            }
        } else {
            Book newBook = new Book(0L, author, title, pageCount,
                    isbn == null || isbn.isEmpty() ? null : isbn, BookStatus.AVAILABLE,
                    publishYear, publisher.isEmpty() ? null : publisher);
            Optional<Book> saved = bookService.addBook(newBook);
            if (saved.isPresent()) {
                Utils.confirmDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.added", title));
                refreshBookList();
                clearBookDetails();
            } else {
                Utils.openDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.addfailed"));
            }
        }
    }

    @FXML
    private void onDeleteBook() {
        if (selectedBookId <= 0) {
            Utils.openDialog(I18n.get("dialog.removebook"), I18n.get("admin.books.selectfirst"));
            return;
        }
        boolean deleted = bookService.deleteBook(selectedBookId);
        if (deleted) {
            Utils.confirmDialog(I18n.get("dialog.removebook"), I18n.get("admin.books.removed"));
            refreshBookList();
            clearBookDetails();
            selectedBookId = 0L;
        } else {
            Utils.openDialog(I18n.get("dialog.removebook"), I18n.get("admin.books.removefailed"));
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
        if (bookYear != null) {
            bookYear.clear();
        }
        if (bookPublisher != null) {
            bookPublisher.clear();
        }
    }

    private void setupUsersTable() {
        if (usersTable == null) {
            return;
        }
        userIdCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().id()));
        userNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullName()));
        userEmailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email()));
        userRoleCol.setCellValueFactory(data -> new SimpleStringProperty(I18n.getEnum(data.getValue().role())));
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
            userCountLabel.setText(I18n.get("admin.users", users.size()));
        }
    }

    @FXML
    private void onDeleteUser() {
        if (usersTable == null) {
            return;
        }
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.openDialog(I18n.get("dialog.removeuser"), I18n.get("admin.users.selectfirst"));
            return;
        }
        boolean deleted = userService.deleteUser(selected.id());
        if (deleted) {
            Utils.confirmDialog(I18n.get("dialog.removeuser"), I18n.get("admin.users.removed", selected.fullName()));
            refreshUsers();
        } else {
            Utils.openDialog(I18n.get("dialog.removeuser"), I18n.get("admin.users.removefailed"));
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
                .addListener((obs, old, selected) -> selectedRentalId = selected != null ? selected.id() : 0L);
    }

    private void refreshRentals() {
        if (rentalsTable == null) {
            return;
        }
        Map<Long, String> userNames = userService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(User::id, User::fullName));
        Map<Long, String> bookTitles = bookService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Book::id, Book::title));
        List<RentalRow> rows = rentalService.findAll().stream()
                .map(r -> {
                    String userName = userNames.getOrDefault(r.userId(), "(id=" + r.userId() + ")");
                    String bookTitleStr = bookTitles.getOrDefault(r.bookId(), "(id=" + r.bookId() + ")");
                    return new RentalRow(r.id(), userName, bookTitleStr,
                            r.borrowDate().toString(), r.dueDate().toString(), I18n.getEnum(r.status()));
                })
                .toList();
        rentalsTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void onReturnBook() {
        if (selectedRentalId <= 0) {
            Utils.openDialog(I18n.get("dialog.bookreturn"), I18n.get("admin.rentals.selectfirst"));
            return;
        }
        boolean done = rentalService.returnBook(selectedRentalId);
        if (done) {
            Utils.confirmDialog(I18n.get("dialog.bookreturn"), I18n.get("admin.rentals.returned"));
            refreshRentals();
            selectedRentalId = 0L;
        } else {
            Utils.openDialog(I18n.get("dialog.bookreturn"), I18n.get("admin.rentals.returnfailed"));
        }
    }

    // --- Extension Requests ---

    private void setupExtRequestsTable() {
        if (extRequestsTable == null) {
            return;
        }
        extReqIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        extReqUserCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        extReqBookCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        extReqDateCol.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        extReqStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        refreshExtRequests();
    }

    private void refreshExtRequests() {
        if (extRequestsTable == null || extensionRequestService == null) {
            return;
        }
        Map<Long, String> userNames = userService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(User::id, User::fullName));
        Map<Long, String> bookTitles = bookService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Book::id, Book::title));
        Map<Long, Rental> rentalsById = rentalService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Rental::id, r -> r));

        List<ExtRequestRow> rows = extensionRequestService.findPending().stream()
                .map(req -> {
                    String userName = userNames.getOrDefault(req.userId(), "(id=" + req.userId() + ")");
                    Rental rental = rentalsById.get(req.rentalId());
                    String bookTitle = rental != null
                            ? bookTitles.getOrDefault(rental.bookId(), "(unknown)")
                            : "(unknown)";
                    return new ExtRequestRow(req.id(), userName, bookTitle,
                            req.requestDate().toString(), I18n.getEnum(req.status()));
                })
                .toList();
        extRequestsTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void onApproveExtension() {
        if (extRequestsTable == null || extensionRequestService == null) {
            return;
        }
        ExtRequestRow selected = extRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.openDialog(I18n.get("dialog.extensionrequest"), I18n.get("admin.ext.selectfirst"));
            return;
        }
        ExtensionRequestService.ApprovalResult result = extensionRequestService.approve(selected.id());
        switch (result) {
            case APPROVED -> {
                Utils.confirmDialog(I18n.get("dialog.extensionrequest"), I18n.get("extension.approved"));
                refreshExtRequests();
                refreshRentals();
            }
            case RESERVATION_CONFLICT ->
                    Utils.openDialog(I18n.get("dialog.extensionrequest"), I18n.get("admin.ext.conflict"));
            case REQUEST_NOT_FOUND ->
                    Utils.openDialog(I18n.get("dialog.extensionrequest"), I18n.get("admin.ext.notfound"));
            case RENTAL_NOT_FOUND ->
                    Utils.openDialog(I18n.get("dialog.extensionrequest"), I18n.get("admin.ext.rentalnotfound"));
            default -> Utils.openDialog(I18n.get("dialog.extensionrequest"), I18n.get("admin.ext.unexpected"));
        }
    }

    @FXML
    private void onRejectExtension() {
        if (extRequestsTable == null || extensionRequestService == null) {
            return;
        }
        ExtRequestRow selected = extRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.openDialog(I18n.get("dialog.extensionrequest"), I18n.get("admin.ext.selectfirst"));
            return;
        }
        extensionRequestService.reject(selected.id());
        Utils.confirmDialog(I18n.get("dialog.extensionrequest"), I18n.get("extension.rejected"));
        refreshExtRequests();
    }

    // --- Reservations ---

    private void setupReservationsTable() {
        if (reservationsTable == null) {
            return;
        }
        resIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        resUserCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        resBookCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        resDateCol.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        resStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        refreshReservations();
    }

    private void refreshReservations() {
        if (reservationsTable == null || reservationService == null) {
            return;
        }
        Map<Long, String> userNames = userService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(User::id, User::fullName));
        Map<Long, String> bookTitlesMap = bookService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Book::id, Book::title));

        List<ReservationRow> rows = reservationService.findPending().stream()
                .map(res -> {
                    String userName = userNames.getOrDefault(res.userId(), "(id=" + res.userId() + ")");
                    String bookTitle = bookTitlesMap.getOrDefault(res.bookId(), "(id=" + res.bookId() + ")");
                    return new ReservationRow(res.id(), userName, bookTitle,
                            res.requestDate().toString(), I18n.getEnum(res.status()));
                })
                .toList();
        reservationsTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void onApproveReservation() {
        if (reservationsTable == null || reservationService == null) {
            return;
        }
        ReservationRow selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.openDialog(I18n.get("dialog.reservation"), I18n.get("admin.res.selectfirst"));
            return;
        }
        boolean approved = reservationService.approve(selected.id());
        if (approved) {
            Utils.confirmDialog(I18n.get("dialog.reservation"), I18n.get("admin.res.approved"));
            refreshReservations();
        } else {
            Utils.openDialog(I18n.get("dialog.reservation"), I18n.get("admin.res.approvefailed"));
        }
    }

    @FXML
    private void onRejectReservation() {
        if (reservationsTable == null || reservationService == null) {
            return;
        }
        ReservationRow selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.openDialog(I18n.get("dialog.reservation"), I18n.get("admin.res.selectfirst"));
            return;
        }
        reservationService.reject(selected.id());
        Utils.confirmDialog(I18n.get("dialog.reservation"), I18n.get("admin.res.rejected"));
        refreshReservations();
    }

    /**
     * Simple DTO for displaying rental data in a TableView.
     */
    public record RentalRow(long id, String userName, String bookTitle, String borrowDate, String dueDate,
                            String status) {
    }

    /**
     * DTO for extension request table rows.
     */
    public record ExtRequestRow(long id, String userName, String bookTitle, String requestDate, String status) {
    }

    /**
     * DTO for reservation table rows.
     */
    public record ReservationRow(long id, String userName, String bookTitle, String requestDate, String status) {
    }
}
