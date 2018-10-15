package nl.bioinf.cawarmerdam.compound_evolver.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class EneFileParser {
    public static List<Double> parseEneFile(InputStream eneFileInputStream, String fastaFileName)
            throws IllegalArgumentException {
        // Prepare list of DNA sequences, and variable to record what sequence is handled at all times
        List<Double> scores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(eneFileInputStream, Charset.defaultCharset()))) {
            String lineFromFile;
            StringBuilder stringBuilder = new StringBuilder();
            // Parse fasta
            while ((lineFromFile = reader.readLine()) != null) {
                // If line starts with '>' a header is encountered
                if (lineFromFile.startsWith(" 1")) {
                    // Get score after this
                    String[] split = lineFromFile.split("\\\\s+");
                    scores.add(Double.parseDouble(split[1]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scores;
    }
}
