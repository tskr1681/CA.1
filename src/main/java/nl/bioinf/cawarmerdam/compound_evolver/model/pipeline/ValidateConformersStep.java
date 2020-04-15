package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolImporter;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import com.chemaxon.search.mcs.MaxCommonSubstructure;
import com.chemaxon.search.mcs.McsSearchOptions;
import com.chemaxon.search.mcs.McsSearchResult;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.ExclusionShape;
import nl.bioinf.cawarmerdam.compound_evolver.util.ConformerHelper;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidateConformersStep implements PipelineStep<Candidate, Candidate> {

    private final Path anchorFilePath;
    private final Map<Long, Integer> clashingConformerCounter;
    private Map<Long, Integer> tooDistantConformerCounter;
    private ExclusionShape exclusionShape;
    private final double maximumDistanceFromAnchor;
    private boolean deleteInvalid;

    public ValidateConformersStep(Path anchorFilePath,
                                  Path receptorFilePath,
                                  double exclusionShapeTolerance,
                                  double maximumDistanceFromAnchor,
                                  Map<Long, Integer> clashingConformerCounter,
                                  Map<Long, Integer> tooDistantConformerCounter,
                                  boolean deleteInvalid) throws PipelineException {
        this.anchorFilePath = anchorFilePath;
        this.clashingConformerCounter = clashingConformerCounter;
        this.tooDistantConformerCounter = tooDistantConformerCounter;
        this.maximumDistanceFromAnchor = maximumDistanceFromAnchor;
        this.deleteInvalid = deleteInvalid;
        try {
            Molecule receptor = new MolImporter(receptorFilePath.toFile(), "pdb").read();
            this.exclusionShape = new ExclusionShape(receptor, exclusionShapeTolerance);
        } catch (IOException e) {
            e.printStackTrace();
            throw new PipelineException(
                    String.format("Could not import receptor molecule %s", receptorFilePath.getFileName()));
        }
    }

    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        List<Molecule> validConformers = new ArrayList<>();
        Path outputFilePath = candidate.getMinimizationOutputFilePath() == null ? candidate.getFixedConformersFile() : candidate.getMinimizationOutputFilePath();
        List<Double> scores = candidate.getConformerScores();
        List<Double> new_scores = new ArrayList<>();
        int conformer_count = 15;
        try {
            if (scores == null) {
                conformer_count = ConformerHelper.getConformerCount(outputFilePath);
            } else {
                conformer_count = scores.size();
            }
        } catch(IOException ignored) {

        }
        for (int i = 0; i < conformer_count; i++) {
            Molecule conformer = ConformerHelper.getConformer(outputFilePath, i);
            if (conformer == null) {
                throw new PipelineException("Got null as conformer!");
            }
            removeMarker(conformer);
            boolean isTooDistant = calculateLeastAnchorRmsd(conformer) > maximumDistanceFromAnchor;
            boolean isInShape = exclusionShape.inShape(conformer);

            if (!isInShape && !isTooDistant) {
                validConformers.add(conformer);
                if (scores != null) {
                    new_scores.add(scores.get(i));
                }
            } else {
                countInvalidities(candidate, isTooDistant, isInShape);
            }
        }
        if (validConformers.size() > 0) {
            ConformerHelper.exportConformers(outputFilePath, validConformers);
            if (scores != null) {
                candidate.setScoredConformersFile(outputFilePath);
                candidate.setConformerScores(new_scores);
            }
            return candidate;
        } else if (deleteInvalid) {
            try {
                FileUtils.deleteDirectory(candidate.getConformersFile().getParent().toFile());
            } catch (IOException exception) {
                System.err.println(exception.getMessage());
            }

        }
        return null;
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
            removeMarker(anchorMolecule);

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


    private void removeMarker(Molecule m) {
        ArrayList<Integer> toRemove = new ArrayList<>();
        MolAtom[] atomArray = m.getAtomArray();
        for(int i = 0; i < atomArray.length; i++) {
            if (atomArray[i].getAtno() == 118) {
                toRemove.add(i);
            }
        }
        for (int i:toRemove) {
            m.removeAtom(i);
        }
    }

}
