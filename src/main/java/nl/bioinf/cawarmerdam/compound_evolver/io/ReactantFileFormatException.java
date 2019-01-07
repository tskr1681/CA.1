/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.io;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ReactantFileFormatException extends Exception {
    private final int lineNumber;
    private final String fileName;

    /**
     * Constructor for reactant file format exception.
     *
     * @param message The exception message.
     * @param lineNumber The line at which the format was not correct.
     * @param fileName The name of the file that contains the erroneous reactant.
     */
    ReactantFileFormatException(String message, int lineNumber, String fileName) {
        super(String.format("Error in file '%s', line %d: %s", fileName, lineNumber, message));
        this.lineNumber = lineNumber;
        this.fileName = fileName;
    }

    /**
     * Getter for the line number.
     *
     * @return the line number.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Getter for the file name.
     *
     * @return the file name.
     */
    public String getFileName() {
        return fileName;
    }
}
