/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

/**
 * A custom exception that should be thrown when pipeline related functionality caused an exception.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class PipelineException extends Exception {
    /**
     * Constructor for pipeline exception.
     *
     * @param message The exception message.
     */
    public PipelineException(String message) {
        super(message);
    }

    /**
     * Constructor for pipeline exception.
     *
     * @param message The exception message.
     * @param cause   The exception cause.
     */
    public PipelineException(String message, Exception cause) {
        super(message, cause);
    }
}
