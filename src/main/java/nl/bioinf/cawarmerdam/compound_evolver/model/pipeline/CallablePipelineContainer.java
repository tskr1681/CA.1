package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.marvin.plugin.PluginException;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.*;

public class CallablePipelineContainer implements Callable<Void> {
    private PipelineStep<Candidate, Void> pipeline;
    private Path pipelineOutputFilePath;
    private Candidate candidate;

    public CallablePipelineContainer(PipelineStep<Candidate, Void> pipeline, Path pipelineOutputFilePath, Candidate candidate) {
        this.pipeline = pipeline;
        this.pipelineOutputFilePath = pipelineOutputFilePath;
        this.candidate = candidate;
    }

    @Override
    public Void call() throws PipelineException, PluginException, IOException {

        Handler fileHandler = null;
        Formatter simpleFormatter = null;
        try {
            Path candidateDirectory = createCandidateDirectory();
            // Creating FileHandler
            fileHandler = new FileHandler(candidateDirectory.resolve("pipeline.log").toString());
            // Creating SimpleFormatter
            simpleFormatter = new SimpleFormatter();
            // Setting formatter to the handler
            fileHandler.setFormatter(simpleFormatter);
            // Assigning handler to logger
            candidate.getPipelineLogger().addHandler(fileHandler);
            candidate.getPipelineLogger().setUseParentHandlers(false);
            // Setting Level to ALL
            fileHandler.setLevel(Level.ALL);
            candidate.getPipelineLogger().setLevel(Level.ALL);
            candidate.getPipelineLogger().info("Starting scoring pipeline for candidate " + candidate.getIdentifier());
            this.pipeline.execute(candidate);
        } catch (PipelineException | IOException e) {
            candidate.getPipelineLogger().log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        candidate.calculateLigandEfficiency();
        candidate.calculateLigandLipophilicityEfficiency();
        return null;
    }

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
}
