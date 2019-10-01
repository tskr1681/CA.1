/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.marvin.plugin.PluginException;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.*;

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
    private final boolean cleanupFiles;

    /**
     * Constructor of a callable pipeline container.
     *
     * @param pipeline The pipeline that has to be executed.
     * @param pipelineOutputFilePath The output where the pipeline writes to.
     * @param candidate The candidate that this container will score.
     * @param cleanupFiles If this should remove temporary files.
     */
    public CallableValidificationPipelineContainer(PipelineStep<Candidate, Candidate> pipeline, Path pipelineOutputFilePath, Candidate candidate, boolean cleanupFiles) {
        this.pipeline = pipeline;
        this.pipelineOutputFilePath = pipelineOutputFilePath;
        this.candidate = candidate;
        this.cleanupFiles = cleanupFiles;
    }

    /**
     * Method responsible for executing the pipeline.
     *
     * @return void
     * @throws PipelineException if an exception occurred during the pipeline execution.
     * @throws PluginException if a Chemaxon plugin failed.
     * @throws IOException if an IO related exception occurred.
     */
    @Override
    public Candidate call() throws PipelineException {
        // Declare logging details
        Handler fileHandler = null;
        Formatter simpleFormatter;
        Path candidateDirectory = null;
        Candidate c;
        try {
            // Create new directory
            candidateDirectory = createCandidateDirectory();
            // Setting Level to ALL
            // Execute pipeline
            c = this.pipeline.execute(candidate);
        } catch (PipelineException e) {
            throw e;
        }
        return c;
    }

    /**
     * Method that creates a directory for candidate specific files to reside in.
     *
     * @return the directory path.
     * @throws PipelineException If the directory could not be created.
     */
    private Path createCandidateDirectory() throws PipelineException {
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
        return directory;
    }

    /**
     * Remove candidate specific files from the directory, excluding the log file.
     *
     * @param candidateDirectory The candidate specific directory.
     */
    private void removeCandidatePipelineFiles(Path candidateDirectory) {
        // Collect specific files.
        final File[] files = candidateDirectory.toFile().listFiles(
                (dir, name) -> !name.matches("^pipeline\\.log$"));
        // Remove all files
        if (files != null) {
            for ( final File file : files ) {
                if ( !file.delete() ) {
                    System.err.println( "Can't remove " + file.getAbsolutePath() );
                }
            }
        }
    }
}
