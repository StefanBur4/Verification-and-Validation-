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

    // TC-41 Verify processLine edge cases (Null, Empty, Comments)
    // Covers: line == null, line.isEmpty(), line.startsWith("#")
    @Test
    void tc41_processLineEdgeCases() {
        // 1. Null line
        mapper.processLine(null);
        assertEquals("", getOutput());

        // 2. Empty line
        mapper.processLine("");
        assertEquals("", getOutput());

        // 3. Whitespace only
        mapper.processLine("   ");
        assertEquals("", getOutput());

        // 4. Comment line
        mapper.processLine("# This is a comment");
        assertEquals("", getOutput());
    }

    // TC-42 Verify log command with missing arguments
    // Covers: handleLog -> parts.length < 2
    @Test
    void tc42_logMissingArguments() {
        mapper.processLine("log");
        assertEquals("Invalid username format", getOutput());
    }

    // TC-43 Verify unknown command when logged in
    // Covers: switch(command) -> default case
    @Test
    void tc43_unknownCommandLoggedIn() {
        loginUser("Alice");
        mapper.processLine("dance");
        assertEquals("Unknown command: dance", getOutput());
    }

    // TC-44 Verify add command with unknown option flag
    // Covers: handleAdd -> switch(opt) -> default case
    @Test
    void tc44_addUnknownOption() {
        loginUser("admin");
        // Included valid options too so it doesn't fail on missing options first
        mapper.processLine("add -t Title -a Author -d 2020 -i 100 -z value");

        // The code prints the error but continues processing.
        // Since we provided all valid opts, it eventually succeeds adding the book.
        // We check that the error message appears in the output.
        String out = getOutput();
        assertTrue(out.contains("Unknown option: -z"));
        assertTrue(out.contains("The book is registered as 1."));
    }

    // TC-45 Verify add command missing specific options (Short-circuit coverage)
    // Covers: if (title == null || author == null || yearStr == null || isbnStr == null)
    // specifically author == null and isbnStr == null branches
    @Test
    void tc45_addMissingSpecificOptions() {
        loginUser("admin");

        // Case A: Title provided, but Author missing (Hits 'author == null')
        mapper.processLine("add -t Title -d 2020 -i 100");
        assertEquals("Missing required option: -t, -a, -d, or -i", getOutput());
        resetOutput();

        // Case B: Title & Author provided, but ISBN missing (Hits 'isbnStr == null')
        // Note: -d is provided, so yearStr is not null.
        mapper.processLine("add -t Title -a Author -d 2020");
        assertEquals("Missing required option: -t, -a, -d, or -i", getOutput());
    }

    // TC-46 Verify remove command with no IDs
    // Covers: handleRemove -> parts.length < 2
    @Test
    void tc46_removeNoArgs() {
        loginUser("admin");
        mapper.processLine("remove");
        // Should return immediately without printing anything
        assertEquals("", getOutput());
    }

    // TC-47 Verify remove multiple books (Formatting check)
    // Covers: handleRemove -> if (i > 0) sb.append(" ") inside the success loop
    @Test
    void tc47_removeMultipleBooksFormatting() {
        loginUser("admin");
        addSampleBook(100, "B1", "A1", 2000); // ID 1
        addSampleBook(101, "B2", "A2", 2000); // ID 2
        resetOutput();

        mapper.processLine("remove 1 2");

        String expected = "The following books were removed: 1 2.";
        assertEquals(expected, getOutput());
    }

    // TC-48 Verify remove where no books are successfully removed
    // Covers: handleRemove -> if (!removed.isEmpty()) false branch
    @Test
    void tc48_removeOnlyInvalidIds() {
        loginUser("admin");
        addSampleBook(100, "B1", "A1", 2000); // ID 1
        resetOutput();

        // Only providing invalid IDs. 'removed' list will be empty.
        mapper.processLine("remove 99 100");

        String expected = "The following IDs do not exist: 99 100.";
        assertEquals(expected, getOutput());
    }

    // TC-49 Verify formatDate with null date (Edge Case)
    // Covers: formatDate -> if (date == null)
    @Test
    void tc49_listAdminWithNullDateBook() {
        loginUser("admin");
        Book b = addSampleBook(100, "Title", "Author", 2000);

        // Force borrow to make it "unavailable" so admin list tries to print extra info
        b.borrow(new Domain.User("Alice"));

        // HACK: Manually set date to NULL using the helper method you already have
        // This simulates a data corruption or edge case unreachable by normal commands
        setLimitReturnDate(b, null);

        resetOutput();
        mapper.processLine("list");

        String out = getOutput();
        // Admin format: ID \t Title \t Author \t Year \t Borrower \t Date
        // Since date is null, formatDate returns "", so it ends with a tab or nothing after borrower
        // Verify it contains borrower "Alice" but no "null" string and no date
        assertTrue(out.contains("Alice"));
        assertFalse(out.contains("null"));
        // Should look like: "1\tTitle\tAuthor\t2000\tAlice\t" (possibly with trailing whitespace)
    }

    // TC-50 Verify remove with invalid ID format (NumberFormatException)
    // Covers: handleRemove -> catch (NumberFormatException)
    @Test
    void tc50_removeInvalidIdFormat() {
        loginUser("admin");
        addSampleBook(100, "Title", "Author", 2000); // ID 1
        resetOutput();

        // "1" is valid, "abc" is invalid. Both should be processed.
        mapper.processLine("remove 1 abc");

        String ls = System.lineSeparator();
        String expected = "Invalid ID format in remove command: abc" + ls +
                "The following books were removed: 1.";

        assertEquals(expected, getOutput());
    }

    // TC-51 Verify borrow with invalid ID format
    // Covers: handleBorrow -> catch (NumberFormatException)
    @Test
    void tc51_borrowInvalidIdFormat() {
        loginUser("Alice");
        mapper.processLine("borrow abc");
        assertEquals("Invalid ID format in borrow command.", getOutput());
    }

    // TC-52 Verify list as Admin showing AVAILABLE books
    // Covers: handleList -> if (admin) -> if (available) TRUE branch
    @Test
    void tc52_listAdminAvailable() {
        loginUser("admin");
        // We add a book but DO NOT borrow it. It stays available.
        addSampleBook(100, "Title", "Author", 2000);
        resetOutput();

        mapper.processLine("list");

        // Admin format for available: ID \t Title \t Author \t Year
        // (It should NOT print borrower info)
        assertEquals("1\tTitle\tAuthor\t2000", getOutput());
    }

    // TC-53 Verify list with filter options (-av, -br)
    // Covers: handleList -> if ("-av".equals...) and if ("-br".equals...)
    @Test
    void tc53_listFilters() {
        loginUser("Alice");
        Book b1 = addSampleBook(100, "Avail", "Auth", 2000); // ID 1
        Book b2 = addSampleBook(101, "Borrow", "Auth", 2000); // ID 2
        b2.borrow(new Domain.User("Alice"));
        resetOutput();

        // 1. Test -av (Available only)
        mapper.processLine("list -av");
        String outAv = getOutput();
        assertTrue(outAv.contains("Avail"));
        assertFalse(outAv.contains("Borrow"));

        resetOutput();

        // 2. Test -br (Borrowed only)
        mapper.processLine("list -br");
        String outBr = getOutput();
        assertFalse(outBr.contains("Avail"));
        assertTrue(outBr.contains("Borrow"));
    }

    // TC-54 Verify add with missing Title
    // Covers: handleAdd -> if (title == null ...) TRUE branch
    @Test
    void tc54_addMissingTitle() {
        loginUser("admin");
        // We provide other fields but SKIP -t
        mapper.processLine("add -a Author -d 2022 -i 123");
        assertEquals("Missing required option: -t, -a, -d, or -i", getOutput());
    }

    // TC-55 Verify check with arguments
    // Covers: handleCheck -> if (parts.length >= 2) TRUE branch
    @Test
    void tc55_checkWithArguments() {
        loginUser("Alice");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice"));
        // Force it to be overdue so it shows up in -b
        setLimitReturnDate(b, new Date(System.currentTimeMillis() - 100000000));
        resetOutput();

        // Using -b triggers the "parts.length >= 2" branch
        mapper.processLine("check -b");
        String out = getOutput();
        assertTrue(out.contains("Title"));
    }

    // TC-56 Verify add with invalid copies format
    // Covers: handleAdd -> parsing copies -> catch (NumberFormatException)
    @Test
    void tc56_addInvalidCopiesFormat() {
        loginUser("admin");
        mapper.processLine("add -t T -a A -d 2020 -i 100 -n xyz");
        assertEquals("Invalid copies number", getOutput());
    }

    // TC-57 Verify return missing arguments
    // Covers: handleReturn -> if (parts.length < 2)
    @Test
    void tc57_returnMissingArgs() {
        loginUser("Alice");
        mapper.processLine("return");
        assertEquals("Usage: return [ID]", getOutput());
    }

    // TC-58 Verify borrow missing arguments
    // Covers: handleBorrow -> if (parts.length < 2)
    @Test
    void tc58_borrowMissingArgs() {
        loginUser("Alice");
        mapper.processLine("borrow");
        assertEquals("Usage: borrow [ID]", getOutput());
    }

    // TC-59 Verify extend missing arguments
    // Covers: handleExtend -> if (parts.length < 2)
    @Test
    void tc59_extendMissingArgs() {
        loginUser("Alice");
        mapper.processLine("extend");
        assertEquals("Usage: extend [ID]", getOutput());
    }

    // TC-60 Verify extend with invalid ID format
    // Covers: handleExtend -> catch (NumberFormatException)
    @Test
    void tc60_extendInvalidIdFormat() {
        loginUser("Alice");
        mapper.processLine("extend abc");
        assertEquals("Invalid ID format in extend command.", getOutput());
    }

    // TC-61 Verify list command alias -available
    // Covers: handleList -> "-available".equals(option)
    @Test
    void tc61_listAvailableAlias() {
        loginUser("admin");
        addSampleBook(100, "BookAvail", "Auth", 2000);
        resetOutput();

        mapper.processLine("list -available");

        String out = getOutput();
        assertTrue(out.contains("BookAvail"));
    }

    // TC-62 Verify list command alias -borrowed
    // Covers: handleList -> "-borrowed".equals(option)
    @Test
    void tc62_listBorrowedAlias() {
        loginUser("admin");
        Book b = addSampleBook(100, "BookBr", "Auth", 2000);
        b.borrow(new Domain.User("Alice"));
        resetOutput();

        mapper.processLine("list -borrowed");

        String out = getOutput();
        assertTrue(out.contains("BookBr"));
    }

    // TC-63 Verify return command on a book that exists but is NOT borrowed
    // Covers: handleReturn -> if (book.isAvailable())
    @Test
    void tc63_returnAvailableBook() {
        loginUser("Alice");
        addSampleBook(100, "Title", "Author", 2000); // Not borrowed
        resetOutput();

        mapper.processLine("return 1");
        assertEquals("Book 1 is not currently borrowed.", getOutput());
    }

    // TC-64 Verify extend command on a book that exists but is NOT borrowed
    // Covers: handleExtend -> if (book.isAvailable())
    @Test
    void tc64_extendAvailableBook() {
        loginUser("Alice");
        addSampleBook(100, "Title", "Author", 2000); // Not borrowed
        resetOutput();

        // The implementation prints "Book not found" for security/logic when trying to extend an available book
        mapper.processLine("extend 1");
        assertEquals("Book not found", getOutput());
    }

    // TC-65 Verify return command with invalid ID format
    // Covers: handleReturn -> catch (NumberFormatException)
    @Test
    void tc65_returnInvalidIdFormat() {
        loginUser("Alice");
        mapper.processLine("return abc");
        assertEquals("Invalid ID format in return command.", getOutput());
    }

    // TC-66 Verify check command with empty library
    // Covers: handleCheck -> if (books.isEmpty())
    @Test
    void tc66_checkEmptyLibrary() {
        loginUser("Alice");
        mapper.processLine("check");
        assertEquals("No books in library.", getOutput());
    }

    // TC-67 Verify check command skips available books
    // Covers: handleCheck -> if (b.isAvailable()) inside loop
    @Test
    void tc67_checkSkipsAvailableBooks() {
        loginUser("Alice");
        addSampleBook(100, "Title", "Author", 2000); // Available
        resetOutput();

        // Should not print anything because the book is available
        mapper.processLine("check");
        assertEquals("No borrowed books found for this filter.", getOutput());
    }

    // TC-68 Verify check command logic for normal users (Filtering other's books)
    // Covers: handleCheck -> !admin && (... !username.equals(current) ...)
    @Test
    void tc68_checkUserFilterLogic() {
        loginUser("Alice");
        Book b1 = addSampleBook(100, "B_Bob", "A", 2000);
        Book b2 = addSampleBook(101, "B_Alice", "A", 2000);

        b1.borrow(new Domain.User("Bob"));
        b2.borrow(new Domain.User("Alice"));

        resetOutput();
        mapper.processLine("check");

        String out = getOutput();
        // Alice should ONLY see her book
        assertTrue(out.contains("B_Alice"));
        assertFalse(out.contains("B_Bob"));
    }

    // TC-69 Verify check -b logic when book is borrowed but NOT overdue
    // Covers: handleCheck -> if (onlyExceeded) -> !before(today) branch
    @Test
    void tc69_checkExceededNotOverdue() {
        loginUser("Alice");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice")); // Default loan is 7 days, so it is NOT overdue

        resetOutput();
        mapper.processLine("check -b");

        // Should be empty because it's not overdue
        assertEquals("No borrowed books found for this filter.", getOutput());
    }

    // TC-70 Verify check -b logic with NULL return date (Defensive coding)
    // Covers: handleCheck -> if (onlyExceeded) -> date == null branch
    @Test
    void tc70_checkExceededNullDate() {
        loginUser("admin");
        Book b = addSampleBook(100, "Title", "Author", 2000);
        b.borrow(new Domain.User("Alice"));

        // Force null date using reflection helper
        setLimitReturnDate(b, null);

        resetOutput();
        mapper.processLine("check -b");

        // Should skip the book because date is null (safety check)
        assertEquals("No borrowed books found for this filter.", getOutput());
    }

    // Coverage Fix: Book.extendLoan() - Branch when limitReturnDate is null
    @Test
    void testBookExtendLoanDirectly_NotBorrowed() {
        // 1. Creamos un libro directamente (sin usar library para aislarlo o usamos library helper)
        Book book = new Book(1, 100, "Title", "Author", 2020);

        // 2. Aseguramos que NO está prestado (limitReturnDate es null)
        assertNull(book.getLimitReturnDate());

        // 3. Llamamos al método directamente.
        // Esto ejecutará la línea: "if (this.limitReturnDate == null) return;"
        book.extendLoan();

        // 4. Verificamos que no pasó nada (sigue null, no explotó)
        assertNull(book.getLimitReturnDate());
        assertFalse(book.isExceeded());
    }

    // Coverage Fix: Library.removeBook & getBookById - Branch 'False' inside loop
    @Test
    void testLibraryLoopMismatches() {
        // 1. Añadimos un libro (ID será 1)
        addSampleBook(100, "Java", "Gosling", 1995);

        // AHORA la lista tiene tamaño 1.

        // 2. Intentamos borrar el ID 99.
        // El bucle entra, mira el libro ID 1.
        // Compara: (1 == 99) -> FALSE. (¡Aquí ganamos el coverage!)
        // Termina el bucle y devuelve false.
        boolean removed = library.removeBook(99);
        assertFalse(removed);

        // 3. Lo mismo para getBookById.
        // Busca ID 99, mira el libro 1, no coincide (Branch False), devuelve null.
        Book book = library.getBookById(99);
        assertNull(book);
    }

    // Coverage Fix: Library.isCurrentUserAdmin - Branch when currentUser is null
    @Test
    void testIsCurrentUserAdmin_NullUser() {
        // 1. Aseguramos que no hay usuario
        library.setCurrentUser(null);

        // 2. Llamamos al método.
        // Evalúa "currentUser != null" como Falso y sale (Short circuit).
        boolean isAdmin = library.isCurrentUserAdmin();

        assertFalse(isAdmin);
    }

}
