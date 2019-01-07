/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class OffspringFailureOverflow extends Exception {
    private List<String> offspringRejectionMessages;

    public OffspringFailureOverflow(String message, List<String> offspringRejectionMessages) {
        super(message);
        this.offspringRejectionMessages = offspringRejectionMessages;
    }

    public List<String> getOffspringRejectionMessages() {
        return offspringRejectionMessages;
    }
}
