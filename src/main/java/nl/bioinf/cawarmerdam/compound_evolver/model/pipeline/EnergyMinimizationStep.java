/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import com.chemaxon.search.mcs.MaxCommonSubstructure;
import com.chemaxon.search.mcs.McsSearchOptions;
import com.chemaxon.search.mcs.McsSearchResult;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.ExclusionShape;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract class that specifies fields that a energy minimization step should have.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public abstract class EnergyMinimizationStep implements PipelineStep<Candidate, Candidate> {

    private String forceField;
    private Path anchorFilePath;

    /**
     * Constructor for an energy minimization step.
     *
     * @param forcefield The forcefield that should be chosen in minimization.
     * @param anchorFilePath The path of the file that holds the anchor.
     */
    EnergyMinimizationStep(String forcefield, Path anchorFilePath) {
        this.forceField = forcefield;
        this.anchorFilePath = anchorFilePath;
    }
}
