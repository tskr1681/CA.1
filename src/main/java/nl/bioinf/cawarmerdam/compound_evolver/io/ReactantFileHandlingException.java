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
public class ReactantFileHandlingException extends Exception {
    private final String fileName;

    /**
     * Constructor for reactant file handling exception.
     *
     * @param message The exception message.
     * @param fileName The filename that corresponds to the exception.
     */
    ReactantFileHandlingException(String message, String fileName) {
        super(message);
        this.fileName = fileName;
    }

    /**
     * Getter for the filename.
     *
     * @return the filename.
     */
    public String getFileName() {
        return fileName;
    }
}
