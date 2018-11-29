/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolConverter;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

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

    private String smartsPattern;
    private Path referenceMolecule;
    private String obfitExecutable;

    public ConformerFixationStep(Path referenceMolecule, String obfitExecutable) {
        this.referenceMolecule = referenceMolecule;
        this.obfitExecutable = obfitExecutable;
        try {
            this.smartsPattern = convertToSmarts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return a smarts pattern in as a String
     * @throws IOException if the in or output files could not be processed
     */
    private String convertToSmarts() throws IOException {
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
    }

    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        Path targetMoleculePath = candidate.getConformersFile();
        Path outFile = targetMoleculePath.resolveSibling(
                Paths.get(String.format("fixed_%s", targetMoleculePath.getFileName().toString())));

        // Try to fixate conformers using obfit
        obFit(targetMoleculePath.toString(), outFile.toString());
        candidate.setFixedConformersFile(outFile);
        return candidate;
    }

    /**
     * Execute the obfit command line program
     * @param conformerLib The path to a reactionFile with conformers
     * @param outFile The path to a reactionFile that can function as an output reactionFile
     * @throws PipelineException if an error occured in executing the obfit command
     */
    private void obFit(String conformerLib, String outFile) throws PipelineException {
        // Initialize string line
        String line = null;

        // Build command
        String command = String.format("%s %s %s %s > %s",
                obfitExecutable,
                smartsPattern,
                referenceMolecule,
                conformerLib,
                outFile);

        try {
            ProcessBuilder builder = new
                    ProcessBuilder(obfitExecutable, smartsPattern, referenceMolecule.toString(), conformerLib);
            builder.redirectOutput(new File(outFile));

            System.out.println("Obfit command = " + builder.command().toString());

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
//            System.out.println("Here is the standard output of the command:\n");
//            while ((line = stdInput.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            // read any errors from the attempted command
//            System.out.println("Here is the standard error of the command (if any):\n");
//            while ((line = stdError.readLine()) != null) {
//                System.out.println(line);
//            }
        } catch (Exception e) {

            // Throw pipeline exception
            throw new PipelineException("Fixing conformers with 'obfit' to reference failed.", e);
        }
    }
}
