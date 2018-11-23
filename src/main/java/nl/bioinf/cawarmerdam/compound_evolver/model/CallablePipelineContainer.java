package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.marvin.plugin.PluginException;

import java.util.concurrent.Callable;

public class CallablePipelineContainer implements Callable<Void> {
    private PipelineStep<Candidate, Double> pipeline;
    private Candidate candidate;

    public CallablePipelineContainer(PipelineStep<Candidate, Double> pipeline, Candidate candidate) {
        this.pipeline = pipeline;
        this.candidate = candidate;
    }

    @Override
    public Void call() throws PipelineException, PluginException {
        System.out.println(Thread.currentThread().getName()+" Start. Candidate identifier = "+candidate.getIdentifier());
        double score = this.pipeline.execute(candidate);
        candidate.setRawScore(score);
        candidate.calculateLigandEfficiency();
        return null;
    }
}
