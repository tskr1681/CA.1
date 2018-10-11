package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.nio.file.Path;

public abstract class EnergyMinimizationStep implements PipelineStep<Path, Path> {

    private String forceField;

    public EnergyMinimizationStep(String forcefield) {
        this.forceField = forcefield;
    }
}