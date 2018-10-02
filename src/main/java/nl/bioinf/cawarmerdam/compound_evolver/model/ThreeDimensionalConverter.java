package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;

public class ThreeDimensionalConverter implements PipelineStep<Molecule,Molecule[]> {

    @Override
    public Molecule[] execute(Molecule molecule) {
        // Use conformer plugin
        ConformerPlugin conformerPlugin = new ConformerPlugin();
        try {
            // Set parameters
            conformerPlugin.setMolecule(molecule);
            conformerPlugin.setMaxNumberOfConformers(100);
            conformerPlugin.setMMFF94(true);
            // Run
            conformerPlugin.run();
        } catch (PluginException e) {
            e.printStackTrace();
        }
//        Cleaner.clean(molecule, 3, "[ca]{15}{15}[mmff94]");
        return conformerPlugin.getConformers();
    }
}
