package cricket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Saves finished matches to a CSV file and reads them back. */
public class MatchStore {

    private static final String HEADER =
            "date,mode,first_team,first_score,second_team,second_score,result";

    private final Path file;

    public MatchStore(String fileName) {
        this.file = Paths.get(fileName);
    }

    public Path getFile() {
        return file;
    }

    public void save(MatchResult result, String mode) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!Files.exists(file) || Files.size(file) == 0) {
            lines.add(HEADER);
        }
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        lines.add(String.join(",",
                date,
                clean(mode),
                clean(result.getFirst().getTeamName()),
                clean(result.getFirst().scoreLine()),
                clean(result.getSecond().getTeamName()),
                clean(result.getSecond().scoreLine()),
                clean(result.getDescription())));
        Files.write(file, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Returns one String[7] per saved match, oldest first. */
    public List<String[]> load() throws IOException {
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        List<String[]> rows = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) {
                continue;
            }
            String[] parts = lines.get(i).split(",", 7);
            if (parts.length == 7) {
                rows.add(parts);
            }
        }
        return rows;
    }

    private static String clean(String value) {
        return value.replace(",", " ").replace("\n", " ");
    }
}
