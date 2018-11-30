package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.marvin.plugin.PluginException;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class CallablePipelineContainer implements Callable<Void> {
    private static final Logger LOGGER = Logger.getLogger(CallablePipelineContainer.class.getName());
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
        candidate.calculateLigandLipophilicityEfficiency();
        return null;
    }
}
