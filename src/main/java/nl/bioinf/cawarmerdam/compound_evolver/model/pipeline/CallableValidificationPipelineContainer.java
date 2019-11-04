/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The pipeline container implements the callable interface so it can be called in multiple threads.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class CallableValidificationPipelineContainer implements Callable<List<Candidate>> {
    private final List<PipelineStep<Candidate, Candidate>> pipeline;
    private final Path pipelineOutputFilePath;
    private final List<Candidate> candidates;

    /**
     * Constructor of a callable pipeline container.
     *
     * @param pipeline The pipeline that has to be executed.
     * @param pipelineOutputFilePath The output where the pipeline writes to.
     * @param candidates The candidates that this container will score.
     */
    public CallableValidificationPipelineContainer(List<PipelineStep<Candidate, Candidate>> pipeline, Path pipelineOutputFilePath, List<Candidate> candidates) {
        this.pipeline = pipeline;
        this.pipelineOutputFilePath = pipelineOutputFilePath;
        this.candidates = candidates;
    }

    /**
     * Method responsible for executing the pipeline.
     *
     * @return void
     * @throws PipelineException if an exception occurred during the pipeline execution.
     */
    @Override
    public List<Candidate> call() throws PipelineException {
        List<Candidate> out = new ArrayList<>();
        System.out.println("candidates = " + candidates);
        for(int i = 0; i < candidates.size(); i++) {
            // Create new directory
            createCandidateDirectory(candidates.get(i));
            // Setting Level to ALL
            // Execute pipeline
            out.add(this.pipeline.get(i).execute(candidates.get(i)));
        }
        return out;
    }

    /**
     * Method that creates a directory for candidate specific files to reside in.
     *
     * @throws PipelineException If the directory could not be created.
     */
    private void createCandidateDirectory(Candidate candidate) throws PipelineException {
        Path directory = pipelineOutputFilePath.resolve(String.valueOf(candidate.getIdentifier()));
        // Make directory if it does not exist
        if (! directory.toFile().exists()){
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
    }
}
