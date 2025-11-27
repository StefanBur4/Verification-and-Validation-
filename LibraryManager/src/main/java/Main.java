import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import Domain.Library;
import Mapper.CommandMapper;

public class Main {
    public static void main(String[] args) {
        Library library = new Library();
        CommandMapper commandMapper = new CommandMapper(library, System.out);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Main.class.getClassLoader().getResourceAsStream("library_manager.txt"),
                        StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                commandMapper.processLine(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading library_manager.txt: " + e.getMessage());
        }
    }
}
