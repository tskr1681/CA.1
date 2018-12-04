package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;

public interface EvolutionProgressConnector {

    enum Status{RUNNING, FAILED, SUCCESS, STARTING}

    void handleNewGeneration(Generation generation);

    boolean isTerminationRequired();

    void putException(Exception exception);

    void setStatus(Status isRunning);

}
