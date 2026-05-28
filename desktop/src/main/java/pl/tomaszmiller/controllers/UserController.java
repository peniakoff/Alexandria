package pl.tomaszmiller.controllers;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
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
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.service.*;
import pl.tomaszmiller.session.UserSession;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the regular-user dashboard.
 * Provides book borrowing, personal rental history and profile settings.
 */
public class UserController implements Initializable {

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
    private TextField bookStatus;
    @FXML
    private Label bookInventoryLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Button borrowBtn;
    @FXML
    private Button reserveBtn;
    @FXML
    private javafx.scene.control.CheckBox showUnavailableCheck;
    @FXML
    private ComboBox<Integer> pageSizeCombo;
    @FXML
    private Label pageInfoLabel;
    @FXML
    private Button prevPageBtn;
    @FXML
    private Button nextPageBtn;

    private PauseTransition searchDebounce;
    private int currentPage = 0;
    private int pageSize = 20;
    private List<Book> allFilteredBooks = List.of();

    @FXML
    private TableView<RentalRow> myRentalsTable;
    @FXML
    private TableColumn<RentalRow, Long> myRentalIdCol;
    @FXML
    private TableColumn<RentalRow, String> myRentalBookCol;
    @FXML
    private TableColumn<RentalRow, String> myRentalBorrowCol;
    @FXML
    private TableColumn<RentalRow, String> myRentalDueCol;
    @FXML
    private TableColumn<RentalRow, String> myRentalStatusCol;
    @FXML
    private TableColumn<RentalRow, Void> myRentalActionCol;

    @FXML
    private TextField settingsFirstName;
    @FXML
    private TextField settingsLastName;
    @FXML
    private TextField settingsPhone;
    @FXML
    private PasswordField settingsNewPassword;
    @FXML
    private PasswordField settingsConfirmPassword;
    @FXML
    private Button saveSettingsBtn;

