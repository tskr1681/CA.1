package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.Molecule;

public class PipelineExecutor implements Runnable {
    private PipelineStep<Candidate, Double> pipeline;
    private Candidate candidate;

    public PipelineExecutor(PipelineStep<Candidate, Double> pipeline, Candidate candidate) {
        this.pipeline = pipeline;
        this.candidate = candidate;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName()+" Start. Candidate identifier = "+candidate.getIdentifier());
        double score = -this.pipeline.execute(candidate);
        candidate.setScore(score);
    }
}
