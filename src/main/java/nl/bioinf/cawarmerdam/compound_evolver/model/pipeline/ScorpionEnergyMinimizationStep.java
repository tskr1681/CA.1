package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to run scorpion as part of the pipeline
 *
 * @author H. (Hylke) Middel
 * @author h.middel@st.hanze.nl
 * @version 0.0.1
 */
public class ScorpionEnergyMinimizationStep implements PipelineStep<Candidate, Candidate> {
    private Path receptorFilePath;
    private String scorpionExecutable;
    private Path anchorFilePath;
    /**
     * Constructor for the scorpion energy minimization step.
     *
     * @param receptor     The force field that should be chosen in minimization.
     * @param anchorFilePath The path of the file that holds the anchor.
     * @param scorpionExecutable The executable to run scorpion, or specifically, viewpaths3.py
     */
    public ScorpionEnergyMinimizationStep(Path receptor, Path anchorFilePath, String scorpionExecutable) {
        this.receptorFilePath = receptor;
        this.scorpionExecutable = scorpionExecutable;
        this.anchorFilePath = anchorFilePath;
    }

    /**
     * Runs scorpion on the candidate
     * @param candidate The candidate to find the conformer scores for
     * @return the candidate with updated parameters
     * @throws PipelineException when a problem occurs with running scorpion
     */
    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        if (candidate == null) {
            throw new PipelineException("Scorpion got null as a candidate, validification failed?");
        }
        Path fixedconformers = candidate.getFixedConformersFile();
        scorpion(fixedconformers);
        //scorpion output takes the form of "original name_scorp.sdf" where original_name is the original file name without the extension

        //Make sure scorpion is actually finished
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String output_str = fixedconformers.toString().replaceAll(".sdf", "") + "_scorp";
        Path output_sdf = fixedconformers.resolveSibling(output_str + ".sdf");

        candidate.setMinimizationOutputFilePath(output_sdf);

        List<Double> scores = getConformerScores(output_sdf);
        if (Collections.min(scores) == Double.POSITIVE_INFINITY) {
            throw new PipelineException("Scorpion did not produce any valid scores");
        }
        candidate.setConformerScores(scores);

        return candidate;
    }

    /**
     * Gets the conformer scores from the output of scorpion
     * @param scorpionoutput the path to the scorpion output file
     * @return a list of scores for each conformer, or positive infinity if the conformer has no score
     * @throws PipelineException if reading the file fails
     */
    private static List<Double> getConformerScores(Path scorpionoutput) throws PipelineException {
        ArrayList<Double> conformerScores = new ArrayList<>();
        try {
            List<String> l = Files.readAllLines(scorpionoutput);
        //Does the next line have a score? Used to read the scores without reading other random numbers
        boolean next_has_score = false;

        //Did the block we were in contain a score? If not, we append Double.POSITIVE_INFINITY, to guarantee it won't be the best one
        boolean block_has_score = false;
        for (String line : l) {
            // The scores are on each line starting with 'Affinity:'
            if (line.startsWith(">  <TOTAL>") && !block_has_score) {
                next_has_score = true;
                block_has_score = true;
            } else if (next_has_score) {
                next_has_score = false;
                //Higher scores are better in scorpion, but the rest of the pipeline assumes lower scores are better
                conformerScores.add(-Double.parseDouble(line));
            } else if (line.startsWith("$$$$")) {
                if (!block_has_score) {
                    conformerScores.add(Double.POSITIVE_INFINITY);
                }
                //New block, so the new block doesn't have a score yet
                block_has_score = false;
            }
        }
        return conformerScores;
        } catch (IOException e) {
            throw new PipelineException("Couldn't get conformer scores from file! " + e);
        }
    }

    private void scorpion(Path inputFile) throws PipelineException {
        // Initialize string line
        String line;

        try {
            //The command to run
            ProcessBuilder builder = new ProcessBuilder(
                    scorpionExecutable,
                    inputFile.getParent().toString(),
                    "-p", String.valueOf(receptorFilePath),
                    "-i", String.valueOf(inputFile));

            // Build process with the command
            Process p = builder.start();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            while ((stdInput.readLine()) != null) {
            }

            // read any errors from the attempted command
            String stdErrorMessage = IOUtils.toString(stdError);
            if (!stdErrorMessage.isEmpty()) {
                System.out.println(String.format("Scorpion has written an error message:%n%s%n", stdErrorMessage));
            }

        } catch (IOException e) {

            // Throw pipeline exception
            throw new PipelineException("Energy minimization with Scorpion failed.", e);
        }
    }

}
