import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String inputFile = "LibraryManager\\src\\library_manager.txt";
        Library library = new Library();
        CommandMapper commandMapper = new CommandMapper(library);

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                commandMapper.processLine(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file '" + inputFile + "': " + e.getMessage());
        }
    }
}
