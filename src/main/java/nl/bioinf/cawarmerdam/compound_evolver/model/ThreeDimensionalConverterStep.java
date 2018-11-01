package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolExporter;
import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ThreeDimensionalConverterStep implements PipelineStep<Candidate, Path> {

    private Path filePath;

    public ThreeDimensionalConverterStep(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public Path execute(Candidate candidate) {
        Path conformerFileName = getConformerFileName(candidate);
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
        return Paths.get(filePath.toString(), String.format("%s.sdf", candidate.getIdentifier())).toAbsolutePath();
    }

    private Molecule[] createConformers(Molecule molecule) throws PluginException {
        // Use conformer plugin
        ConformerPlugin conformerPlugin = new ConformerPlugin();
        // Set parameters
        conformerPlugin.setMolecule(molecule);
        conformerPlugin.setMaxNumberOfConformers(15);
        conformerPlugin.setMMFF94(true);
        // Run
        conformerPlugin.run();
        return conformerPlugin.getConformers();
    }
}
