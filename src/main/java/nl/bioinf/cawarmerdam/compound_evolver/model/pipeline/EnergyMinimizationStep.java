package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import com.chemaxon.search.mcs.MaxCommonSubstructure;
import com.chemaxon.search.mcs.McsSearchOptions;
import com.chemaxon.search.mcs.McsSearchResult;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.nio.file.Path;

public abstract class EnergyMinimizationStep implements PipelineStep<Candidate, Void> {

    private String forceField;
    private Path anchorFilePath;

    public EnergyMinimizationStep(String forcefield, Path anchorFilePath) {
        this.forceField = forcefield;
        this.anchorFilePath = anchorFilePath;
    }

    double calculateLeastAnchorRmsd(Molecule minimizedMolecule) throws PipelineException {
        try {
            // Create importer for file
            MolImporter importer = new MolImporter(anchorFilePath.toFile());

            // read the first molecule from the file
            Molecule anchorMolecule = importer.read();

            // Find the most common sub structure of the two molecules.
            // This should be the substructure that was also used in aligning the molecule
            // with the anchor in the fixation step. In the current procedure however it is
            // possible that other substructures are found that are different.
            // To get the right one, we assume that the one with the lowest rmsd is the best.
            // Setting the order sensitivity to true makes sure that all possibilities are found.
            McsSearchOptions buildOptions = new McsSearchOptions.Builder()
                    .orderSensitive(true)
                    .bondTypeMatching(false).build(); // Set the bond type matching to false to allow differently
            // drawn ring structures to be found.

            MaxCommonSubstructure mcs = MaxCommonSubstructure.newInstance(buildOptions);
            mcs.setMolecules(anchorMolecule, minimizedMolecule);

            // Find the matching substructures
            McsSearchResult result = mcs.find();
            // Get the rmsd of the first match
            double leastRmsd = calculateRmsd(minimizedMolecule, anchorMolecule, result);
            // Get the rmsd of the other matches and assign leastRmsd if the rmsd is smaller than the current leastRmsd
            while (mcs.hasNextResult()) {
                result = mcs.nextResult();
                leastRmsd = Math.min(calculateRmsd(minimizedMolecule, anchorMolecule, result), leastRmsd);
            }
            return leastRmsd;
        } catch (IOException e) {
            throw new PipelineException("Could not import anchor file", e);
        }
    }

    private double calculateRmsd(Molecule minimizedMolecule, Molecule anchorMolecule, McsSearchResult result) throws PipelineException {
        int[] atomMapping = result.getAtomMapping();

        double deviations = 0;

        int atomCount = result.getAtomCount();
        if (atomCount == 0) throw new PipelineException("No common substructure found");

        for (int i = 0; i < atomMapping.length; i++) {

            // Get the atom that maps to atom i from the anchor
            int targetIndex = atomMapping[i];
            if (targetIndex == -1) continue;

            // Get the atoms that match from the query a
            MolAtom anchorAtom = anchorMolecule.getAtom(i);
            MolAtom minimizedAtom = minimizedMolecule.getAtom(targetIndex);

            // Calculate square (pow(x,2) deviations for X, Y and Z coordinates.
            deviations +=
                    Math.pow(anchorAtom.getX() - minimizedAtom.getX(), 2) +
                    Math.pow(anchorAtom.getY() - minimizedAtom.getY(), 2) +
                    Math.pow(anchorAtom.getZ() - minimizedAtom.getZ(), 2);
        }
        return Math.sqrt(deviations / atomCount);
    }

    Molecule getBestConformer(Path minimizedConformersFilePath, int bestConformerIndex) throws PipelineException {
        try {
            // Create importer for file
            MolImporter importer = new MolImporter(minimizedConformersFilePath.toFile());

            // Current conformer index
            int conformerIndex = 0;

            // read the first molecule from the file
            Molecule m = importer.read();
            while (m != null && !(bestConformerIndex == conformerIndex)) {
                conformerIndex += 1;

                // read the next molecule from the input file
                m = importer.read();
            }
            importer.close();
            if (bestConformerIndex != conformerIndex) {
                throw new PipelineException(String.format("Could not obtain conformer %d from '%s'",
                        bestConformerIndex,
                        minimizedConformersFilePath.getFileName()));
            }
            return m;
        } catch (IOException e) {
            throw new PipelineException("Could not import minimized file", e);
        }
    }

    void exportConformer(Molecule conformer, Path outputFilePath) throws PipelineException {
        try {
            MolExporter exporter = new MolExporter(outputFilePath.toString(), "sdf");
            exporter.write(conformer);
            exporter.close();
        } catch (IOException e) {
            throw new PipelineException("Could not write best conformer to file", e);
        }

    }
}
