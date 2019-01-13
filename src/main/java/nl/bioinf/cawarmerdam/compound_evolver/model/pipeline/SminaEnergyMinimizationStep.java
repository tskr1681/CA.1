/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolImporter;
import chemaxon.struc.DPoint3;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.ExclusionShape;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Minimization step that uses Smina.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class SminaEnergyMinimizationStep extends EnergyMinimizationStep {
    private Path receptorFilePath;
    private String sminaExecutable;

    /**
     * Constructor for smina energy minimization step.
     *
     * @param forcefield The force field that smina should use.
     * @param receptorFilePath The path of the file that holds the receptor.
     * @param anchorFilePath The path to the file that holds the anchor.
     * @param sminaExecutable The path to the executable of Smina.
     * @param pythonExecutable The path to the python executable to run the prepare receptor tool from ADT.
     * @param prepareReceptorExecutable The path to the prepare receptor tool from ADT.
     * @throws PipelineException if the receptor cannot be prepared for energy minimization with Smina.
     */
    public SminaEnergyMinimizationStep(String forcefield,
                                       Path receptorFilePath,
                                       Path anchorFilePath,
                                       String sminaExecutable,
                                       String pythonExecutable,
                                       String prepareReceptorExecutable) throws PipelineException {
        super(forcefield, anchorFilePath);
        this.receptorFilePath = convertToPdbQt(receptorFilePath, pythonExecutable, prepareReceptorExecutable);
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
        Path inputFile = candidate.getFixedConformersFile();
        Map<String, Double> conformerCoordinates = getConformerCoordinates(inputFile);
        // THe output file path will be called smina.sdf, located in the candidate specific folder.
        Path outputPath = inputFile.resolveSibling("smina.sdf");
        // Run the smina minimization
        ArrayList<String> smina = smina(inputFile, conformerCoordinates, outputPath, candidate);
        // Set the conformer scores and output path.
        candidate.setConformerScores(getConformerScores(smina));
        candidate.setMinimizationOutputFilePath(outputPath);
        return candidate;
    }

    /**
     * Method that parses the smina output and collects the binding affinity score for every conformer.
     * This is the first value after 'Affinity:'
     *
     * @param smina The smina output, line by line.
     * @return the score for each conformer as given by smina.
     */
    private List<Double> getConformerScores(ArrayList<String> smina) {
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
     * @param conformerCoordinates A map that describes the box to consider for minimization.
     * @param outputPath The path where the minimized molecule should live.
     * @param candidate The candidate instance that is scored.
     * @return the smina output.
     * @throws PipelineException if smina minimization failed.
     */
    private ArrayList<String> smina(Path inputFile,
                                    Map<String, Double> conformerCoordinates,
                                    Path outputPath,
                                    Candidate candidate) throws PipelineException {
        // Initialize string line
        String line = null;

        // Initialize smina output list.
        ArrayList<String> sminaOutput = new ArrayList<>();

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    sminaExecutable,
                    "--receptor", String.valueOf(receptorFilePath),
                    "--ligand", String.valueOf(inputFile),
                    "--center_x", conformerCoordinates.get("centerX").toString(),
                    "--center_y", conformerCoordinates.get("centerY").toString(),
                    "--center_z", conformerCoordinates.get("centerZ").toString(),
                    "--size_x", conformerCoordinates.get("sizeX").toString(),
                    "--size_y", conformerCoordinates.get("sizeY").toString(),
                    "--size_z", conformerCoordinates.get("sizeZ").toString(),
                    "--minimize", "--log", inputFile.resolveSibling("log.txt").toString(),
                    "--out", outputPath.toString());

            // Build process with the command
            Process p = builder.start();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            while ((line = stdInput.readLine()) != null) {
                sminaOutput.add(line);
            }

            // read any errors from the attempted command
            String stdErrorMessage = IOUtils.toString(stdError);
            if (!stdErrorMessage.isEmpty()) {
                candidate.getPipelineLogger().warning(
                        String.format("Smina has written an error message:%n%s%n", stdErrorMessage));
            }
            return sminaOutput;

        } catch (IOException e) {

            // Throw pipeline exception
            throw new PipelineException("Energy minimization with Smina failed.", e);
        }
    }

    /**
     * Converts the input .pdb file to a .pdbqt file that smina wants.
     *
     * @param pdbPath The path to the file that holds the pdb structure.
     * @param pythonExecutable The path to the python executable to run the prepare receptor tool from ADT.
     * @param prepareReceptorExecutable The path to the prepare receptor tool from ADT.
     * @return the path to the file that holds the converted pdbqt structure.
     * @throws PipelineException if the file could not be converted.
     */
    private Path convertToPdbQt(Path pdbPath, String pythonExecutable, String prepareReceptorExecutable) throws PipelineException {
        String fileName = pdbPath.getFileName().toString();
        Path pdbqtFilePath = Paths.get(FilenameUtils.removeExtension(pdbPath.toString()) + ".pdbqt");

        String line = null;
        try {
            // Build process
            ProcessBuilder builder = new ProcessBuilder(
                    pythonExecutable,
                    prepareReceptorExecutable,
                    "-r", pdbPath.toString(),
                    "-A", "hydrogens",
                    "-o", pdbqtFilePath.toString());

            return convertFileWithProcessBuilder(pdbqtFilePath, builder);
        } catch (IOException | InterruptedException e) {

            // Format exception message
            String exceptionMessage = String.format(
                    "The conversion of file '%s' to '%s' failed.",
                    pdbPath.getFileName(),
                    pdbqtFilePath.getFileName());

            // Throw pipeline exception
            throw new PipelineException(exceptionMessage, e);
        }
    }

    /**
     * Method that determines the location and size of a box around the conformer that will be scored.
     *
     * @param ligandPath The path to the file that holds the ligand, or compound.
     * @return a map that describes a box that should be considered for minimization.
     * @throws PipelineException if the compound file could not be imported.
     */
    private static Map<String, Double> getConformerCoordinates(Path ligandPath) throws PipelineException {
        try {

            // Initialize new Lists for centers and sizes
            double minX, minY, minZ = minY = minX = Double.POSITIVE_INFINITY;
            double maxX, maxY, maxZ = maxY = maxX = Double.NEGATIVE_INFINITY;

            // Create importer for file
            MolImporter importer = new MolImporter(ligandPath.toFile());

            // read the first molecule from the file
            Molecule m = importer.read();
            while (m != null) {
                // Get the center and size of each molecule
                DPoint3[] enclosingCube = m.getEnclosingCube();

                // Update min and max values if necessary
                minX = Double.min(minX, enclosingCube[0].x);
                minY = Double.min(minY, enclosingCube[0].y);
                minZ = Double.min(minZ, enclosingCube[0].z);

                maxX = Double.max(maxX, enclosingCube[1].x);
                maxY = Double.max(maxY, enclosingCube[1].y);
                maxZ = Double.max(maxZ, enclosingCube[1].z);

                // read the next molecule from the input file
                m = importer.read();
            }
            importer.close();

            // Get center and size of the ligands
            Map<String, Double> coordinates = new HashMap<>();

            coordinates.put("sizeX", (maxX - minX) * 2);
            coordinates.put("sizeY", (maxY - minY) * 2);
            coordinates.put("sizeZ", (maxZ - minZ) * 2);

            coordinates.put("centerX", (minX + maxX) / 2);
            coordinates.put("centerY", (minY + maxY) / 2);
            coordinates.put("centerZ", (minZ + maxZ) / 2);

            return coordinates;

        } catch (IOException e) {
            throw new PipelineException("Conformer coordinates could not be obtained.", e);
        }
    }
}
