package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.marvin.alignment.Alignment;
import chemaxon.marvin.alignment.AlignmentException;
import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;

public class ConformerRMSDCalculator {

    public static double getConformerRMSD(Molecule conformer) throws PluginException, AlignmentException {
        Molecule best_conformer = conformer;
        Molecule free_conformer = conformer;
        ConformerPlugin cp = new ConformerPlugin();
        cp.setMolecule(free_conformer);
        cp.setLowestEnergyConformerCalculation(true);
        cp.run();
        free_conformer = cp.getConformers()[0];
        Alignment ap = new Alignment();
        ap.addMolecule(best_conformer, false, true);
        ap.addMolecule(free_conformer, false, true);
        ap.align();
        best_conformer = ap.getMoleculeWithAlignedCoordinates(0);
        free_conformer = ap.getMoleculeWithAlignedCoordinates(1);
        return calculateRmsd(free_conformer, best_conformer);
    }

    private static double calculateRmsd(Molecule minimizedMolecule, Molecule anchorMolecule) {
        double deviations = 0;
        int atomCount = minimizedMolecule.getAtomCount();

        for (int i = 0; i < atomCount; i++) {

            // Get the atoms that match from the query a
            MolAtom anchorAtom = anchorMolecule.getAtom(i);
            MolAtom minimizedAtom = minimizedMolecule.getAtom(i);

            // Calculate square (pow(x,2) deviations for X, Y and Z coordinates.
            deviations +=
                    Math.pow(anchorAtom.getX() - minimizedAtom.getX(), 2) +
                            Math.pow(anchorAtom.getY() - minimizedAtom.getY(), 2) +
                            Math.pow(anchorAtom.getZ() - minimizedAtom.getZ(), 2);
        }
        return Math.sqrt(deviations / atomCount);
    }

}
