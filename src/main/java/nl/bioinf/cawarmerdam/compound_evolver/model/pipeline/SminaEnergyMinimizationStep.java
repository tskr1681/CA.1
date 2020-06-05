/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import org.apache.tika.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimization step that uses Smina.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class SminaEnergyMinimizationStep implements PipelineStep<Candidate, Candidate> {
    private final Path receptorFilePath;
    private final String sminaExecutable;
    private boolean debug;

    /**
     * Constructor for smina energy minimization step.
     *
     * @param receptorFilePath The path of the file that holds the receptor.
     * @param sminaExecutable The path to the executable of Smina.
     */
    public SminaEnergyMinimizationStep(Path receptorFilePath,
                                       String sminaExecutable)  {
        this.receptorFilePath = receptorFilePath;
        this.sminaExecutable = sminaExecutable;
    }

    /**
     * Method that executes the minimization of a candidate by use of Smina.
     *
     * @param candidate The candidate whose conformers should be scored.
     * @return the scored candidate.
     * @throws PipelineException if the candidate cannot be scored.
     */
    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        if (debug) {
            System.out.println("Running Smina!");
        }
        if (candidate == null)
            throw new PipelineException("Smina got null as a candidate, validation failed?");

        // If we've already minimized, used the minimized versions as input, otherwise use the fixed conformers
        Path inputFile = candidate.getMinimizationOutputFilePath() != null ? candidate.getMinimizationOutputFilePath() : candidate.getFixedConformersFile();
        // THe output file path will be called smina.sdf, located in the candidate specific folder.
        Path outputPath = inputFile.resolveSibling("smina.sdf");
        // Run the smina minimization
        List<String> smina = smina(inputFile, outputPath);
        // Set the conformer scores and output path.
        candidate.setFixedConformersFile(outputPath);
        candidate.setConformerScores(getConformerScores(smina));
        candidate.setScoredConformersFile(outputPath);
        if (debug) {
            System.out.println("Completing Smina run, output candidate: " + candidate);
        }
        return candidate;
    }

    /**
     * Method that parses the smina output and collects the binding affinity score for every conformer.
     * This is the first value after 'Affinity:'
     *
     * @param smina The smina output, line by line.
     * @return the score for each conformer as given by smina.
     */
    private List<Double> getConformerScores(List<String> smina) {
        ArrayList<Double> conformerScores = new ArrayList<>();
        for (String line : smina) {
            // The scores are on each line starting with 'Affinity:'
            if (line.startsWith("Affinity:")) {
                String[] split = line.split("\\s+");
                // The second element(1) is the first value of the two.
                conformerScores.add(Double.parseDouble(split[1]));
            }
        }
        return conformerScores;
    }

    /**
     * The method responsible for running smina in a process.
     *
     * @param inputFile The path to an sdf file that should be scored.
     * @param outputPath The path where the minimized molecule should live.
     * @return the smina output.
     * @throws PipelineException if smina minimization failed.
     */
    private List<String> smina(Path inputFile,
                                    Path outputPath) throws PipelineException {
        // Initialize smina output list.
        List<String> sminaOutput;

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    sminaExecutable,
                    "-r", receptorFilePath.toString(),
                    "-l", inputFile.toString(),
                    "--minimize_iters", String.valueOf(800),
                    "--minimize",
                    "--log", inputFile.resolveSibling("log.txt").toString(),
                    "-o", outputPath.toString(),
                    "--force_cap", "0.1");

            // Build process with the command
            Process p = builder.start();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));


            sminaOutput = IOUtils.readLines(stdInput);

            if (debug) {
                for (String readLine : IOUtils.readLines(stdError)) {
                    System.err.println(readLine);
                }
            }

            return sminaOutput;

        } catch (IOException e) {

            // Throw pipeline exception
            throw new PipelineException("Energy minimization with Smina failed.", e);
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
