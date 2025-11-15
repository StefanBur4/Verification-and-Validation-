package Mapper;

import Domain.Library;
import Domain.User;
import Domain.Administrator;
import Domain.Book;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CommandMapper {

    private final Library library;

    public CommandMapper(Library library) {
        this.library = library;
    }

    /*
     * Method to process each line in the library_manager.txt file and execute the
     * corresponding command
     */
    public void processLine(String line) {
        if (line == null)
            return;

        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            // Ignore empty lines and comments in input file
            return;
        }

        String[] parts = line.split("\\s+");
        String command = parts[0];

        // Global rule: if not logged in, only 'log' is allowed
        if (!"log".equals(command) && !library.hasLoggedInUser()) {
            System.out.println("You must log in with: log [USERNAME]");
            return;
        }

        switch (command) {
            case "log":
                handleLog(parts);
                break;
            case "logout":
                handleLogout();
                break;
            case "add":
                handleAdd(parts);
                break;
            case "list":
                handleList(parts);
                break;
            case "borrow":
                handleBorrow(parts);
                break;
            case "return":
                handleReturn(parts);
                break;
            case "extend":
                handleExtend(parts);
                break;
            case "remove":
                handleRemove(parts);
                break;
            case "check":
                handleCheck(parts);
                break;
            case "search":
                handleSearch(parts);
                break;
            default:
                System.out.println("Unknown command: " + command);
        }
    }

    // -------- Date helper class --------

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        return df.format(date);
    }

    // -------- log / logout --------

    private void handleLog(String[] parts) {
        // Missing username
        if (parts.length < 2) {
            System.out.println("Invalid username format");
            return;
        }

        // User already logged in check
        if (library.hasLoggedInUser()) {
            System.out.println("User already logged in");
            return;
        }

        String username = parts[1];

        // Invalid characters (only letters are allowed here)
        if (!username.matches("[A-Za-z]+")) {
            System.out.println("Invalid username format");
            return;
        }

        User user;
        if ("admin".equals(username)) {
            user = new Administrator(username);
        } else {
            user = new User(username);
        }

        // User successfully logged in
        library.setCurrentUser(user);
        System.out.println("You are log as " + username);
    }

    private void handleLogout() {
        // If this is reached, a user is logged in (global check in processLine)
        library.setCurrentUser(null);
        System.out.println("You are logged out.");
    }

    // -------- add / remove --------

    private void handleAdd(String[] parts) {
        // Only admin can add books
        if (!library.isCurrentUserAdmin()) {
            System.out.println("User not authorized");
            return;
        }

        String title = null;
        String author = null;
        String yearStr = null;
        String isbnStr = null;
        String copiesStr = null;

        // Parse options: -t [TITLE] -a [AUTHOR] -d [YEAR] -i [ISBN] -n [COPIES]
        for (int i = 1; i < parts.length - 1; i++) {
            String opt = parts[i];
            String val = parts[i + 1];

            switch (opt) {
                case "-t":
                    title = val;
                    i++;
                    break;
                case "-a":
                    author = val;
                    i++;
                    break;
                case "-d":
                    yearStr = val;
                    i++;
                    break;
                case "-i":
                    isbnStr = val;
                    i++;
                    break;
                case "-n":
                    copiesStr = val;
                    i++;
                    break;
                default:
                    System.out.println("Unknown option: " + opt);
                    break;
            }
        }

        if (title == null || author == null || yearStr == null || isbnStr == null) {
            System.out.println("Missing required option: -t, -a, -d, or -i");
            return;
        }

        int year;
        try {
            year = Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid year format");
            return;
        }

        int isbn;
        try {
            isbn = Integer.parseInt(isbnStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid ISBN format");
            return;
        }

        int copies = 1;
        if (copiesStr != null) {
            try {
                copies = Integer.parseInt(copiesStr);
                if (copies <= 0) {
                    System.out.println("Invalid copies number");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid copies number");
                return;
            }
        }
        /*
         * Spec: if a book with the same ISBN exists, add copies to the existing
         * entry. In this implementation each copy is represented as its own Book with a
         * unique ID.
         */

        if (copies == 1) {
            Book book = library.addSingleBook(isbn, title, author, year);
            System.out.println("The book is registered as " + book.getID() + ".");
        } else {
            StringBuilder ids = new StringBuilder();
            for (int c = 0; c < copies; c++) {
                Book book = library.addSingleBook(isbn, title, author, year);
                if (c > 0) {
                    ids.append(" ");
                }
                ids.append(book.getID());
            }
            System.out.println("The books are registered as " + ids.toString() + ".");
        }
    }

    private void handleRemove(String[] parts) {
        // Only admin can remove books
        if (!library.isCurrentUserAdmin()) {
            System.out.println("User not authorized");
            return;
        }

        if (parts.length < 2) {
            // No IDs given
            return;
        }

        List<Integer> removed = new ArrayList<>();
        List<Integer> notFound = new ArrayList<>();

        for (int i = 1; i < parts.length; i++) {
            try {
                int id = Integer.parseInt(parts[i]);
                boolean ok = library.removeBook(id);
                if (ok) {
                    removed.add(id);
                } else {
                    notFound.add(id);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid ID format in remove command: " + parts[i]);
            }
        }

        if (!removed.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < removed.size(); i++) {
                if (i > 0)
                    sb.append(" ");
                sb.append(removed.get(i));
            }
            System.out.println("The following books were removed: " + sb.toString() + ".");
        }

        if (!notFound.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < notFound.size(); i++) {
                if (i > 0)
                    sb.append(" ");
                sb.append(notFound.get(i));
            }
            System.out.println("The following IDs do not exist: " + sb.toString() + ".");
        }
    }

    // -------- list --------

    private void handleList(String[] parts) {
        String option = "-all"; // Default option

        if (parts.length >= 2) {
            option = parts[1];
        }

        List<Book> books = library.getAllBooks();
        if (books.isEmpty()) {
            System.out.println("No books in library.");
            return;
        }

        boolean admin = library.isCurrentUserAdmin();

        for (Book b : books) {
            boolean available = b.isAvailable();

            if ("-av".equals(option) || "-available".equals(option)) {
                if (!available) {
                    continue;
                }
            } else if ("-br".equals(option) || "-borrowed".equals(option)) {
                if (available) {
                    continue;
                }
            } // "-all" or unknown option: show all

            if (!admin) {
                // Regular user: ID, title, author, year
                System.out.println(
                        b.getID() + "\t" +
                                b.getTitle() + "\t" +
                                b.getAuthor() + "\t" +
                                b.getYearPublished());
            } else {
                // Admin: for borrowed books additionally borrower + limit date
                if (available) {
                    System.out.println(
                            b.getID() + "\t" +
                                    b.getTitle() + "\t" +
                                    b.getAuthor() + "\t" +
                                    b.getYearPublished());
                } else {
                    System.out.println(
                            b.getID() + "\t" +
                                    b.getTitle() + "\t" +
                                    b.getAuthor() + "\t" +
                                    b.getYearPublished() + "\t" +
                                    b.getBorrower().getUsername() + "\t" +
                                    formatDate(b.getLimitReturnDate()));
                }
            }
        }
    }

    // -------- borrow / return / extend --------

    private void handleBorrow(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: borrow [ID]");
            return;
        }

        try {
            int id = Integer.parseInt(parts[1]);
            Book book = library.getBookById(id);
            if (book == null) {
                System.out.println("No book found with ID " + id + ".");
                return;
            }
            if (!book.isAvailable()) {
                System.out.println("Book " + id + " is already borrowed.");
                return;
            }

            library.borrowBook(id);
            System.out.println("Book " + id + " borrowed by "
                    + library.getCurrentUser().getUsername()
                    + " until " + formatDate(book.getLimitReturnDate()) + ".");

        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format in borrow command.");
        }
    }

    private void handleReturn(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: return [ID]");
            return;
        }

        try {
            int id = Integer.parseInt(parts[1]);
            Book book = library.getBookById(id);
            if (book == null) {
                System.out.println("No book found with ID " + id + ".");
                return;
            }
            if (book.isAvailable()) {
                System.out.println("Book " + id + " is not currently borrowed.");
                return;
            }
            User borrower = book.getBorrower();
            if (!borrower.getUsername().equals(library.getCurrentUser().getUsername())) {
                System.out.println("Book " + id + " is borrowed by another user.");
                return;
            }

            library.returnBook(id);
            System.out.println("Book " + id + " returned.");

        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format in return command.");
        }
    }

    private void handleExtend(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: extend [ID]");
            return;
        }

        try {
            int id = Integer.parseInt(parts[1]);
            Book book = library.getBookById(id);
            if (book == null) {
                System.out.println("Book not found");
                return;
            }
            if (book.isAvailable()) {
                System.out.println("Book not found");
                return;
            }
            if (!book.getBorrower().getUsername()
                    .equals(library.getCurrentUser().getUsername())) {
                System.out.println("Unauthorized: You are not the borrower");
                return;
            }
            if (book.isExceeded()) {
                System.out.println("Extension limit reached");
                return;
            }

            library.extendLoan(id);
            System.out.println("Loan extended. New limit date: "
                    + formatDate(book.getLimitReturnDate()));

        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format in extend command.");
        }
    }

    // -------- check --------

    private void handleCheck(String[] parts) {
        boolean onlyExceeded = false; // Default is -all

        if (parts.length >= 2) {
            if ("-b".equals(parts[1])) {
                onlyExceeded = true;
            }
        }

        List<Book> books = library.getAllBooks();
        if (books.isEmpty()) {
            System.out.println("No books in library.");
            return;
        }

        User current = library.getCurrentUser();
        boolean admin = current.isAdmin();
        boolean anyPrinted = false;

        for (Book b : books) {
            // Skip available books
            if (b.isAvailable()) {
                continue;
            }

            // For normal users only show books borrowed by themselves
            if (!admin && (b.getBorrower() == null ||
                    !b.getBorrower().getUsername().equals(current.getUsername()))) {
                continue;
            }

            // For -b, only show books whose limit date is in the past (overdue)
            if (onlyExceeded) {
                Date today = new Date(System.currentTimeMillis());
                if (b.getLimitReturnDate() == null || !b.getLimitReturnDate().before(today)) {
                    continue;
                }
            }

            anyPrinted = true;
            if (admin) {
                System.out.println(
                        b.getID() + "\t" +
                                b.getISBN() + "\t" +
                                b.getTitle() + "\t" +
                                b.getBorrower().getUsername() + "\t" +
                                formatDate(b.getLimitReturnDate()));
            } else {
                System.out.println(
                        b.getID() + "\t" +
                                b.getISBN() + "\t" +
                                b.getTitle() + "\t" +
                                formatDate(b.getLimitReturnDate()));
            }
        }

        if (!anyPrinted) {
            System.out.println("No borrowed books found for this filter.");
        }
    }

    // -------- search --------

    private void handleSearch(String[] parts) {
        if (parts.length == 1) {
            System.out.println("Usage: search [FILTERS]");
            System.out.println("Filters:");
            System.out.println("  -t [TITLE]   or -title [TITLE]");
            System.out.println("  -a [AUTHOR]  or -author [AUTHOR]");
            System.out.println("  -d [YEAR]    or -date [YEAR]");
            return;
        }

        String titleFilter = null;
        String authorFilter = null;
        Integer yearFilter = null;

        // Read options in pairs: -t value, -a value, -d value ...
        for (int i = 1; i < parts.length - 1; i += 2) {
            String opt = parts[i];
            String val = parts[i + 1];

            switch (opt) {
                case "-t":
                case "-title":
                    titleFilter = val;
                    break;
                case "-a":
                case "-author":
                    authorFilter = val;
                    break;
                case "-d":
                case "-date":
                    try {
                        yearFilter = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid year in search filter: " + val);
                        return;
                    }
                    break;
                default:
                    System.out.println("Unknown search option: " + opt);
                    return;
            }
        }

        List<Book> books = library.getAllBooks();
        boolean anyPrinted = false;

        for (Book b : books) {
            if (titleFilter != null && !b.getTitle().equals(titleFilter)) {
                continue;
            }
            if (authorFilter != null && !b.getAuthor().equals(authorFilter)) {
                continue;
            }
            if (yearFilter != null && b.getYearPublished() != yearFilter) {
                continue;
            }

            anyPrinted = true;
            System.out.println(
                    b.getID() + "\t" +
                            b.getISBN() + "\t" +
                            b.getTitle() + "\t" +
                            b.getAuthor() + "\t" +
                            b.getYearPublished());
        }

        if (!anyPrinted) {
            System.out.println("No books match the given search filters.");
        }
    }
}