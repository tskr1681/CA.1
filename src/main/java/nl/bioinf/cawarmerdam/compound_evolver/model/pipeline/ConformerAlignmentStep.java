/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolImporter;
import chemaxon.marvin.alignment.*;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.util.ConformerHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Conformer alignment step that uses the alignment functionality in the chemaxon package.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ConformerAlignmentStep implements PipelineStep<Candidate, Candidate> {

    private final Molecule referenceMolecule;
    private final boolean fast;
    private static final MMPAlignmentProperties mmpAlignmentProperties = new MMPAlignmentProperties(AlignmentProperties.FlexibilityMode.KEEP_FIRST_RIGID_SECOND_FLEXIBLE_EXTRA, AlignmentAccuracyMode.FAST, 0.3d);

    /**
     * Constructor for the conformer alignment step.
     *
     * @param anchor The path to the file that holds the anchor molecule.
     * @throws PipelineException If the anchor file could not be imported.
     */
    public ConformerAlignmentStep(Path anchor, boolean fast) throws PipelineException {
        this.referenceMolecule = importReferenceMolecule(anchor);
        this.fast = fast;
    }

    /**
     * Method responsible for importing the reference molecule from a given path.
     *
     * @param anchor The anchor file path.
     * @return the imported molecule
     * @throws PipelineException if the given anchor could not be imported.
     */
    private Molecule importReferenceMolecule(Path anchor) throws PipelineException {
        MolImporter importer;
        try {
            importer = new MolImporter(anchor.toFile());

            return importer.read();

        } catch (IOException e) {
            throw new PipelineException("Could not read reference fragment", e);
        }
    }

    /**
     * Executes the conformer alignment step.
     *
     * @param candidate The candidate whose conformers have to be aligned.
     * @return the candidate with aligned conformers.
     * @throws PipelineException if an exception occurred in the alignment step.
     */
    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        // Get the path of the file with the conformers
        Path conformersFilePath = candidate.getConformersFile();
        // Create the filename of the resulting file
        String fixedConformerFileName = "fixed-" + conformersFilePath.getFileName();
        // Sets the fixed conformer file path to the candidate
        candidate.setFixedConformersFile(conformersFilePath.resolveSibling(fixedConformerFileName));

        // Align and export the conformers
        List<Molecule> alignedMolecules = alignConformersFromPath(conformersFilePath);
        ConformerHelper.exportConformers(candidate.getFixedConformersFile(), alignedMolecules);
        return candidate;
    }

    /**
     * Method aligning conformers that are read from the given path.
     *
     * @param conformersPath The path that the conformers are read from.
     * @return the list of aligned conformers.
     * @throws PipelineException if the conformers could not be read or aligned.
     */
    @SuppressWarnings("deprecation")
    private List<Molecule> alignConformersFromPath(Path conformersPath) throws PipelineException {
        List<Molecule> alignedMolecules = new ArrayList<>();

        // Create importer for file
        MolImporter importer;
        try {
            importer = new MolImporter(conformersPath.toFile());

            // read the first molecule from the file
            Molecule m = importer.read();
            while (m != null) {
                if (!fast) {
                    MMPAlignmentResult result;
                    Molecule referenceP = MMPAlignment.preprocess(referenceMolecule, false);
                    Molecule toAlignP = MMPAlignment.preprocess(m, false);
                    MMPAlignment alignment = new MMPAlignment(referenceP, toAlignP, mmpAlignmentProperties);

                    Callable<MMPAlignmentResult> task = alignment::calculate;

                    FutureTask<MMPAlignmentResult> future = new FutureTask<>(task);
                    Thread t = new Thread(future);
                    t.start();
                    try {
                        result = future.get(50, TimeUnit.SECONDS);
                    } catch (TimeoutException | ExecutionException | InterruptedException ex) {
                        future.cancel(true);
                        t.stop();
                        throw new PipelineException("Alignment took too long, ", ex);
                    } finally {
                        future.cancel(true);
                        t.stop();
                    }
                    if (result != null) {
                        alignedMolecules.add(result.getMolecule2Aligned());
                    }
                } else {
                    Alignment alignment = new Alignment();

                    try {
                        alignment.addMolecule(referenceMolecule, false, false);
                        alignment.addMolecule(m, true, true);


                        Callable<Void> task = () -> {
                            alignment.align();
                            return null;
                        };

                        FutureTask<Void> future = new FutureTask<>(task);
                        Thread t = new Thread(future);
                        t.start();
                        try {
                            future.get(50, TimeUnit.SECONDS);
                        } catch (TimeoutException | ExecutionException | InterruptedException ex) {
                            future.cancel(true);
                            t.stop();
                            throw new PipelineException("Alignment took too long, ", ex);
                        } finally {
                            future.cancel(true);
                            t.stop();
                        }
                        alignedMolecules.add(alignment.getMoleculeWithAlignedCoordinates(1));
                    } catch (AlignmentException e) {
                        e.printStackTrace();
                    }
                }
                // read the next molecule from the input file
                m = importer.read();
            }
            importer.close();
        } catch (IOException e) {
            throw new PipelineException("Could not read conformers for alignment", e);
        }
        return alignedMolecules;
    }
}
