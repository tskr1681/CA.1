package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolConverter;
import chemaxon.formats.MolExporter;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SminaEnergyMinimizationStep extends EnergyMinimizationStep {
    private Path receptorFilePath;
    private String sminaExecutable;

    public SminaEnergyMinimizationStep(String forcefield, Path receptorFilePath, String sminaExecutable) {
        super(forcefield);
        this.receptorFilePath = receptorFilePath;
        this.sminaExecutable = sminaExecutable;
    }

    @Override
    public Double execute(Path inputFile) {
        String ligandName = FilenameUtils.removeExtension(String.valueOf(inputFile.getFileName()));
        String receptorName = FilenameUtils.removeExtension(String.valueOf(receptorFilePath.getFileName()));
        smina(inputFile);
        return 0.0;
    }

    private void smina(Path inputFile) {
        // Initialize string line
        String line = null;

        // Build command
        String command = String.format("%s --receptor \"%s\" --ligand \"%s\" " +
                        "--center_x \"%s\" --center_y \"%s\" --center_z \"%s\" " +
                        "--size_x \"%s\" --size_y \"%s\" --size_z \"%s\" --exhaustiveness 1",
                sminaExecutable,
                String.valueOf(receptorFilePath),
                String.valueOf(inputFile),
                26,
                -22.5,
                -6.5,
                20,
                20,
                20);
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
            throw new PipeLineException(String.format(
                    "minimizing energy with command: '%s' failed with the following exception: %s",
                    command,
                    e.toString()));
        }
    }
}
