package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolExporter;
import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ThreeDimensionalConverterStep implements PipelineStep<Candidate, Path> {

    private Path filePath;

    public ThreeDimensionalConverterStep(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public Path execute(Candidate candidate) throws PipeLineException {
        Path conformerFileName = getConformerFileName(candidate);
        File directory = new File(String.valueOf(conformerFileName.getParent()));
        // Make directory if it does not exist
        if (! directory.exists()){
            boolean mkdirSuccess = directory.mkdir();
            // Throw an exception if making a new directory failed.
            if (!mkdirSuccess) {
                throw new PipeLineException("Failed to make directory");
            }
        }
        try {
            Molecule[] conformers = createConformers(candidate.getPhenotype());
            MolExporter exporter = new MolExporter(conformerFileName.toString(), "sdf");
            for (Molecule conformer : conformers) {
                exporter.write(conformer);
            }
        } catch (IOException | PluginException e) {
            e.printStackTrace();
        }
        return conformerFileName;
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
        conformerPlugin.setMaxNumberOfConformers(15);
        // Run
        conformerPlugin.run();
        return conformerPlugin.getConformers();
    }
}
