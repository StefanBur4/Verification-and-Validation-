import java.sql.Date;
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
            // ignore empty lines and comments
            return;
        }

        String[] parts = line.split("\\s+");
        String command = parts[0];

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
                handleList();
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

    // -------- log / logout --------

    private void handleLog(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Missing username for log command.");
            return;
        }

        if (library.hasLoggedInUser()) {
            System.out.println("A user is already logged in.");
            return;
        }

        String username = parts[1];
        User user;
        if ("admin".equals(username)) {
            user = new Administrator(username);
        } else {
            user = new User(username);
        }

        library.setCurrentUser(user);
        System.out.println("You are logged in as " + username + ".");
    }

    private void handleLogout() {
        if (!library.hasLoggedInUser()) {
            System.out.println("No user is currently logged in.");
            return;
        }

        String name = library.getCurrentUser().getUsername();
        library.setCurrentUser(null);
        System.out.println("User " + name + " logged out.");
    }

    // -------- add / remove --------

    private void handleAdd(String[] parts) {
        if (!library.hasLoggedInUser()) {
            System.out.println("Please log in first.");
            return;
        }
        if (!library.isCurrentUserAdmin()) {
            System.out.println("Only admin can add books.");
            return;
        }

        if (parts.length < 5) {
            System.out.println("Usage: add <ISBN> <TITLE> <AUTHOR> <YEAR>");
            return;
        }

        try {
            int isbn = Integer.parseInt(parts[1]);
            String title = parts[2];
            String author = parts[3];
            int year = Integer.parseInt(parts[4]);

            Book book = library.addSingleBook(isbn, title, author, year);
            System.out.println("Book added with ID " + book.getID() + ".");

        } catch (NumberFormatException e) {
            System.out.println("Invalid number format in add command.");
        }
    }

    private void handleRemove(String[] parts) {
        if (!library.hasLoggedInUser()) {
            System.out.println("Please log in first.");
            return;
        }
        if (!library.isCurrentUserAdmin()) {
            System.out.println("Only admin can remove books.");
            return;
        }

        if (parts.length < 2) {
            System.out.println("Usage: remove <ID>");
            return;
        }

        try {
            int id = Integer.parseInt(parts[1]);
            boolean removed = library.removeBook(id);
            if (removed) {
                System.out.println("Book with ID " + id + " removed.");
            } else {
                System.out.println("No book found with ID " + id + ".");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format in remove command.");
        }
    }

    // -------- list --------

    private void handleList() {
        List<Book> books = library.getAllBooks();
        if (books.isEmpty()) {
            System.out.println("No books in library.");
            return;
        }

        for (Book b : books) {
            String status;
            if (b.isAvailable()) {
                status = "available";
            } else {
                User borrower = b.getBorrower();
                Date limit = b.getLimitReturnDate();
                status = "borrowed by " + borrower.getUsername()
                        + " until " + limit;
            }

            System.out.println(
                    b.getID() + " - " + b.getTitle() + " (" +
                            b.getAuthor() + ", " + b.getYearPublished() +
                            ") [" + status + "]");
        }
    }

    // -------- borrow / return / extend --------

    private void handleBorrow(String[] parts) {
        if (!library.hasLoggedInUser()) {
            System.out.println("Please log in first.");
            return;
        }
        if (parts.length < 2) {
            System.out.println("Usage: borrow <ID>");
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
                    + " until " + book.getLimitReturnDate() + ".");

        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format in borrow command.");
        }
    }

    private void handleReturn(String[] parts) {
        if (!library.hasLoggedInUser()) {
            System.out.println("Please log in first.");
            return;
        }
        if (parts.length < 2) {
            System.out.println("Usage: return <ID>");
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
        if (!library.hasLoggedInUser()) {
            System.out.println("Please log in first.");
            return;
        }
        if (parts.length < 2) {
            System.out.println("Usage: extend <ID>");
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
            if (!book.getBorrower().getUsername()
                    .equals(library.getCurrentUser().getUsername())) {
                System.out.println("Book " + id + " is borrowed by another user.");
                return;
            }
            if (book.isExceeded()) {
                System.out.println("Loan for book " + id + " was already extended once.");
                return;
            }

            library.extendLoan(id);
            System.out.println("Loan for book " + id + " extended until "
                    + book.getLimitReturnDate() + ".");

        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format in extend command.");
        }
    }

    // -------- check --------

    private void handleCheck(String[] parts) {
        if (!library.hasLoggedInUser()) {
            System.out.println("Please log in first.");
            return;
        }

        boolean onlyExceeded = false;
        if (parts.length >= 2 && "-b".equals(parts[1])) {
            onlyExceeded = true;
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
            if (b.isAvailable()) {
                continue; // nur ausgeliehene Bücher
            }

            // für normale User nur eigene Bücher
            if (!admin && (b.getBorrower() == null ||
                    !b.getBorrower().getUsername().equals(current.getUsername()))) {
                continue;
            }

            // -b: nur exceeded
            if (onlyExceeded && !b.isExceeded()) {
                continue;
            }

            anyPrinted = true;
            System.out.println(
                    b.getID() + "\t" +
                            b.getISBN() + "\t" +
                            b.getTitle() + "\t" +
                            b.getBorrower().getUsername() + "\t" +
                            b.getLimitReturnDate());
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

        // Optionen paarweise lesen: -t value, -a value, -d value ...
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
                    b.getYearPublished()
            );
        }

        if (!anyPrinted) {
            System.out.println("No books match the given search filters.");
        }
    }
}