    private long selectedBookId = 0L;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshBookList();
        if (theList != null) {
            theList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, newVal) -> onBookSelected(newVal));
        }
        setupMyRentalsTable();
        loadUserSettings();
        if (welcomeLabel != null) {
            User user = UserSession.getCurrentUser();
            if (user != null) {
                welcomeLabel.setText(I18n.get("nav.welcome", user.fullName()));
            }
        }
        setupSearchDebounce();
        setupPagination();
    }

    private void refreshBookList() {
        currentPage = 0;
        if (searchField != null) {
            performSearch();
        } else {
            boolean showUnavailable = showUnavailableCheck != null && showUnavailableCheck.isSelected();
            List<Book> all = bookService.findAll();
            if (!showUnavailable) {
                all = all.stream()
                        .filter(Book::isVisibleInCatalog)
                        .toList();
            }
            allFilteredBooks = all;
            updatePagedList();
        }
    }

    private void setupSearchDebounce() {
        if (searchField == null) {
            return;
        }
        searchDebounce = new PauseTransition(Duration.millis(500));
        searchDebounce.setOnFinished(e -> performSearch());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.playFromStart();
        });
    }

    private void setupPagination() {
        if (pageSizeCombo != null) {
            pageSizeCombo.setItems(FXCollections.observableArrayList(20, 50, 100));
            pageSizeCombo.setValue(20);
            pageSizeCombo.setOnAction(e -> {
                pageSize = pageSizeCombo.getValue();
                currentPage = 0;
                updatePagedList();
            });
        }
    }

    private void performSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        boolean showUnavailable = showUnavailableCheck != null && showUnavailableCheck.isSelected();
        List<Book> all = bookService.findAll();

        if (!showUnavailable) {
            all = all.stream()
                    .filter(Book::isVisibleInCatalog)
                    .toList();
        }

        if (query.isEmpty()) {
            allFilteredBooks = all;
        } else {
            String normalizedQuery = query.toLowerCase();
            allFilteredBooks = all.stream()
                    .filter(b -> b.title().toLowerCase().contains(normalizedQuery)
                            || b.author().toLowerCase().contains(normalizedQuery))
                    .sorted((a, c) -> a.title().compareToIgnoreCase(c.title()))
                    .toList();
        }
        currentPage = 0;
        updatePagedList();
    }

    @FXML
    private void onToggleShowUnavailable() {
        performSearch();
    }

    private void updatePagedList() {
        if (theList == null) {
            return;
        }
        int totalItems = allFilteredBooks.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<String> pageItems = allFilteredBooks.subList(fromIndex, toIndex).stream()
                .map(Book::title)
                .toList();
        theList.setItems(FXCollections.observableArrayList(pageItems));
        if (pageInfoLabel != null) {
            pageInfoLabel.setText(I18n.get("page.info", currentPage + 1, totalPages, totalItems));
        }
        if (prevPageBtn != null) {
            prevPageBtn.setDisable(currentPage <= 0);
        }
        if (nextPageBtn != null) {
            nextPageBtn.setDisable(currentPage >= totalPages - 1);
        }
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePagedList();
        }
    }

    @FXML
    private void onNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allFilteredBooks.size() / pageSize));
        if (currentPage < totalPages - 1) {
            currentPage++;
            updatePagedList();
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
        if (bookIsbn != null) {
            bookIsbn.setText(b.isbn() != null ? b.isbn() : "");
        }
        if (bookYear != null) {
            bookYear.setText(b.publishYear() > 0 ? String.valueOf(b.publishYear()) : "");
        }
        if (bookPublisher != null) {
            bookPublisher.setText(b.publisher() != null ? b.publisher() : "");
        }
        if (bookStatus != null) {
            bookStatus.setText(I18n.getEnum(b.status()));
        }
        if (bookInventoryLabel != null) {
            bookInventoryLabel.setText(I18n.get("book.inventory.user",
                    b.inventory().availableCopies(), b.inventory().activeCopies()));
        }
        // Show/hide borrow vs reserve based on availability
        if (borrowBtn != null) {
            borrowBtn.setVisible(b.isBorrowable());
            borrowBtn.setManaged(b.isBorrowable());
        }
        if (reserveBtn != null) {
            reserveBtn.setVisible(b.isVisibleInCatalog() && !b.isBorrowable());
            reserveBtn.setManaged(b.isVisibleInCatalog() && !b.isBorrowable());
        }
    }

    @FXML
    private void onSearchBook() {
        performSearch();
    }

    @FXML
    private void onBorrowBook() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            Utils.openDialog(I18n.get("dialog.borrowbook"), I18n.get("borrow.notloggedin"));
            return;
        }
        if (selectedBookId <= 0) {
            Utils.openDialog(I18n.get("dialog.borrowbook"), I18n.get("borrow.selectfirst"));
            return;
        }
        // Check if book is available
        Optional<Book> bookOpt = bookService.findById(selectedBookId);
        if (bookOpt.isPresent() && !bookOpt.get().isBorrowable()) {
            Utils.openDialog(I18n.get("dialog.borrowbook"), I18n.get("borrow.unavailable"));
            return;
        }
        Optional<Rental> rental = rentalService.borrow(currentUser.id(), selectedBookId);
        if (rental.isPresent()) {
            Utils.confirmDialog(I18n.get("dialog.borrowbook"),
                    I18n.get("borrow.success", rental.get().dueDate()));
            refreshMyRentals();
        } else {
            Utils.openDialog(I18n.get("dialog.borrowbook"), I18n.get("borrow.failed"));
        }
    }

    @FXML
    private void onReserveBook() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            Utils.openDialog(I18n.get("dialog.reservebook"), I18n.get("reserve.notloggedin"));
            return;
        }
        if (selectedBookId <= 0) {
            Utils.openDialog(I18n.get("dialog.reservebook"), I18n.get("reserve.selectfirst"));
            return;
        }
        if (reservationService == null) {
            Utils.openDialog(I18n.get("dialog.reservebook"), I18n.get("reserve.unavailable"));
            return;
        }
        var result = reservationService.reserve(currentUser.id(), selectedBookId);
        if (result.isPresent()) {
            Utils.confirmDialog(I18n.get("dialog.reservebook"), I18n.get("reserve.success"));
        } else {
            Utils.openDialog(I18n.get("dialog.reservebook"), I18n.get("reserve.failed"));
        }
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
        if (bookStatus != null) {
            bookStatus.clear();
        }
        if (bookInventoryLabel != null) {
            bookInventoryLabel.setText(I18n.get("book.inventory.user", 0, 0));
        }
    }

    private void setupMyRentalsTable() {
        if (myRentalsTable == null) {
            return;
        }
        myRentalIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        myRentalBookCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        myRentalBorrowCol.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        myRentalDueCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        myRentalStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Color-coded status cell
        myRentalStatusCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) {
                    setText(item);
                    setStyle("");
                    return;
                }
                setText(item);
                RentalRow row = getTableView().getItems().get(idx);
                switch (row.colorCode()) {
                    case "GREEN" -> setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                    case "YELLOW" -> setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                    case "RED" -> setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24;");
                    default -> setStyle("");
                }
            }
        });

        // Action column with "Request Extension" button
        if (myRentalActionCol != null) {
            myRentalActionCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final Button extBtn = new Button(I18n.get("extension.button"));

                {
                    extBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-padding: 2 8; -fx-font-size: 11px;");
                    extBtn.setOnAction(e -> {
                        int idx = getIndex();
                        if (idx < 0 || idx >= getTableView().getItems().size()) return;
                        RentalRow row = getTableView().getItems().get(idx);
                        onRequestExtension(row);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        return;
                    }
                    int idx = getIndex();
                    if (idx < 0 || idx >= getTableView().getItems().size()) {
                        setGraphic(null);
                        return;
                    }
                    RentalRow row = getTableView().getItems().get(idx);
                    if ("YELLOW".equals(row.colorCode())) {
                        setGraphic(extBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            });
        }

        refreshMyRentals();
    }

    private void onRequestExtension(RentalRow row) {
        if (extensionRequestService == null) {
            Utils.openDialog(I18n.get("dialog.extension"), I18n.get("extension.unavailable"));
            return;
        }
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        var result = extensionRequestService.requestExtension(row.rentalId(), currentUser.id());
        if (result.isPresent()) {
            Utils.confirmDialog(I18n.get("dialog.extensionrequest"), I18n.get("extension.success"));
        } else {
            Utils.openDialog(I18n.get("dialog.extensionrequest"), I18n.get("extension.failed"));
        }
    }

    private void refreshMyRentals() {
        if (myRentalsTable == null) {
            return;
        }
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            myRentalsTable.setItems(FXCollections.observableArrayList());
            return;
        }
        Map<Long, String> bookTitles = bookService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Book::id, Book::title));
        List<RentalRow> rows = rentalService.findByUser(currentUser.id()).stream()
                .map(r -> {
                    String bookTitleStr = bookTitles.getOrDefault(r.bookId(), "(id=" + r.bookId() + ")");
                    String colorCode = computeColorCode(r);
                    return new RentalRow(r.id(), r.id(), bookTitleStr,
                            r.borrowDate().toString(), r.dueDate().toString(), I18n.getEnum(r.status()), colorCode);
                })
                .toList();
        myRentalsTable.setItems(FXCollections.observableArrayList(rows));
    }

    private String computeColorCode(Rental r) {
        if (r.status() == pl.tomaszmiller.model.RentalStatus.OVERDUE
                || r.status() == pl.tomaszmiller.model.RentalStatus.RETURNED_LATE
                || (r.isActive() && r.isOverdue())) {
            return "RED";
        }
        if (r.isActive()) {
            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), r.dueDate());
            if (daysUntilDue <= 3) {
                return "YELLOW";
            }
            return "GREEN";
        }
        return "";
    }

    private void loadUserSettings() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null || settingsFirstName == null) {
            return;
        }
        settingsFirstName.setText(currentUser.firstName());
        settingsLastName.setText(currentUser.lastName());
        settingsPhone.setText(currentUser.phoneNumber());
    }

    @FXML
    private void onSaveSettings() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String firstName = settingsFirstName.getText() == null ? "" : settingsFirstName.getText().trim();
        String lastName = settingsLastName.getText() == null ? "" : settingsLastName.getText().trim();
        String phone = settingsPhone.getText() == null ? "" : settingsPhone.getText().trim();

        if (!Utils.isValidName(firstName) || !Utils.isValidName(lastName)) {
            Utils.openDialog(I18n.get("dialog.settings"), I18n.get("settings.namereq"));
            return;
        }
        if (!phone.isEmpty() && !Utils.isValidPhoneNumber(phone)) {
            Utils.openDialog(I18n.get("dialog.settings"), I18n.get("settings.phoneinvalid"));
            return;
        }

        String newPass = settingsNewPassword != null ? settingsNewPassword.getText() : "";
        String confirmPass = settingsConfirmPassword != null ? settingsConfirmPassword.getText() : "";
        if (newPass != null && !newPass.isBlank()) {
            if (newPass.length() < 8) {
                Utils.openDialog(I18n.get("dialog.changepassword"), I18n.get("settings.passshort"));
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Utils.openDialog(I18n.get("dialog.changepassword"), I18n.get("settings.passmatch"));
                return;
            }
        }

        User updated = new User(currentUser.id(), firstName, lastName,
                currentUser.email(), phone, currentUser.role());
        boolean saved = userService.updateProfile(updated);
        if (saved) {
            UserSession.setCurrentUser(updated);
            if (welcomeLabel != null) {
                welcomeLabel.setText(I18n.get("nav.welcome", updated.fullName()));
            }
        } else {
            Utils.openDialog(I18n.get("dialog.settings"), I18n.get("settings.savefailed"));
            return;
        }

        if (newPass != null && !newPass.isBlank()) {
            boolean changed = userService.changePassword(currentUser.id(), newPass);
            if (changed) {
                settingsNewPassword.clear();
                settingsConfirmPassword.clear();
                Utils.confirmDialog(I18n.get("dialog.settings"), I18n.get("settings.passupdated"));
            } else {
                Utils.openDialog(I18n.get("dialog.changepassword"), I18n.get("settings.passfailed"));
            }
            return;
        }
        Utils.confirmDialog(I18n.get("dialog.settings"), I18n.get("settings.updated"));
    }

    /**
     * DTO for the personal rentals TableView.
     */
    public record RentalRow(long id, long rentalId, String bookTitle, String borrowDate, String dueDate, String status,
                            String colorCode) {
    }
}
