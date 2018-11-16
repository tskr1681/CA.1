package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.Molecule;

import java.util.concurrent.Callable;

public class CallablePipelineContainer implements Callable<Void> {
    private PipelineStep<Candidate, Double> pipeline;
    private Candidate candidate;

    public CallablePipelineContainer(PipelineStep<Candidate, Double> pipeline, Candidate candidate) {
        this.pipeline = pipeline;
        this.candidate = candidate;
    }

    @Override
    public Void call() throws PipeLineException {
        System.out.println(Thread.currentThread().getName()+" Start. Candidate identifier = "+candidate.getIdentifier());
        double score = this.pipeline.execute(candidate);
        candidate.setScore(score);
        return null;
    }
}
