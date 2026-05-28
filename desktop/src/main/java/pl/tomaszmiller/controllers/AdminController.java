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
import pl.tomaszmiller.auth.DesktopAuthSession;
import pl.tomaszmiller.config.AppConfig;
import pl.tomaszmiller.i18n.I18n;
import pl.tomaszmiller.model.*;
import pl.tomaszmiller.service.*;
import pl.tomaszmiller.session.UserSession;

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
    private final OpenLibraryService openLibraryService = AppConfig.getInstance().getOpenLibraryService();

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
    private TextField inventoryQuantityField;
    @FXML
    private Label inventorySummaryLabel;
    @FXML
    private Label inventoryStatusLabel;
    @FXML
    private TextField searchField;

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
        refreshInventoryDetails(null);
    }

    @FXML
    private void onLogout() {
        authService.logout();
        DesktopAuthSession.clear();
        UserSession.clearCurrentUser();
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
        bookYear.setText(b.publishYear() > 0 ? String.valueOf(b.publishYear()) : "");
        bookPublisher.setText(b.publisher() != null ? b.publisher() : "");
        refreshInventoryDetails(b);
    }

    @FXML
    private void onFetchFromOpenLibrary() {
        String isbn = normalize(bookIsbn.getText());
        if (isbn.isBlank()) {
            Utils.openDialog(I18n.get("dialog.openlibrary"), I18n.get("admin.books.lookup.isbnrequired"));
            return;
        }
        Optional<BookMetadata> metadata = openLibraryService.lookupByIsbn(isbn);
        if (metadata.isEmpty()) {
            Utils.openDialog(I18n.get("dialog.openlibrary"), I18n.get("admin.books.lookup.notfound"));
            return;
        }
        BookMetadata bookMetadata = metadata.get();
        bookAuthor.setText(bookMetadata.author());
        bookTitle.setText(bookMetadata.title());
        pages.setText(bookMetadata.pages() > 0 ? String.valueOf(bookMetadata.pages()) : "");
        bookIsbn.setText(bookMetadata.isbn());
        bookYear.setText(bookMetadata.publishYear() > 0 ? String.valueOf(bookMetadata.publishYear()) : "");
        bookPublisher.setText(bookMetadata.publisher());
        Utils.confirmDialog(I18n.get("dialog.openlibrary"), I18n.get("admin.books.lookup.loaded"));
    }

    @FXML
    private void onSaveBook() {
        BookFormData formData = readBookFormData();
        if (formData == null) {
            return;
        }
        if (selectedBookId > 0) {
            boolean updated = bookService.updateBook(new Book(selectedBookId, formData.author(), formData.title(),
                    formData.pageCount(), formData.isbn(), BookStatus.AVAILABLE,
                    formData.publishYear(), formData.publisher()));
            if (updated) {
                Utils.confirmDialog(I18n.get("dialog.editbook"), I18n.get("admin.books.updated", formData.title()));
                refreshBookList();
                selectBookByTitle(formData.title());
            } else {
                Utils.openDialog(I18n.get("dialog.editbook"), I18n.get("admin.books.updatefailed"));
            }
            return;
        }

        Integer quantity = readQuantity(false);
        if (quantity == null) {
            return;
        }
        Optional<Book> saved = bookService.addBook(new Book(0L, formData.author(), formData.title(), formData.pageCount(),
                formData.isbn(), BookStatus.AVAILABLE, formData.publishYear(), formData.publisher()), quantity);
        if (saved.isPresent()) {
            Utils.confirmDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.added", formData.title()));
            refreshBookList();
            selectBookByTitle(saved.get().title());
        } else {
            Utils.openDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.addfailed"));
        }
    }

    @FXML
    private void onAddCopies() {
        updateInventory(BookOperation.ADD);
    }

    @FXML
    private void onRemoveCopies() {
        updateInventory(BookOperation.REMOVE);
    }

    @FXML
    private void onArchiveCopies() {
        updateInventory(BookOperation.ARCHIVE);
    }

    @FXML
    private void onWithdrawDamagedCopies() {
        updateInventory(BookOperation.WITHDRAW_DAMAGED);
    }

    @FXML
    private void onWithdrawStolenCopies() {
        updateInventory(BookOperation.WITHDRAW_STOLEN);
    }

    @FXML
    private void onClearBookForm() {
        clearBookDetails();
    }

    private void updateInventory(BookOperation operation) {
        if (selectedBookId <= 0) {
            Utils.openDialog(I18n.get("dialog.inventory"), I18n.get("admin.books.selectfirst"));
            return;
        }
        Integer quantity = readQuantity(true);
        if (quantity == null) {
            return;
        }
        BookService.InventoryOperationResult result = switch (operation) {
            case ADD -> bookService.addCopies(selectedBookId, quantity);
            case REMOVE -> bookService.removeCopies(selectedBookId, quantity);
            case ARCHIVE -> bookService.archiveCopies(selectedBookId, quantity);
            case WITHDRAW_DAMAGED -> bookService.withdrawCopies(selectedBookId, quantity, InventoryRemovalReason.DAMAGED);
            case WITHDRAW_STOLEN -> bookService.withdrawCopies(selectedBookId, quantity, InventoryRemovalReason.STOLEN);
        };
        if (result == BookService.InventoryOperationResult.SUCCESS) {
            String key = switch (operation) {
                case ADD -> "admin.books.inventory.added";
                case REMOVE -> "admin.books.inventory.removed";
                case ARCHIVE -> "admin.books.inventory.archived";
                case WITHDRAW_DAMAGED -> "admin.books.inventory.damaged";
                case WITHDRAW_STOLEN -> "admin.books.inventory.stolen";
            };
            Utils.confirmDialog(I18n.get("dialog.inventory"), I18n.get(key, quantity));
            refreshBookList();
            bookService.findById(selectedBookId).ifPresent(this::fillBookDetails);
        } else {
            String errorKey = result == BookService.InventoryOperationResult.INVALID_QUANTITY
                    ? "admin.books.quantity.invalid"
                    : result == BookService.InventoryOperationResult.NOT_ENOUGH_AVAILABLE_COPIES
                    ? "admin.books.inventory.insufficient"
                    : "admin.books.updatefailed";
            Utils.openDialog(I18n.get("dialog.inventory"), I18n.get(errorKey));
        }
    }

    @FXML
    private void onSearchBook() {
        String query = normalize(searchField.getText()).toLowerCase();
        if (query.isEmpty()) {
            refreshBookList();
            return;
        }
        List<String> filtered = bookService.findAll().stream()
                .filter(b -> b.title().toLowerCase().contains(query)
                        || b.author().toLowerCase().contains(query)
                        || (b.isbn() != null && b.isbn().toLowerCase().contains(query)))
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
        if (inventoryQuantityField != null) {
            inventoryQuantityField.setText("1");
        }
        refreshInventoryDetails(null);
        if (theList != null) {
            theList.getSelectionModel().clearSelection();
        }
    }

    private void fillBookDetails(Book book) {
        selectedBookId = book.id();
        bookAuthor.setText(book.author());
        bookTitle.setText(book.title());
        pages.setText(String.valueOf(book.pages()));
        bookIsbn.setText(book.isbn() != null ? book.isbn() : "");
        bookYear.setText(book.publishYear() > 0 ? String.valueOf(book.publishYear()) : "");
        bookPublisher.setText(book.publisher() != null ? book.publisher() : "");
        refreshInventoryDetails(book);
    }

    private void refreshInventoryDetails(Book book) {
        if (inventorySummaryLabel == null || inventoryStatusLabel == null) {
            return;
        }
        if (book == null) {
            inventoryStatusLabel.setText(I18n.get("admin.books.inventory.empty"));
            inventorySummaryLabel.setText(I18n.get("admin.books.inventory.summary", 0, 0, 0, 0, 0));
            return;
        }
        BookInventory inventory = book.inventory();
        inventoryStatusLabel.setText(I18n.getEnum(book.status()));
        inventorySummaryLabel.setText(I18n.get("admin.books.inventory.summary",
                inventory.availableCopies(),
                inventory.activeCopies(),
                inventory.archivedCopies(),
                inventory.removedDamagedCopies(),
                inventory.removedStolenCopies()));
    }

    private BookFormData readBookFormData() {
        String author = normalize(bookAuthor.getText());
        String title = normalize(bookTitle.getText());
        String pagesStr = normalize(pages.getText());
        String isbn = normalize(bookIsbn.getText());
        String yearStr = normalize(bookYear.getText());
        String publisher = normalize(bookPublisher.getText());

        if (author.isEmpty() || title.isEmpty()) {
            Utils.openDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.fields"));
            return null;
        }
        int pageCount = 0;
        if (!pagesStr.isEmpty()) {
            try {
                pageCount = Integer.parseInt(pagesStr);
            } catch (NumberFormatException e) {
                Utils.openDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.pagesinvalid"));
                return null;
            }
        }
        int publishYear = 0;
        if (!yearStr.isEmpty()) {
            try {
                publishYear = Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                Utils.openDialog(I18n.get("dialog.addbook"), I18n.get("admin.books.yearinvalid"));
                return null;
            }
        }
        return new BookFormData(author, title, pageCount, isbn.isEmpty() ? null : isbn,
                publishYear, publisher.isEmpty() ? null : publisher);
    }

    private Integer readQuantity(boolean allowExisting) {
        if (inventoryQuantityField == null) {
            return 1;
        }
        String value = normalize(inventoryQuantityField.getText());
        if (value.isEmpty() && !allowExisting) {
            Utils.openDialog(I18n.get("dialog.inventory"), I18n.get("admin.books.quantity.required"));
            return null;
        }
        try {
            int quantity = Integer.parseInt(value);
            if (quantity <= 0) {
                throw new NumberFormatException();
            }
            return quantity;
        } catch (NumberFormatException e) {
            Utils.openDialog(I18n.get("dialog.inventory"), I18n.get("admin.books.quantity.invalid"));
            return null;
        }
    }

    private void selectBookByTitle(String title) {
        if (theList == null) {
            return;
        }
        refreshBookList();
        theList.getSelectionModel().select(title);
        onBookSelected(title);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
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
                .map(r -> new RentalRow(r.id(),
                        userNames.getOrDefault(r.userId(), "(id=" + r.userId() + ")"),
                        bookTitles.getOrDefault(r.bookId(), "(id=" + r.bookId() + ")"),
                        r.borrowDate().toString(),
                        r.dueDate().toString(),
                        I18n.getEnum(r.status())))
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
            refreshBookList();
            if (selectedBookId > 0) {
                bookService.findById(selectedBookId).ifPresent(this::fillBookDetails);
            }
            selectedRentalId = 0L;
        } else {
            Utils.openDialog(I18n.get("dialog.bookreturn"), I18n.get("admin.rentals.returnfailed"));
        }
    }

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
                .map(res -> new ReservationRow(res.id(),
                        userNames.getOrDefault(res.userId(), "(id=" + res.userId() + ")"),
                        bookTitlesMap.getOrDefault(res.bookId(), "(id=" + res.bookId() + ")"),
                        res.requestDate().toString(),
                        I18n.getEnum(res.status())))
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

    private enum BookOperation {
        ADD,
        REMOVE,
        ARCHIVE,
        WITHDRAW_DAMAGED,
        WITHDRAW_STOLEN
    }

    private record BookFormData(String author, String title, int pageCount, String isbn,
                                int publishYear, String publisher) {
    }

    public record RentalRow(long id, String userName, String bookTitle, String borrowDate, String dueDate,
                            String status) {
    }

    public record ExtRequestRow(long id, String userName, String bookTitle, String requestDate, String status) {
    }

    public record ReservationRow(long id, String userName, String bookTitle, String requestDate, String status) {
    }
}
