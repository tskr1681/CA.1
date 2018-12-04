package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.ArrayList;
import java.util.List;

public class SessionEvolutionProgressConnector implements EvolutionProgressConnector {
    private List<Exception> exceptions = new ArrayList<>();
    private Status status;
    private List<Generation> generationBuffer = new ArrayList<>();
    private List<Generation> generations = new ArrayList<>();
    private boolean terminationRequired = false;

    public SessionEvolutionProgressConnector() {
        status = Status.STARTING;
    }

    public List<Generation> getGenerationBuffer() {
        ArrayList<Generation> oldBuffer = new ArrayList<>(generationBuffer);
        generationBuffer.clear();
        generations.addAll(oldBuffer);
        return oldBuffer;
    }

    public List<Generation> getGenerations() {
        return generations;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public void terminateEvolutionProgress() {
        this.terminationRequired = true;
    }

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
    public void setStatus(Status isRunning) {
        this.status = isRunning;
    }
}
