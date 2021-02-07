/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolExporter;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The pipeline container implements the callable interface so it can be called in multiple threads.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class CallableFullPipelineContainer implements Callable<Void> {
    private final List<PipelineStep<Candidate, Void>> pipeline;
    private final Path pipelineOutputFilePath;
    private final List<Candidate> candidates;

    /**
     * Constructor of a callable pipeline container.
     *
     * @param pipeline               The pipeline that has to be executed.
     * @param pipelineOutputFilePath The output where the pipeline writes to.
     * @param candidates             The candidates that this container will score.
     */
    public CallableFullPipelineContainer(List<PipelineStep<Candidate, Void>> pipeline, Path pipelineOutputFilePath, List<Candidate> candidates) {
        this.pipeline = pipeline;
        this.pipelineOutputFilePath = pipelineOutputFilePath;
        this.candidates = candidates;
    }

    /**
     * Method responsible for executing the pipeline.
     *
     * @return void
     * @throws PipelineException if an exception occurred during the pipeline execution.
     * @throws PluginException   if a Chemaxon plugin failed.
     */
    @Override
    public Void call() throws PipelineException, PluginException {
        // Declare logging details
        for (int i = 0; i < candidates.size(); i++) {
            Path candidateDirectory = null;
            try {
                if (candidates.get(i).isScored())
                    return null;
                // Create new directory
                candidateDirectory = createCandidateDirectory(candidates.get(i));

                //Reset all the files in the candidate, to make sure they are properly validated
                candidates.get(i).setScoredConformersFile(null);
                candidates.get(i).setMinimizationOutputFilePath(null);
                candidates.get(i).setFixedConformersFile(null);
                candidates.get(i).setConformersFile(null);
                candidates.get(i).setConformerScores(null);

                // Execute pipeline
                MolExporter m = new MolExporter(new FileOutputStream(candidateDirectory.resolve("reactants.smiles").toFile()), "smiles");
                for (Molecule reactant : candidates.get(i).getReactants()) {
                    m.write(reactant);
                }
                m.close();
                this.pipeline.get(i).execute(candidates.get(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (candidates.get(i).isScored()) {
                candidates.get(i).calculateLigandEfficiency();
                candidates.get(i).calculateLigandLipophilicityEfficiency();
            }
        }
        return null;
    }

    /**
     * Method that creates a directory for candidate specific files to reside in.
     *
     * @return the directory path.
     * @throws PipelineException If the directory could not be created.
     */
    private Path createCandidateDirectory(Candidate candidate) throws PipelineException {
        Path directory = pipelineOutputFilePath.resolve(String.valueOf(candidate.getIdentifier()));
        // Make directory if it does not exist
        if (!directory.toFile().exists()) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {

                // Format exception method
                String exceptionMessage = String.format("Could not create directory '%s' for docking files",
                        directory.toString());
                // Throw pipeline exception
                throw new PipelineException(
                        exceptionMessage, e);
            }
        }
        return directory;
    }
}
