/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.util;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Class with a method that generates a csv from a list of lists.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class GenerateCsv {

    /**
     * Method that generates a csv from a list of lists.
     *
     * @param data The list of lists that has to be converted to a list.
     * @param lineSeparator The line separator to use.
     * @return a string of all items in the nested lists separated by ', ', which are separated by a line separator that
     * is given.
     */
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
