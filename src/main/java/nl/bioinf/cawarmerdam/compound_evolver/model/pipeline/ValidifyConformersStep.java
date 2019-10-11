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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidifyConformersStep implements PipelineStep<Candidate, Candidate> {

    private final Path anchorFilePath;
    private final Map<Long, Integer> clashingConformerCounter;
    private Map<Long, Integer> tooDistantConformerCounter;
    private ExclusionShape exclusionShape;
    private double maximumDistanceFromAnchor;

    public ValidifyConformersStep(Path anchorFilePath,
                                       Path receptorFilePath,
                                       double exclusionShapeTolerance,
                                       double maximumDistanceFromAnchor,
                                       Map<Long, Integer> clashingConformerCounter,
                                       Map<Long, Integer> tooDistantConformerCounter) throws PipelineException {
        this.anchorFilePath = anchorFilePath;
        this.clashingConformerCounter = clashingConformerCounter;
        this.tooDistantConformerCounter = tooDistantConformerCounter;
        this.maximumDistanceFromAnchor = maximumDistanceFromAnchor;
        try {
            Molecule receptor = new MolImporter(String.valueOf(receptorFilePath)).read();
            this.exclusionShape = new ExclusionShape(receptor, exclusionShapeTolerance);
        } catch (IOException e) {
            throw new PipelineException(
                    String.format("Could not import receptor molecule %s", receptorFilePath.getFileName()));
        }
    }

    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        List<Molecule> validConformers = new ArrayList<>();
        Path outputFilePath = candidate.getFixedConformersFile();

        int conformer_count = 15;
        try {
           conformer_count  = getConformerCount(outputFilePath);
        } catch(IOException ignored) {

        }
        for (int i = 0; i < conformer_count; i++) {
            Molecule conformer = getConformer(outputFilePath, i);
            boolean isTooDistant = calculateLeastAnchorRmsd(conformer) > maximumDistanceFromAnchor;
            boolean isInShape = exclusionShape.inShape(conformer);

            if (!isInShape && !isTooDistant) {
                validConformers.add(conformer);
            } else {
                countInvalidities(candidate, isTooDistant, isInShape);
            }
        }
        if (validConformers.size() > 0) {
            exportValidConformers(outputFilePath, validConformers);
            System.out.println("candidate = " + candidate);
            return candidate;
        } else {
            return null;
        }
    }

    /**
     * Method exporting a list of molecules to the given file path.
     *
     * @param fixedConformersPath The file path that the list of molecules should be written to.
     * @param alignedMolecules The list of molecule to write.
     * @throws PipelineException if the molecules could not be exported.
     */
    private void exportValidConformers(Path fixedConformersPath, List<Molecule> alignedMolecules)
            throws PipelineException {

        // Create exporter
        MolExporter exporter;
        try {
            // Initialize exporter
            exporter = new MolExporter(fixedConformersPath.toString(), "sdf");

            // Loop through the conformers by using conformer count
            for (Molecule molecule : alignedMolecules) {
                // Do plus 1 to only pick conformers and not the reference
                exporter.write(molecule);
            }
            // Close the exporter
            exporter.close();
        } catch (IOException e) {
            throw new PipelineException("Could not export aligned conformers", e);
        }
    }

    /**
     * Counts a too distant conformer or a conformer that is in the restricted space.
     *
     * @param candidate The candidate that the conformer belongs to.
     * @param isTooDistant If the conformer is too distant.
     * @param isInShape If the conformer is in the excluded shape.
     */
    private void countInvalidities(Candidate candidate, boolean isTooDistant, boolean isInShape) {
        if (isInShape) {
            // Count the clashing conformer.
            clashingConformerCounter.compute(
                    candidate.getIdentifier(), (key, oldValue) -> ((oldValue == null) ? 1 : oldValue+1));
        }
        if (isTooDistant) {
            tooDistantConformerCounter.compute(
                    candidate.getIdentifier(), (key, oldValue) -> ((oldValue == null) ? 1 : oldValue+1));
        }
    }

    /**
     * Calculate the least RMSD between the anchor and the substructure of the given molecule that matches the
     * anchor best.
     *
     * @param minimizedMolecule The molecule to calculate the RMSD from.
     * @return the RMSD.
     * @throws PipelineException if the molecule could not be imported.
     */
    private double calculateLeastAnchorRmsd(Molecule minimizedMolecule) throws PipelineException {
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

    /**
     * Calculates the RMSD between the minimized molecules best to the anchor matching substructure
     * and the anchor molecule.
     *
     * @param minimizedMolecule The molecule with a substructure (partially matching) to the anchor molecule.
     * @param anchorMolecule The anchor molecule.
     * @param result The maximum common substructure search result.
     * @return the RMSD.
     * @throws PipelineException if no substructure was found in the minimized molecule.
     */
    private double calculateRmsd(Molecule minimizedMolecule, Molecule anchorMolecule, McsSearchResult result)
            throws PipelineException {
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

    /**
     * Reads a conformer from a file at the specified index.
     *
     * @param minimizedConformersFilePath The path to the file that holds multiple conformers.
     * @param conformerIndex The index at which the conformer resides (zero-indexed).
     * @return the conformers molecule instance.
     * @throws PipelineException if the conformer could not be obtained, or the minimized conformer file could not be
     * imported.
     */
    private Molecule getConformer(Path minimizedConformersFilePath, int conformerIndex) throws PipelineException {
        try {
            // Create importer for file
            MolImporter importer = new MolImporter(minimizedConformersFilePath.toFile());

            // Current conformer index
            int currentConformerIndex = 0;

            // read the first molecule from the file
            Molecule m = importer.read();
            while (m != null && !(conformerIndex == currentConformerIndex)) {
                currentConformerIndex += 1;

                // read the next molecule from the input file
                m = importer.read();
            }
            importer.close();
            // The while loop should exit whenever the considered conformer index is reached. If the two indices
            // are not equal something went wrong (index is probably out of range) and an exception should be thrown.
            if (conformerIndex != currentConformerIndex) {
                throw new PipelineException(String.format("Could not obtain conformer %d from '%s'",
                        conformerIndex,
                        minimizedConformersFilePath.getFileName()));
            }
            // Return the molecule
            return m;
        } catch (IOException e) {
            throw new PipelineException("Could not import minimized file", e);
        }
    }

    private int getConformerCount(Path conformerPath) throws IOException {
        MolImporter importer = new MolImporter(conformerPath.toFile());

        // Current conformer index
        int currentConformerIndex = 0;

        // read the first molecule from the file
        Molecule m = importer.read();
        while (m != null) {
            currentConformerIndex += 1;

            // read the next molecule from the input file
            m = importer.read();
        }
        importer.close();
        return currentConformerIndex;
    }
}
