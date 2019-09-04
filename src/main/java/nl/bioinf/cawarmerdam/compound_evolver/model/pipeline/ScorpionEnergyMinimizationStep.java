package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.nio.file.Path;

public class ScorpionEnergyMinimizationStep extends EnergyMinimizationStep {
    /**
     * Constructor for an energy minimization step.
     *
     * @param forceField     The force field that should be chosen in minimization.
     * @param anchorFilePath The path of the file that holds the anchor.
     */
    public ScorpionEnergyMinimizationStep(String forceField, Path anchorFilePath) {

        super(forceField, anchorFilePath);
    }

    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        // TODO: Run scorpion here
        return candidate;
    }
}
