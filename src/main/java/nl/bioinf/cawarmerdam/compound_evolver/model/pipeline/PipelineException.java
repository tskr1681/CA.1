package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

public class PipelineException extends Exception {
    public PipelineException(String message) {
        super(message);
    }

    public PipelineException(String message, Exception cause) {
        super(message, cause);
    }
}