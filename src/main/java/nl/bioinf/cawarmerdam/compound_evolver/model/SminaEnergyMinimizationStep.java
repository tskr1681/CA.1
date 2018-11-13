package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolImporter;
import chemaxon.struc.DPoint3;
import chemaxon.struc.Molecule;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SminaEnergyMinimizationStep extends EnergyMinimizationStep {
    private Path receptorFilePath;
    private String sminaExecutable;

    public SminaEnergyMinimizationStep(String forcefield, Path receptorFilePath, String sminaExecutable, String pythonExecutable, String prepareReceptorExecutable) {
        super(forcefield);
        this.receptorFilePath = convertToPdbQt(receptorFilePath, pythonExecutable, prepareReceptorExecutable);
        this.sminaExecutable = sminaExecutable;
    }

    @Override
    public Double execute(Path inputFile) {
        String ligandName = FilenameUtils.removeExtension(String.valueOf(inputFile.getFileName()));
        String receptorName = FilenameUtils.removeExtension(String.valueOf(receptorFilePath.getFileName()));
        Map<String, Double> conformerCoordinates = getConformerCoordinates(inputFile);
        smina(inputFile, conformerCoordinates);
        return 0.0;
    }

    private void smina(Path inputFile, Map<String, Double> conformerCoordinates) {
        // Initialize string line
        String line = null;

        // Build command
        String command = String.format("%s --receptor \"%s\" --ligand \"%s\" " +
                        "--center_x \"%s\" --center_y \"%s\" --center_z \"%s\" " +
                        "--size_x \"%s\" --size_y \"%s\" --size_z \"%s\" --minimize",
                sminaExecutable,
                String.valueOf(receptorFilePath),
                String.valueOf(inputFile),
                conformerCoordinates.get("centerX"),
                conformerCoordinates.get("centerY"),
                conformerCoordinates.get("centerZ"),
                conformerCoordinates.get("sizeX"),
                conformerCoordinates.get("sizeY"),
                conformerCoordinates.get("sizeZ"));
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
                    "--minimize");

            // Build process with the command
            Process p = builder.start();

            System.out.println("builder = " + builder.command().toString());

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            throw new PipeLineException(String.format(
                    "minimizing energy with command: '%s' failed with the following exception: %s",
                    command,
                    e.toString()));
        }
    }

    private Path convertToPdbQt(Path pdbPath, String pythonExecutable, String prepareReceptorExecutable) {
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

            System.out.println("builder = " + builder.command().toString());

            final Process p = builder.start();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return pdbqtFilePath;
    }

    public static Map<String, Double> getConformerCoordinates(Path ligandPath) {
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
            e.printStackTrace();
            // Throw exception
        }

        return null;
    }
}
