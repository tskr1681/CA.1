package nl.bioinf.cawarmerdam.compound_evolver.util;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class GenerateCsv {

    public static <T> String generateCsvFile(List<List<T>> data, String lineSeparator) {
        // Initialize joiner
        StringJoiner joiner = new StringJoiner(lineSeparator);

        // Loop through outer list
        for (List<T> objects : data) {

            // For turn the items of the inner list into a string and join these with a separator (comma)
            joiner.add(objects.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));
        }

        // Return the joined joiner object
        return joiner.toString();
    }
}
