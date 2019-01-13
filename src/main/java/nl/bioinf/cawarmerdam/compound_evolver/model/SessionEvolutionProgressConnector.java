/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that is used as a connector between the evolution and a servlet.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class SessionEvolutionProgressConnector implements EvolutionProgressConnector {
    private List<Exception> exceptions = new ArrayList<>();
    private Status status;
    private List<Generation> generationBuffer = new ArrayList<>();
    private List<Generation> generations = new ArrayList<>();
    private boolean terminationRequired = false;

    /**
     * Constructor for the session evolution progress connector.
     * It starts with status 'starting'
     */
    public SessionEvolutionProgressConnector() {
        status = Status.STARTING;
    }

    /**
     * Empties, the list of recently made generations and stores them in the list of other generations and returns them.
     *
     * @return the list of recently made generations.
     */
    public List<Generation> getGenerationBuffer() {
        ArrayList<Generation> oldBuffer = new ArrayList<>(generationBuffer);
        generationBuffer.clear();
        generations.addAll(oldBuffer);
        return oldBuffer;
    }

    /**
     * Getter for the list of generations that are stored.
     *
     * @return the list of previously made generations not in the generations buffer.
     */
    public List<Generation> getGenerations() {
        return generations;
    }

    /**
     * Getter for the list of exceptions that where thrown while scoring candidates.
     *
     * @return the list of exceptions that where thrown while scoring candidates.
     */
    public List<Exception> getExceptions() {
        return exceptions;
    }

    /**
     * Method that sets the termination to be required. Evolution will act according to this when a new generation is
     * tried.
     */
    public void terminateEvolutionProgress() {
        this.terminationRequired = true;
    }

    /**
     * Getter for the last set status of evolution.
     *
     * @return the last set status of evolution.
     */
    public Status getStatus() {
        return this.status;
    }

    @Override
    public void handleNewGeneration(Generation generation) {
        generationBuffer.add(generation);
    }

    @Override
    public boolean isTerminationRequired() {
        return terminationRequired;
    }

    @Override
    public void putException(Exception exception) {
        this.exceptions.add(exception);
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }
}
