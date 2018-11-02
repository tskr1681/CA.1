package nl.bioinf.cawarmerdam.compound_evolver.model;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

public class SessionEvolutionProgressConnector implements EvolutionProgressConnector {
    private List<Exception> exceptions;
    private boolean isRunning;
    private List<Generation> generationBuffer = new ArrayList<>();

    public SessionEvolutionProgressConnector() {
        isRunning = true;
    }

    @Override
    public void handleNewGeneration(Generation generation) {
        // Initialize arraylist
//        List<Generation> generationList = new ArrayList<>();

        // We don't know if the session attribute really is a list of generations, but it should be.

//        @SuppressWarnings("unchecked")
//        List<Generation> generation_buffer = (List<Generation>) session.getAttribute("generation_buffer");

//        // Include existing buffer if not null
//        if (generation_buffer != null) {
//            generationList.addAll(generation_buffer);
//        }

        // Add the new generation to the buffer
        generationBuffer.add(generation);
    }

    public List<Generation> emptyGenerationBuffer() {
        ArrayList<Generation> oldBuffer = new ArrayList<>(generationBuffer);
        generationBuffer.clear();
        return oldBuffer;
    }

    @Override
    public boolean isTerminationRequired() {
        return false;
    }

    @Override
    public void putException(Exception exception) {
        this.exceptions.add(exception);
    }

    @Override
    public void setStatus(boolean isRunning) {
        this.isRunning = isRunning;
    }
}
