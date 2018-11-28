package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.marvin.plugin.PluginException;

import java.util.concurrent.Callable;

public class CallablePipelineContainer implements Callable<Void> {
    private PipelineStep<Candidate, Void> pipeline;
    private Candidate candidate;

    public CallablePipelineContainer(PipelineStep<Candidate, Void> pipeline, Candidate candidate) {
        this.pipeline = pipeline;
        this.candidate = candidate;
    }

    @Override
    public Void call() throws PipelineException, PluginException {
        System.out.println(Thread.currentThread().getName()+" Start. Candidate identifier = "+candidate.getIdentifier());
        this.pipeline.execute(candidate);
        candidate.calculateLigandEfficiency();
        return null;
    }
}
