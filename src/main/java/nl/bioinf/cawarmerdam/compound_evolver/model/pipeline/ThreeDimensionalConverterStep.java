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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ThreeDimensionalConverterStep implements PipelineStep<Candidate, Candidate> {

    private Path filePath;
    private int conformerCount;

    public ThreeDimensionalConverterStep(Path filePath, int conformerCount) {
        this.filePath = filePath;
        this.conformerCount = conformerCount;
    }

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

    private Path getConformerFileName(Candidate candidate) {
        return Paths.get(filePath.toString(),
                String.valueOf(candidate.getIdentifier()),
                "conformers.sdf").toAbsolutePath();
    }

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
