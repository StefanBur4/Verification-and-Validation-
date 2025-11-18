package Test;

import Domain.Book;
import Domain.Library;
import Mapper.CommandMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

public class CommandMapperTest {

    private Library library;
    private CommandMapper mapper;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        library = new Library();
        mapper = new CommandMapper(library);

        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String getOutput() {
        return outContent.toString().replace("\r\n", "\n").trim();
    }

    private void resetOutput() {
        outContent.reset();
    }

    private void loginUser(String username) {
        mapper.processLine("log " + username);
        resetOutput();
    }

    private Book addSampleBook(int idIsbn, String title, String author, int year) {
        return library.addSingleBook(idIsbn, title, author, year);
    }

    private void setLimitReturnDate(Book book, Date date) {
        try {
            Field field = Book.class.getDeclaredField("limitReturnDate");
            field.setAccessible(true);
            field.set(book, date);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TC-01 Verify invalid command when not logged in
    @Test
    void tc01_invalidCommandNotLoggedIn() {
        mapper.processLine("borrow 1");
        assertEquals("You must log in with: log [USERNAME]", getOutput());
    }

    // TC-02 Verify login with invalid username format (numbers)
    @Test
    void tc02_invalidUsernameFormat() {
        mapper.processLine("log user1");
        assertEquals("Invalid username format", getOutput());
    }

    // TC-03 Verify admin login success
    @Test
    void tc03_adminLoginSuccess() {
        mapper.processLine("log admin");
        assertEquals("You are log as admin", getOutput());
    }

    // TC-04 Verify login when already logged in
    @Test
    void tc04_loginWhenAlreadyLoggedIn() {
        mapper.processLine("log admin");
        resetOutput();
        mapper.processLine("log userTwo");
        assertEquals("User already logged in", getOutput());
    }

    // TC-05 Verify logout
    @Test
    void tc05_logoutSuccess() {
        loginUser("Alice");
        mapper.processLine("logout");
        assertEquals("You are logged out.", getOutput());
    }

    // TC-06 Verify logout when not logged in
    @Test
    void tc06_logoutNotLoggedIn() {
        mapper.processLine("logout");
        assertEquals("You must log in with: log [USERNAME]", getOutput());
    }

    // TC-07 Verify unauthorized add attempt
    @Test
    void tc07_unauthorizedAdd() {
        loginUser("Alice");
        mapper.processLine("add -t Book -a Author -d 2022 -i 12345");
        assertEquals("User not authorized", getOutput());
    }

    // TC-08 Verify add book with missing required option
    @Test
    void tc08_addMissingRequiredOption() {
        loginUser("admin");
        mapper.processLine("add -t Title -a Author");
        assertEquals("Missing required option: -t, -a, -d, or -i", getOutput());
    }

    // TC-09 Verify add book with invalid Year format
    @Test
    void tc09_addInvalidYearFormat() {
        loginUser("admin");
        mapper.processLine("add -t Title -a Author -d two -i 123");
        assertEquals("Invalid year format", getOutput());
    }

    // TC-10 Verify add book with invalid ISBN format
    @Test
    void tc10_addInvalidIsbnFormat() {
        loginUser("admin");
        mapper.processLine("add -t Title -a Author -d 2020 -i abc");
        assertEquals("Invalid ISBN format", getOutput());
    }

    // TC-11 Verify add book with invalid copies (Negative)
    @Test
    void tc11_addInvalidCopiesNegative() {
        loginUser("admin");
        mapper.processLine("add -t Title -a Author -d 2020 -i 123 -n -5");
        assertEquals("Invalid copies number", getOutput());
    }

    // TC-12 Verify add single book (Success)
    @Test
    void tc12_addSingleBookSuccess() {
        loginUser("admin");
        mapper.processLine("add -t Java -a Gosling -d 1995 -i 100");
        assertEquals("The book is registered as 1.", getOutput());
    }

    // TC-13 Verify add multiple copies
    @Test
    void tc13_addMultipleCopies() {
        loginUser("admin");
        // existing book so that next ID is 2
        addSampleBook(101, "Other", "Author", 1990);
        resetOutput();

        mapper.processLine("add -t Java -a Gosling -d 1995 -i 100 -n 2");
        assertEquals("The books are registered as 2 3.", getOutput());
    }

    // TC-14 Verify remove book (Admin only)
    @Test
    void tc14_removeAdminOnly() {
        loginUser("Alice");
        mapper.processLine("remove 1");
        assertEquals("User not authorized", getOutput());
    }

    // TC-15 Verify remove mixed IDs (Valid and Invalid)
    @Test
    void tc15_removeMixedIds() {
        loginUser("admin");
        addSampleBook(100, "Java", "Gosling", 1995); // ID 1
        resetOutput();

        mapper.processLine("remove 1 99");
        String ls = System.lineSeparator();
        String expected = "The following books were removed: 1." + ls +
                          "The following IDs do not exist: 99.";
        assertEquals(expected, getOutput());
    }

    // TC-16 List all books (Empty Library)
    @Test
    void tc16_listEmptyLibrary() {
        loginUser("Alice");
        mapper.processLine("list");
        assertEquals("No books in library.", getOutput());
    }

    // TC-17 List books as Regular User
    @Test
    void tc17_listRegularUser() {
        loginUser("Alice");
        addSampleBook(100, "Title", "Author", 2000);
        resetOutput();

        mapper.processLine("list");
        assertEquals("1\tTitle\tAuthor\t2000", getOutput());
    }

    // TC-18 List books as Admin (Detailed)
    @Test
    void tc18_listAdminDetailed() {
        loginUser("admin");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice")); // borrowed by Alice
        resetOutput();

        mapper.processLine("list");
        String out = getOutput();
        assertTrue(out.startsWith("1\tTitle\tAuthor\t2000\tAlice\t"));
    }

    // TC-19 Search with unknown option
    @Test
    void tc19_searchUnknownOption() {
        loginUser("Alice");
        mapper.processLine("search -x Value");
        assertEquals("Unknown search option: -x", getOutput());
    }

    // TC-20 Search success for Title
    @Test
    void tc20_searchSuccessTitle() {
        loginUser("Alice");
        addSampleBook(100, "Java", "Gosling", 1995);
        resetOutput();

        mapper.processLine("search -t Java");
        assertEquals("1\t100\tJava\tGosling\t1995", getOutput());
    }

    // TC-21 Search success for Author
    @Test
    void tc21_searchSuccessAuthor() {
        loginUser("Alice");
        addSampleBook(100, "Java", "Gosling", 1995);
        resetOutput();

        mapper.processLine("search -a Gosling");
        assertEquals("1\t100\tJava\tGosling\t1995", getOutput());
    }

    // TC-22 Search success for Date
    @Test
    void tc22_searchSuccessDate() {
        loginUser("Alice");
        addSampleBook(100, "Java", "Gosling", 1995);
        resetOutput();

        mapper.processLine("search -d 1995");
        assertEquals("1\t100\tJava\tGosling\t1995", getOutput());
    }

    // TC-23 Verify search usage without filters
    @Test
    void tc23_searchUsageWithoutFilters() {
        loginUser("Alice");
        resetOutput();

        mapper.processLine("search");
        String ls = System.lineSeparator();
        String expected = "Usage: search [FILTERS]" + ls +
                "Filters:" + ls +
                "  -t [TITLE]   or -title [TITLE]" + ls +
                "  -a [AUTHOR]  or -author [AUTHOR]" + ls +
                "  -d [YEAR]    or -date [YEAR]";
        assertEquals(expected, getOutput());
    }

    // TC-24 Verify search with invalid year filter
    @Test
    void tc24_searchInvalidYearFilter() {
        loginUser("Alice");
        addSampleBook(100, "Java", "Gosling", 1995);
        resetOutput();

        mapper.processLine("search -d abc");
        assertEquals("Invalid year in search filter: abc", getOutput());
    }

    // TC-25 Verify search with author filter and no match
    @Test
    void tc25_searchAuthorNoMatch() {
        loginUser("Alice");
        addSampleBook(100, "Java", "Gosling", 1995);
        resetOutput();

        mapper.processLine("search -a Tolkien");
        assertEquals("No books match the given search filters.", getOutput());
    }

    // TC-26 Verify search with date filter and no match
    @Test
    void tc26_searchDateNoMatch() {
        loginUser("Alice");
        addSampleBook(100, "Java", "Gosling", 1995);
        resetOutput();

        mapper.processLine("search -d 2000");
        assertEquals("No books match the given search filters.", getOutput());
    }

    // TC-27 Search no results (Title)
    @Test
    void tc27_searchTitleNoResults() {
        loginUser("Alice");
        addSampleBook(100, "Java", "Gosling", 1995);
        resetOutput();

        mapper.processLine("search -t Python");
        assertEquals("No books match the given search filters.", getOutput());
    }

    // TC-28 Borrow non-existent ID
    @Test
    void tc28_borrowNonExistentId() {
        loginUser("Alice");
        mapper.processLine("borrow 99");
        assertEquals("No book found with ID 99.", getOutput());
    }

    // TC-29 Borrow already borrowed book
    @Test
    void tc29_borrowAlreadyBorrowed() {
        loginUser("Alice");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Bob"));
        resetOutput();

        mapper.processLine("borrow 1");
        assertEquals("Book 1 is already borrowed.", getOutput());
    }

    // TC-30 Borrow success
    @Test
    void tc30_borrowSuccess() {
        loginUser("Alice");
        addSampleBook(100, "Title", "Author", 2000);
        resetOutput();

        mapper.processLine("borrow 1");
        String out = getOutput();
        assertTrue(out.startsWith("Book 1 borrowed by Alice until "));
    }

    // TC-31 Return book not borrowed by user
    @Test
    void tc31_returnNotBorrowedByUser() {
        loginUser("Bob");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice"));
        resetOutput();

        mapper.processLine("return 1");
        assertEquals("Book 1 is borrowed by another user.", getOutput());
    }

    // TC-32 Return book non-existent ID
    @Test
    void tc32_returnNonExistentId() {
        loginUser("Bob");
        mapper.processLine("return 99");
        assertEquals("No book found with ID 99.", getOutput());
    }

    // TC-33 Return book success
    @Test
    void tc33_returnSuccess() {
        loginUser("Alice");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice"));
        resetOutput();

        mapper.processLine("return 1");
        assertEquals("Book 1 returned.", getOutput());
    }

    // TC-34 Extend loan (Unauthorized)
    @Test
    void tc34_extendUnauthorized() {
        loginUser("Bob");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice"));
        resetOutput();

        mapper.processLine("extend 1");
        assertEquals("Unauthorized: You are not the borrower", getOutput());
    }

    // TC-35 Extend loan non-existent ID
    @Test
    void tc35_extendNonExistentId() {
        loginUser("Bob");
        mapper.processLine("extend 99");
        assertEquals("Book not found", getOutput());
    }

    // TC-36 Extend loan (Success)
    @Test
    void tc36_extendSuccess() {
        loginUser("Alice");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice"));
        resetOutput();

        mapper.processLine("extend 1");
        String out = getOutput();
        assertTrue(out.startsWith("Loan extended. New limit date: "));
    }

    // TC-37 Extend loan (Limit Reached)
    @Test
    void tc37_extendLimitReached() {
        loginUser("Alice");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice"));
        b.extendLoan(); // already extended once -> isExceeded = true
        resetOutput();

        mapper.processLine("extend 1");
        assertEquals("Extension limit reached", getOutput());
    }

    // TC-38 Check borrowed books (User)
    @Test
    void tc38_checkBorrowedBooksUser() {
        loginUser("Alice");
        Book b1 = addSampleBook(100, "Title", "Author", 2000);
        Book b2 = addSampleBook(101, "Title", "Author", 2000);
        Domain.User alice = new Domain.User("Alice");
        b1.borrow(alice);
        b2.borrow(alice);
        resetOutput();

        mapper.processLine("check -all");
        String out = getOutput();
        String[] lines = out.split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("1\t100\tTitle\t"));
        assertTrue(lines[1].startsWith("2\t101\tTitle\t"));
    }

    // TC-39 Check exceed borrowed books (User)
    @Test
    void tc39_checkExceededBorrowedBooksUser() {
        loginUser("Alice");
        Book b1 = addSampleBook(100, "Title", "Author", 2000);
        Book b2 = addSampleBook(101, "Title", "Author", 2000);
        Domain.User alice = new Domain.User("Alice");
        b1.borrow(alice);
        b2.borrow(alice);

        // Make only book 2 overdue
        Date yesterday = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        setLimitReturnDate(b2, yesterday);

        resetOutput();
        mapper.processLine("check -b");
        String out = getOutput();
        String[] lines = out.split("\n");
        assertEquals(1, lines.length);
        assertTrue(lines[0].startsWith("2\t101\tTitle\t"));
    }

    // TC-40 Check borrowed books (Admin)
    @Test
    void tc40_checkBorrowedBooksAdmin() {
        loginUser("admin");
        Domain.User alice = new Domain.User("Alice");
        Domain.User bob = new Domain.User("Bob");

        Book b1 = addSampleBook(100, "Title", "Author", 2000);
        Book b2 = addSampleBook(101, "Title", "Author", 2000);
        Book b3 = addSampleBook(102, "Title", "Author", 2000);

        b1.borrow(alice);
        b2.borrow(alice);
        b3.borrow(bob);
        resetOutput();

        mapper.processLine("check -all");
        String out = getOutput();
        String[] lines = out.split("\n");
        assertEquals(3, lines.length);
        assertTrue(lines[0].startsWith("1\t100\tTitle\tAlice\t"));
        assertTrue(lines[1].startsWith("2\t101\tTitle\tAlice\t"));
        assertTrue(lines[2].startsWith("3\t102\tTitle\tBob\t"));
    }
}