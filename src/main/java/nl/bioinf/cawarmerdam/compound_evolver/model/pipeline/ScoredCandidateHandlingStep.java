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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ScoredCandidateHandlingStep implements PipelineStep<Candidate, Void> {

    private Path anchorFilePath;
    private ExclusionShape exclusionShape;

    public ScoredCandidateHandlingStep(Path anchorFilePath, Path receptorFilePath) throws PipelineException {
        this.anchorFilePath = anchorFilePath;
        try {
            Molecule receptor = new MolImporter(String.valueOf(receptorFilePath)).read();
            this.exclusionShape = new ExclusionShape(receptor);
        } catch (IOException e) {
            throw new PipelineException(
                    String.format("Could not import receptor molecule %s", receptorFilePath.getFileName()));
        }
    }

    @Override
    public Void execute(Candidate candidate) throws PipelineException {
        List<Double> conformerScores = candidate.getConformerScores();
        Path outputFilePath = candidate.getMinimizationOutputFilePath();
        double score = 0.0;
        if (conformerScores.size() > 0) {
            List<Double> nonClashingConformerScores = new ArrayList<>();
            List<Molecule> nonClashingConformers = new ArrayList<>();
            for (int i = 0; i < conformerScores.size(); i++) {
                Molecule conformer = getConformer(outputFilePath, i);
                boolean isInShape = exclusionShape.inShape(conformer);
                if (!isInShape) {
                    nonClashingConformerScores.add(conformerScores.get(i));
                    nonClashingConformers.add(conformer);
                }
            }
            if (nonClashingConformerScores.size() > 0) {
                score = Collections.min(nonClashingConformerScores);
                Molecule bestConformer = nonClashingConformers.get(nonClashingConformerScores.indexOf(score));
                candidate.setCommonSubstructureToAnchorRmsd(calculateLeastAnchorRmsd(bestConformer));
                exportConformer(bestConformer, outputFilePath.resolveSibling("best-conformer.sdf"));
            }
        }
        candidate.setRawScore(score);
        return null;
    }

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
            if (conformerIndex != currentConformerIndex) {
                throw new PipelineException(String.format("Could not obtain conformer %d from '%s'",
                        conformerIndex,
                        minimizedConformersFilePath.getFileName()));
            }
            return m;
        } catch (IOException e) {
            throw new PipelineException("Could not import minimized file", e);
        }
    }

    private void exportConformer(Molecule conformer, Path outputFilePath) throws PipelineException {
        try {
            MolExporter exporter = new MolExporter(outputFilePath.toString(), "sdf");
            exporter.write(conformer);
            exporter.close();
        } catch (IOException e) {
            throw new PipelineException("Could not write best conformer to file", e);
        }

    }
}
