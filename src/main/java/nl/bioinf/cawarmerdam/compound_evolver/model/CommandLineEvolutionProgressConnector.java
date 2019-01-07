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
public class CommandLineEvolutionProgressConnector implements EvolutionProgressConnector {
    @Override
    public void handleNewGeneration(Generation generation) {
        // Do nothing
    }

    @Override
    public boolean isTerminationRequired() {
        return false;
    }

    @Override
    public void putException(Exception exception) {
        // Do something
    }

    @Override
    public void setStatus(Status isRunning) {
        // Do something
    }

}
