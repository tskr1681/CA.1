package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.calculations.hydrogenize.Hydrogenize;
import chemaxon.formats.MolExporter;
import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ThreeDimensionalConverterStep implements PipelineStep<Candidate, Path> {

    private Path filePath;

    public ThreeDimensionalConverterStep(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public Path execute(Candidate candidate) throws PipelineException {
        Path conformerFileName = getConformerFileName(candidate);
        Path directory = conformerFileName.getParent();
        // Make directory if it does not exist
        if (! directory.toFile().exists()){
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {

                // Format exception method
                String exceptionMessage = String.format("Could not create directory '%s' for docking files",
                        directory.toString());
                // Throw pipeline exception
                throw new PipelineException(
                        exceptionMessage, e);
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
