package Domain;

import java.sql.Date;

public class Book {
    protected int ID;
    protected int ISBN;
    protected String title;
    protected String author;
    protected int yearPublished;
    private User borrower;
    private Date limitReturnDate;
    private boolean isExceeded;

    public Book(int ID, int ISBN, String title, String author, int yearPublished) {
        this.ID = ID;
        this.ISBN = ISBN;
        this.title = title;
        this.author = author;
        this.yearPublished = yearPublished;
        this.borrower = null;
        this.limitReturnDate = null;
        this.isExceeded = false;
    }

    public int getID() {
        return ID;
    }

    public int getISBN() {
        return ISBN;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getYearPublished() {
        return yearPublished;
    }

    public User getBorrower() {
        return borrower;
    }

    public Date getLimitReturnDate() {
        return limitReturnDate;
    }

    public boolean isExceeded() {
        return isExceeded;
    }

    // ------------ Additional functional methods ------------

    /* Returns true if the book is available for loan, false otherwise. */
    public boolean isAvailable() {
        return borrower == null;
    }

    /* Changes book to borrowed by a user with a limit return date */
    public void borrow(User username) {
        this.borrower = username;
        this.limitReturnDate = addDays(new Date(System.currentTimeMillis()), 7);
        this.isExceeded = false;
    }

    /* Changes book to returned (clears all loan information) */
    public void returnBook() {
        this.borrower = null;
        this.limitReturnDate = null;
        this.isExceeded = false;
    }

    /* Extends the loan limit date and marks the book as exceeded */
    public void extendLoan() {
        if (this.limitReturnDate == null) {
            return; // Cannot extend loan if the book is not borrowed
        }
        this.limitReturnDate = addDays(this.limitReturnDate, 7);
        this.isExceeded = true;
    }

    /* Helper method to add days to a date */
    private Date addDays(Date date, int days) {
        long millis = date.getTime();
        long addedMillis = days * 24L * 60 * 60 * 1000;
        return new Date(millis + addedMillis);
    }
}
