/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public interface EvolutionProgressConnector {

    enum Status{RUNNING, FAILED, SUCCESS, STARTING}

    void handleNewGeneration(Generation generation);

    boolean isTerminationRequired();

    void putException(Exception exception);

    void setStatus(Status isRunning);

}
