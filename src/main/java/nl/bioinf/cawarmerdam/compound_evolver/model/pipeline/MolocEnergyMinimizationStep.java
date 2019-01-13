/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.io.EneFileParser;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * An energy minimization step implemented with the Moloc program.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class MolocEnergyMinimizationStep extends EnergyMinimizationStep {

    private Path receptorFilePath;
    private String molocExecutable;

    /**
     * Constructor for the Moloc energy minimization step.
     *
     * @param forceField The force field that moloc should use.
     * @param receptorFilePath The path of the file that holds the receptor.
     * @param anchorFilePath The path to the file that holds the anchor.
     * @param molocExecutable The path to the executable of moloc's Mol3d program.
     * @param esprntoExecutable The path to the executable of moloc's Esprnto program.
     * @throws PipelineException if an exception occurred when converting the receptor to the mab format.
     */
    public MolocEnergyMinimizationStep(String forceField, Path receptorFilePath, Path anchorFilePath, String molocExecutable, String esprntoExecutable) throws PipelineException {
        super(forceField, anchorFilePath);
        this.receptorFilePath = convertToMabFile(receptorFilePath, esprntoExecutable);
        this.molocExecutable = molocExecutable;
    }

    /**
     * Executes the Moloc minimization step.
     *
     * @param candidate The candidate whose conformers have to be minimized.
     * @return the minimized candidate.
     * @throws PipelineException if conformers could not be minimized.
     */
    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {

        // Get the file names from the input
        Path inputFile = candidate.getFixedConformersFile();
        String ligandName = FilenameUtils.removeExtension(String.valueOf(inputFile.getFileName()));
        String receptorName = FilenameUtils.removeExtension(String.valueOf(receptorFilePath.getFileName()));

        // Run mol3d
        mol3d(inputFile, candidate);

        // Get the output files.
        File minimizedConformersFile = inputFile.resolveSibling(String.format("%s_3d.sd", ligandName)).toFile();
        Path newMinimizedConformersFilePath = inputFile.resolveSibling(String.format("%s_3d.sdf", ligandName));

        // Rename the output .sd file to .sdf
        boolean renamed = minimizedConformersFile.renameTo(newMinimizedConformersFilePath.toFile());

        // Throw an exception if the file was not renamed.
        if (!renamed) throw new PipelineException(String.format("Could not rename '%s' to '%s'",
                minimizedConformersFile, newMinimizedConformersFilePath));
        // Set scores.
        candidate.setConformerScores(getConformerScores(inputFile.getParent(), ligandName, receptorName));
        candidate.setMinimizationOutputFilePath(newMinimizedConformersFilePath);
        return candidate;
    }

    /**
     * Gets the conformer scores.
     *
     * @param directory The directory where the output files should reside.
     * @param ligandName The name of the ligand file without extension.
     * @param receptorName The name of the receptor file without extension.
     * @return a list of scores corresponding to the conformers.
     * @throws PipelineException if the .ene scores file could not be found.
     */
    private List<Double> getConformerScores(Path directory, String ligandName, String receptorName) throws PipelineException {
        // Get the ene file path: according to documentation and experience the ene file path will
        // be created in the dir where the .sd file is also created.
        //
        // The name is only OFTEN <ligandName>_<receptorName>.ene
        Path eneFilePath = Paths.get(String.format("%s_%s.ene", ligandName, receptorName));
        File eneFile = directory.resolve(String.valueOf(eneFilePath)).toFile();

        // Return the output from the parser if no error occurred.
        try {
            FileInputStream eneFileInputStream = new FileInputStream(eneFile);
            return EneFileParser.parseEneFile(eneFileInputStream, eneFile.getName());
        } catch (FileNotFoundException e) {
            throw new PipelineException("Could not read ENE score file from moloc", e);
        }
    }

    /**
     * Runs Moloc's Mol3d program in a process.
     *
     * @param inputFile The input .sdf file path with conformers.
     * @param candidate The candidate instance that is being scored.
     * @throws PipelineException if the process was not finished successfully.
     */
    private void mol3d(Path inputFile, Candidate candidate) throws PipelineException {
        // Build command
        String command = String.format("%s -e \"%s\" \"-w0.01\" \"%s\"",
                molocExecutable,
                receptorFilePath,
                inputFile);
        System.out.println("command = " + command);
        try {
            // Build process
            ProcessBuilder builder = new ProcessBuilder(
                    molocExecutable,
                    "-e", receptorFilePath.toString(),
                    "-w0.01",
                    inputFile.toString());

            // Print process
            System.out.println("pb.toString() = " + builder.command().toString());

            // Start the process
            final Process p = builder.start();

            p.waitFor();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            candidate.getPipelineLogger().info(
                    String.format("Mol3d has written the following output:%n%s%n", IOUtils.toString(stdInput)));

            // read any errors from the attempted command
            String stdErrorMessage = IOUtils.toString(stdError);
            if (!stdErrorMessage.isEmpty()) {
                candidate.getPipelineLogger().warning(
                        String.format("Mol3d has written an error message:%n%s%n", stdErrorMessage));
            }

        } catch (InterruptedException | IOException e) {

            // Throw pipeline exception
            throw new PipelineException("Energy minimization with Smina failed.", e);
        }
    }

    /**
     * Method that converts a pdb file to mab file.
     *
     * @param receptorFile The path to the file that holds the receptor in pdb format.
     * @param esprntoExecutable The path to the executable of Moloc's Esprnto program.
     * @return the converted path to the file.
     * @throws PipelineException if the conversion failed.
     */
    private Path convertToMabFile(Path receptorFile, String esprntoExecutable) throws PipelineException {
        Path mabFilePath = receptorFile.resolveSibling(
                FilenameUtils.removeExtension(receptorFile.getFileName().toString()) +
                        "_e.mab");

        // Format exception message
        String exceptionMessage = String.format(
                "The conversion of file '%s' to '%s' failed.",
                receptorFile.getFileName(),
                mabFilePath.getFileName());

        try {
            // Build process
            ProcessBuilder builder = new ProcessBuilder(
                    esprntoExecutable,
                    "-pM", receptorFile.toString());

            // Start the process
            final Process p = builder.start();

            p.waitFor();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // Read the output from the command
            System.out.println(
                    String.format("Conversion to mab file has written output:%n%s%n", IOUtils.toString(stdInput)));

            // read any errors from the attempted command
            String stdErrorMessage = IOUtils.toString(stdError);
            if (!stdErrorMessage.isEmpty()) {
                System.out.println(
                        String.format("Conversion to mab file has written an error message:%n%s%n", stdErrorMessage));
            }

        } catch (InterruptedException | IOException e) {

            // Throw pipeline exception
            throw new PipelineException(exceptionMessage, e);
        }
        if (!mabFilePath.toFile().exists()) throw new PipelineException(exceptionMessage);
        return mabFilePath;
    }
}
