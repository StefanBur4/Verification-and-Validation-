import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Library {

    private final List<Book> books = new ArrayList<>();
    private int nextId = 1;

    private User currentUser;

    private static final int LOAN_DAYS = 7;

    // ------------ User administration ------------

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean hasLoggedInUser() {
        return currentUser != null;
    }

    public boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    // ------------ Book administration ------------

    /* Adds a single book to the library with a unique identifier */
    public Book addSingleBook(int isbn, String title, String author, int yearPublished) {
        int id = nextId++;
        Book book = new Book(id, isbn, title, author, yearPublished);
        books.add(book);
        return book;
    }

    /* Returns a book by its unique identifier out of the books list */
    public Book getBookById(int id) {
        for (Book b : books) {
            if (b.getID() == id) {
                return b;
            }
        }
        return null;
    }

    /* Returns all books in the library as an unmodifiable list */
    public List<Book> getAllBooks() {
        return Collections.unmodifiableList(books);
    }

    /* Removes book out of the library by its unique identifier */
    public boolean removeBook(int id) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getID() == id) {
                books.remove(i);
                return true;
            }
        }
        return false;
    }

    // -------- Borrow operations --------

    /*
     * Set a book state to borrowed and set the limit date to current date plus
     * seven loan days
     */
    public void borrowBook(int id) {
        Book book = getBookById(id);

        Date limitDate = addDays(new Date(System.currentTimeMillis()), LOAN_DAYS);
        book.borrow(currentUser, limitDate);
    }

    /* Set a book state to not borrowed */
    public void returnBook(int id) {
        Book book = getBookById(id);
        book.returnBook();
    }

    /* Extend the loan of a book by another seven days */
    public void extendLoan(int id) {
        Book book = getBookById(id);
        Date currentLimit = book.getLimitReturnDate();

        Date newLimit = addDays(currentLimit, LOAN_DAYS);
        book.extendLoan(newLimit);
    }

    // -------- Helper method for date calculations --------

    /* Adds a specified number of days to a given date */
    private Date addDays(Date base, int days) {
        long millisPerDay = 24L * 60L * 60L * 1000L;
        long newTime = base.getTime() + days * millisPerDay;
        return new Date(newTime);
    }
}