package pl.tomaszmiller.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.config.AppConfig;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.service.BookService;
import pl.tomaszmiller.service.RentalService;
import pl.tomaszmiller.service.UserService;
import pl.tomaszmiller.session.UserSession;

import java.net.URL;
import java.util.List;
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

    @FXML private ListView<String> theList;
    @FXML private TextField bookAuthor;
    @FXML private TextField bookTitle;
    @FXML private TextField pages;
    @FXML private TextField searchField;
    @FXML private Button borrowBtn;

    @FXML private TableView<RentalRow> myRentalsTable;
    @FXML private TableColumn<RentalRow, Long> myRentalIdCol;
    @FXML private TableColumn<RentalRow, String> myRentalBookCol;
    @FXML private TableColumn<RentalRow, String> myRentalBorrowCol;
    @FXML private TableColumn<RentalRow, String> myRentalDueCol;
    @FXML private TableColumn<RentalRow, String> myRentalStatusCol;

    @FXML private TextField settingsFirstName;
    @FXML private TextField settingsLastName;
    @FXML private TextField settingsPhone;
    @FXML private PasswordField settingsNewPassword;
    @FXML private PasswordField settingsConfirmPassword;
    @FXML private Button saveSettingsBtn;

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

    @FXML
    private void onBorrowBook() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            Utils.openDialog("Borrow book", "You must be logged in to borrow a book.");
            return;
        }
        if (selectedBookId <= 0) {
            Utils.openDialog("Borrow book", "First select a book from the list.");
            return;
        }
        Optional<Rental> rental = rentalService.borrow(currentUser.id(), selectedBookId);
        if (rental.isPresent()) {
            Utils.confirmDialog("Borrow book",
                    "Book has been borrowed. Due date: " + rental.get().dueDate() + ".");
            refreshMyRentals();
        } else {
            Utils.openDialog("Borrow book", "Failed to borrow book.");
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
        refreshMyRentals();
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
        List<RentalRow> rows = rentalService.findByUser(currentUser.id()).stream()
                .map(r -> {
                    String bookTitleStr = bookService.findById(r.bookId())
                            .map(Book::title).orElse("(id=" + r.bookId() + ")");
                    return new RentalRow(r.id(), bookTitleStr,
                            r.borrowDate().toString(), r.dueDate().toString(), r.status().name());
                })
                .toList();
        myRentalsTable.setItems(FXCollections.observableArrayList(rows));
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
            Utils.openDialog("Settings", "First and last name are required (2-48 characters, letters only).");
            return;
        }
        if (!phone.isEmpty() && !Utils.isValidPhoneNumber(phone)) {
            Utils.openDialog("Settings", "Invalid phone number format.");
            return;
        }

        User updated = new User(currentUser.id(), firstName, lastName,
                currentUser.email(), phone, currentUser.role());
        boolean saved = userService.updateProfile(updated);
        if (saved) {
            UserSession.setCurrentUser(updated);
        } else {
            Utils.openDialog("Settings", "Failed to save changes.");
            return;
        }

        String newPass = settingsNewPassword != null ? settingsNewPassword.getText() : "";
        String confirmPass = settingsConfirmPassword != null ? settingsConfirmPassword.getText() : "";
        if (newPass != null && !newPass.isBlank()) {
            if (newPass.length() < 8) {
                Utils.openDialog("Change password", "Password must be at least 8 characters long.");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Utils.openDialog("Change password", "Passwords do not match.");
                return;
            }
            boolean changed = userService.changePassword(currentUser.id(), newPass);
            if (changed) {
                settingsNewPassword.clear();
                settingsConfirmPassword.clear();
                Utils.confirmDialog("Settings", "Data and password have been updated.");
            } else {
                Utils.openDialog("Change password", "Failed to change password.");
            }
            return;
        }
        Utils.confirmDialog("Settings", "Data has been updated.");
    }

    /**
     * DTO for the personal rentals TableView.
     */
    public static final class RentalRow {
        private final long id;
        private final String bookTitle;
        private final String borrowDate;
        private final String dueDate;
        private final String status;

        public RentalRow(long id, String bookTitle, String borrowDate, String dueDate, String status) {
            this.id = id;
            this.bookTitle = bookTitle;
            this.borrowDate = borrowDate;
            this.dueDate = dueDate;
            this.status = status;
        }

        public long getId() {
            return id;
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
