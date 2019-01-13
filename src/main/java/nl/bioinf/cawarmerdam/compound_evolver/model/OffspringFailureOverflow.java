/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;

/**
 * Exception that should be thrown whenever the creation of new offspring has failed too many times.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class OffspringFailureOverflow extends Exception {
    private final List<String> offspringRejectionMessages;

    /**
     * Constructor for the offspring failure overflow.
     *
     * @param message The exception message.
     * @param offspringRejectionMessages The messages that are created whenever new offspring fails.
     */
    OffspringFailureOverflow(String message, List<String> offspringRejectionMessages) {
        super(message);
        this.offspringRejectionMessages = offspringRejectionMessages;
    }

    /**
     * Getter for the messages that are created whenever new offspring fails.
     *
     * @return messages that are created whenever new offspring fails.
     */
    public List<String> getOffspringRejectionMessages() {
        return offspringRejectionMessages;
    }
}
