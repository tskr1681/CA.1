/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;

/**
 * Interface that specifies how the compound evolver can push data.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public interface EvolutionProgressConnector {

    /**
     * The status that can apply to the evolution process.
     */
    enum Status {RUNNING, FAILED, SUCCESS, STARTING}


    /**
     * Getter for the list of generations that are stored.
     *
     * @return the list of previously made generations not in the generations buffer.
     */
    List<Generation> getGenerations();

    /**
     * Method that should handle a newly scored generation.
     *
     * @param generation A new, scored generation.
     */
    void handleNewGeneration(Generation generation);

    /**
     * Method that returns if termination is required according to the connector.
     *
     * @return true if termination is required, false if not.
     */
    boolean isTerminationRequired();

    /**
     * Method that handles a new exception that was thrown in scoring a candidate.
     *
     * @param exception the thrown exception.
     */
    void putException(Exception exception);

    /**
     * Method that should be used to update the status of evolution.
     *
     * @param status The status of evolution.
     */
    void setStatus(Status status);

}
