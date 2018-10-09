package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MolocEnergyMinimizationStep extends EnergyMinimizationStep {

    private String receptorFilePath;
    private String molocExecutable;

    public MolocEnergyMinimizationStep(String forcefield, String receptorFilePath, String molocExecutable) {
        super(forcefield);
        this.receptorFilePath = receptorFilePath;
        this.molocExecutable = molocExecutable;
    }

    @Override
    public Path execute(Path inputFile) {
        mol3d(inputFile);
        return inputFile;
    }

    private void mol3d(Path inputFile) {
        // Initialize string line
        String line = null;

        // Build command
        String command = String.format("%s -e %s -w0.01 %s",
                molocExecutable,
                receptorFilePath,
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
