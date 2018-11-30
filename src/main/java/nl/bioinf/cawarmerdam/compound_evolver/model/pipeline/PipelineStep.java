package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

public interface PipelineStep<I, O> {

    O execute(I value) throws PipelineException;

    default <R> PipelineStep<I, R> pipe(PipelineStep<O, R> source) {
        return value -> source.execute(execute(value));
    }

    static <I, O> PipelineStep<I, O> of(PipelineStep<I, O> source) {
        return source;
    }
}