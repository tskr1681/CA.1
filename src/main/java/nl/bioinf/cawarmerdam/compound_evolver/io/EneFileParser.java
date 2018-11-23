package nl.bioinf.cawarmerdam.compound_evolver.io;

import nl.bioinf.cawarmerdam.compound_evolver.model.PipelineException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EneFileParser {
    public static List<Double> parseEneFile(InputStream eneFileInputStream, String eneFileName)
            throws PipelineException {
        // Prepare list of DNA sequences, and variable to record what sequence is handled at all times
        List<Double> scores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(eneFileInputStream, Charset.defaultCharset()))) {
            String lineFromFile;
            while ((lineFromFile = reader.readLine()) != null) {
                // If line starts with ' 1' the next text is the score
                if (lineFromFile.startsWith(" 1")) {
                    // Get score after this
                    // Only the characters 5 (0 based, inclusive) until the character 10 (exclusive) can contain a score
                    String score = lineFromFile.substring(2, 10);
                    scores.add(Double.parseDouble(score));
                }
            }
        } catch (IOException e) {
            throw new PipelineException(String.format("Could not read ENE file %s", eneFileName), e);
        }
        return scores;
    }
}
