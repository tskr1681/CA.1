package nl.bioinf.cawarmerdam.compound_evolver.model;

public interface EvolutionProgressConnector {

    void handleNewGeneration(Generation generation);

    boolean isTerminationRequired();

    void putException(Exception exception);

    void setStatus(boolean isRunning);

}
