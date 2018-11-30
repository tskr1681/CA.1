package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;

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

    @Override
    public void addScores(List<Double> normFitnesses) {

    }
}