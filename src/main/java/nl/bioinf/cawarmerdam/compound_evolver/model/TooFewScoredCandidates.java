/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

/**
 * Exception that gets thrown whenever selection could not be performed.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class TooFewScoredCandidates extends Exception {
    /**
     * Constructor for the unselectable population exception.
     *
     * @param message The exception message.
     */
    public TooFewScoredCandidates(String message) {
        super(message);
    }
}
