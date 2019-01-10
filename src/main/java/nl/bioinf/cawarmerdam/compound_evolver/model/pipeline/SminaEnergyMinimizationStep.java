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
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class SminaEnergyMinimizationStep extends EnergyMinimizationStep {
    private Path receptorFilePath;
    private String sminaExecutable;

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

    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        Path inputFile = candidate.getFixedConformersFile();
        Map<String, Double> conformerCoordinates = getConformerCoordinates(inputFile);
        Path outputPath = inputFile.resolveSibling("smina.sdf");
        ArrayList<String> smina = smina(inputFile, conformerCoordinates, outputPath, candidate);
        candidate.setConformerScores(getConformerScores(smina));
        candidate.setMinimizationOutputFilePath(outputPath);
        return candidate;
    }

    private List<Double> getConformerScores(ArrayList<String> smina) {
        ArrayList<Double> conformerScores = new ArrayList<>();
        for (String line : smina) {
            if (line.startsWith("Affinity:")) {
                String[] split = line.split("\\s+");
                conformerScores.add(Double.parseDouble(split[1]));
            }
        }
        return conformerScores;
    }

    private ArrayList<String> smina(Path inputFile, Map<String, Double> conformerCoordinates, Path outputPath, Candidate candidate) throws PipelineException {
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
