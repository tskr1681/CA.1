package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolExporter;
import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ThreeDimensionalConverter implements PipelineStep<Molecule, Path> {

    private int i;
    private Path filePath;

    public ThreeDimensionalConverter(Path filePath) {
        this.filePath = filePath;
        this.i = 0;
    }

    @Override
    public Path execute(Molecule molecule) {
        Path conformerFileName = getConformerFileName();
        System.out.println("conformerFileName = " + conformerFileName);
        try {
            Molecule[] conformers = createConformers(molecule);
            MolExporter exporter = new MolExporter(conformerFileName.toString(), "sdf");
            for (Molecule conformer : conformers) {
                exporter.write(conformer);
            }
        } catch (IOException | PluginException e) {
            e.printStackTrace();
        }
        return conformerFileName;
    }

    private Path getConformerFileName() {
        return Paths.get(filePath.toString(), String.format("%s.sdf", i++)).toAbsolutePath();
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
