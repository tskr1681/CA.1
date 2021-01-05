/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolExporter;
import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class that handles the creation of conformers from non-3D molecules.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ThreeDimensionalConverterStep implements PipelineStep<Candidate, Candidate> {

    private final Path filePath;
    private final int conformerCount;

    /**
     * Constructor for three dimensional converter step.
     *
     * @param filePath       The path that corresponds to the location of pipeline files for the entire run
     * @param conformerCount The amount of conformers that should be generated.
     */
    public ThreeDimensionalConverterStep(Path filePath, int conformerCount) {
        this.filePath = filePath;
        this.conformerCount = conformerCount;
    }

    /**
     * Method that executes the conformer creation step.
     *
     * @param candidate The candidate that needs conformers generated.
     * @return the candidate with conformers generated.
     * @throws PipelineException if the conformers could not be generated.
     */
    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        Path conformerFilePath = getConformerFileName(candidate);
        try {
            Molecule[] conformers = createConformers(candidate.getPhenotype());
            MolExporter exporter = new MolExporter(conformerFilePath.toString(), "sdf");
            for (Molecule conformer : conformers) {
                exporter.write(conformer);
            }
            exporter.close();
        } catch (IOException | PluginException e) {
            throw new PipelineException("Could not create conformers", e);
        }
        candidate.setConformersFile(conformerFilePath);
        return candidate;
    }

    /**
     * Method that retrieves the path for the conformers that are generated.
     *
     * @param candidate The candidate for which the file is meant.
     * @return the path for the conformers file.
     */
    private Path getConformerFileName(Candidate candidate) {
        return Paths.get(filePath.toString(),
                String.valueOf(candidate.getIdentifier()),
                "conformers.sdf").toAbsolutePath();
    }

    /**
     * Method that creates a set amount of conformers for the given molecule.
     *
     * @param molecule The molecule to create conformers from.
     * @return a list of conformers
     * @throws PluginException if the conformers could not be generated.
     */
    private Molecule[] createConformers(Molecule molecule) throws PluginException {
        // Use conformer plugin
        ConformerPlugin conformerPlugin = new ConformerPlugin();
        // Set parameters
        conformerPlugin.setMolecule(molecule);
        conformerPlugin.setMaxNumberOfConformers(conformerCount);
        // Run
        conformerPlugin.run();
        return conformerPlugin.getConformers();
    }
}
