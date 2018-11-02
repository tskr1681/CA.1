package nl.bioinf.cawarmerdam.compound_evolver.model;

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
    public void setStatus(boolean isRunning) {
        // Do something
    }
}
