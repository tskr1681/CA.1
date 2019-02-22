/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolConverter;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ConformerFixationStep implements PipelineStep<Candidate, Candidate> {

    private final String smartsPattern;
    private final Path referenceMolecule;
    private final String obfitExecutable;

    public ConformerFixationStep(Path referenceMolecule, String obfitExecutable) throws PipelineException {
        this.referenceMolecule = referenceMolecule;
        this.obfitExecutable = obfitExecutable;
        this.smartsPattern = convertToSmarts();
    }

    /**
     * @return a smarts pattern in as a String
     * @throws PipelineException if the in or output files could not be processed
     */
    private String convertToSmarts() throws PipelineException {
        try {
            // Get input and output
            InputStream inputStream = new FileInputStream(referenceMolecule.toFile());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // Create a converter instance
            MolConverter converter = new MolConverter(inputStream, outputStream, "smiles:u", false);
            // Convert and close
            converter.convert();
            converter.close();
            // Convert the output stream to a String
            String smartsFileAsString = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            return smartsFileAsString.replace("\n", "").replace("\r", "");
        } catch (IOException e) {
            throw new PipelineException("Could not create smarts pattern", e);
        }
    }

    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        Path targetMoleculePath = candidate.getConformersFile();
        Path outFile = targetMoleculePath.resolveSibling(
                Paths.get(String.format("fixed_%s", targetMoleculePath.getFileName().toString())));

        // Try to fixate conformers using obfit
        obFit(targetMoleculePath.toString(), outFile.toString(), candidate);
        candidate.setFixedConformersFile(outFile);
        return candidate;
    }

    /**
     * Execute the obfit command line program.
     * @param conformerLib The path to a reactionFiles with conformers.
     * @param outFile The path to a reactionFiles that can function as an output reactionFiles.
     * @param candidate The candidate that is being scored.
     * @throws PipelineException if an error occured in executing the obfit command.
     */
    private void obFit(String conformerLib, String outFile, Candidate candidate) throws PipelineException {

        // Build command
        String command = String.format("%s %s %s %s > %s",
                obfitExecutable,
                smartsPattern,
                referenceMolecule,
                conformerLib,
                outFile);

//        candidate.getPipelineLogger().info(String.format("Starting obfit with this command:%n%s%n", command));

        try {
            ProcessBuilder builder = new
                    ProcessBuilder(obfitExecutable, smartsPattern, referenceMolecule.toString(), conformerLib);
            builder.redirectOutput(new File(outFile));

            // Start the process
            final Process p = builder.start();

            p.waitFor();

//            BufferedReader stdInput = new BufferedReader(new
//                    InputStreamReader(p.getInputStream()));
//
//            BufferedReader stdError = new BufferedReader(new
//                    InputStreamReader(p.getErrorStream()));
//
//            // read the output from the command
//            candidate.getPipelineLogger().info(
//                    String.format("Obfit has written the following output:%n%s%n", IOUtils.toString(stdInput)));

//            // read any errors from the attempted command
//            String stdErrorMessage = IOUtils.toString(stdError);
//            if (!stdErrorMessage.isEmpty()) {
//                candidate.getPipelineLogger().warning(
//                        String.format("Obfit has written an error message:%n%s%n", stdErrorMessage));
//            }

        } catch (IOException | InterruptedException e) {

            // Throw pipeline exception
            throw new PipelineException("Fixing conformers with 'obfit' to reference failed.", e);
        }
    }
}
