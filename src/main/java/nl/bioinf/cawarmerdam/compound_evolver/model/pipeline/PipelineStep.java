/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public interface PipelineStep<I, O> {

    /**
     * Method that executes the task of the considered pipeline step.
     *
     * @param value The input, and possibly, simultaneously the output of the previous pipeline step's execute method.
     * @return the output, and possibly, simultaneously the input of the following pipeline step's execute method.
     * @throws PipelineException if a pipeline related exception occurred.
     */
    O execute(I value) throws PipelineException;

    default <R> PipelineStep<I, R> pipe(PipelineStep<O, R> source) {
        return value -> source.execute(execute(value));
    }

    static <I, O> PipelineStep<I, O> of(PipelineStep<I, O> source) {
        return source;
    }
}