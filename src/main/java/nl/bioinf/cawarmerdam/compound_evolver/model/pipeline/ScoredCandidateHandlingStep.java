/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.ExclusionShape;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A pipeline step that handles scored candidates.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ScoredCandidateHandlingStep implements PipelineStep<Candidate, Void> {

    private final Path anchorFilePath;
    private final Map<Long, Integer> clashingConformerCounter;
    private Map<Long, Integer> tooDistantConformerCounter;
    private ExclusionShape exclusionShape;
    private double maximumDistanceFromAnchor;

    /**
     * Constructor for a scored candidate handling step.
     *
     * @param anchorFilePath The path to the file that holds the anchor.
     * @param receptorFilePath The path of the file that holds the receptor.
     * @param exclusionShapeTolerance The tolerance of the exclusion shape in Ångström. default is 0.
     * @param maximumDistanceFromAnchor The maximum distance the anchor may be from the anchor matching structure
     *                                  in a conformer.
     * @param clashingConformerCounter A map that is used to count the amount of conformers clashing.
     * @param tooDistantConformerCounter A map that is used to count the amount of conformers that are too distant
     *                                   from the anchor.
     * @throws PipelineException if the molecules could not be imported.
     */
    public ScoredCandidateHandlingStep(Path anchorFilePath,
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

    /**
     * Executes the scored candidate handling step.
     *
     * @param candidate The candidate that is scored.
     * @return void
     * @throws PipelineException if the candidates conformers could not be handled.
     */
    @Override
    public Void execute(Candidate candidate) throws PipelineException {
        List<Double> conformerScores = candidate.getConformerScores();
        Path outputFilePath = candidate.getMinimizationOutputFilePath();
        // Declare default score
        if (conformerScores.size() > 0) {

            // Filter conformers that clash according to the exclusion shape.
            List<Double> validConformerScores = new ArrayList<>();
            List<Molecule> conformers = new ArrayList<>();
            for (int i = 0; i < conformerScores.size(); i++) {
                Molecule conformer = getConformer(outputFilePath, i);
                conformers.add(conformer);
            }
                // Get best conformer.
                double score = Collections.min(conformerScores);
                Molecule bestConformer = conformers.get(conformerScores.indexOf(score));
                exportConformer(bestConformer, outputFilePath.resolveSibling("best-conformer.sdf"));
                candidate.setRawScore(score);
        }
        return null;
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

    /**
     * Method that exports a molecule (conformer).
     *
     * @param conformer The conformer to export.
     * @param outputFilePath The path to the file to write the conformer to.
     * @throws PipelineException if the conformer could not be written.
     */
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
