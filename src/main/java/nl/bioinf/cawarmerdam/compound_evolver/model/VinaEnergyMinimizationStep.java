package nl.bioinf.cawarmerdam.compound_evolver.model;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Path;

public class VinaEnergyMinimizationStep extends EnergyMinimizationStep {
    private Path receptorFilePath;
    private String vinaExecutable;

    public VinaEnergyMinimizationStep(String forcefield, Path receptorFilePath, String vinaExecutable) {
        super(forcefield);
        this.receptorFilePath = receptorFilePath;
        this.vinaExecutable = vinaExecutable;
    }

    @Override
    public Double execute(Path inputFile) {
        String ligandName = FilenameUtils.removeExtension(String.valueOf(inputFile.getFileName()));
        String receptorName = FilenameUtils.removeExtension(String.valueOf(receptorFilePath.getFileName()));
        // Convert ligand to pdb
        // Prepare ligand to pdbqt
        vina(inputFile);
        return 0.0;
    }

    private void vina(Path inputFile) {
        // Initialize string line
        String line = null;

        // Build command
        String command = String.format("%s --receptor \"%s\" --ligand \"%s\" " +
                        "--center_x \"%s\" --center_y \"%s\" --center_z \"%s\" " +
                        "--size_x \"%s\" --size_y \"%s\" --size_z \"%s\"",
                vinaExecutable,
                String.valueOf(receptorFilePath),
                0,
                0,
                0,
                200,
                200,
                200,
                inputFile);
        try {
            // Build process with the command
            Process p = Runtime.getRuntime().exec(command);

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
            throw new PipeLineError(String.format(
                    "minimizing energy with command: '%s' failed with the following exception: %s",
                    command,
                    e.toString()));
        }
    }
}
