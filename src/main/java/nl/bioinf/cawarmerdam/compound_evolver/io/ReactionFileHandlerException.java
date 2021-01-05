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
public class ReactionFileHandlerException extends Exception {
    private final String fileName;

    /**
     * Constructor of reaction file handler exception.
     *
     * @param message  The exception message.
     * @param fileName The file name that corresponds to the exception.
     */
    ReactionFileHandlerException(String message, String fileName) {
        super(message);
        this.fileName = fileName;
    }

    /**
     * Getter for the file name.
     *
     * @return The file name.
     */
    public String getFileName() {
        return fileName;
    }
}
