/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.io;

import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.PipelineException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to parse Ene files into a list of doubles
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
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
            // Throw exception
            throw new PipelineException(String.format("Could not read ENE file %s", eneFileName), e);
        }
        return scores;
    }
}
