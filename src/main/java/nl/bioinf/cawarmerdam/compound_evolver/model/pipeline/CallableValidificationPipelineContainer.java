/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * The pipeline container implements the callable interface so it can be called in multiple threads.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class CallableValidificationPipelineContainer implements Callable<Candidate> {
    private final PipelineStep<Candidate, Candidate> pipeline;
    private final Path pipelineOutputFilePath;
    private final Candidate candidate;

    /**
     * Constructor of a callable pipeline container.
     *
     * @param pipeline The pipeline that has to be executed.
     * @param pipelineOutputFilePath The output where the pipeline writes to.
     * @param candidate The candidate that this container will score.
     */
    public CallableValidificationPipelineContainer(PipelineStep<Candidate, Candidate> pipeline, Path pipelineOutputFilePath, Candidate candidate) {
        this.pipeline = pipeline;
        this.pipelineOutputFilePath = pipelineOutputFilePath;
        this.candidate = candidate;
    }

    /**
     * Method responsible for executing the pipeline.
     *
     * @return void
     * @throws PipelineException if an exception occurred during the pipeline execution.
     */
    @Override
    public Candidate call() throws PipelineException {
        Candidate c;
        // Create new directory
        createCandidateDirectory();
        // Setting Level to ALL
        // Execute pipeline
        c = this.pipeline.execute(candidate);
        return c;
    }

    /**
     * Method that creates a directory for candidate specific files to reside in.
     *
     * @throws PipelineException If the directory could not be created.
     */
    private void createCandidateDirectory() throws PipelineException {
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
