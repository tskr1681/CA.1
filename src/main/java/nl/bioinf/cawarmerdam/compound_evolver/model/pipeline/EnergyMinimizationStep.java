/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.nio.file.Path;

/**
 * Abstract class that specifies fields that a energy minimization step should have.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@Deprecated
public abstract class EnergyMinimizationStep implements PipelineStep<Candidate, Candidate> {

    private final String forceField;
    private final Path anchorFilePath;

    /**
     * Constructor for an energy minimization step.
     *
     * @param forceField     The force field that should be chosen in minimization.
     * @param anchorFilePath The path of the file that holds the anchor.
     */
    EnergyMinimizationStep(String forceField, Path anchorFilePath) {
        this.forceField = forceField;
        this.anchorFilePath = anchorFilePath;
    }

}
