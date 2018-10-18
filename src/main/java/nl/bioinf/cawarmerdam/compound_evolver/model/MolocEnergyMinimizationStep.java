package nl.bioinf.cawarmerdam.compound_evolver.model;

import nl.bioinf.cawarmerdam.compound_evolver.io.EneFileParser;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MolocEnergyMinimizationStep extends EnergyMinimizationStep {

    private String receptorFilePath;
    private String molocExecutable;

    public MolocEnergyMinimizationStep(String forcefield, String receptorFilePath, String molocExecutable) {
        super(forcefield);
        this.receptorFilePath = receptorFilePath;
        this.molocExecutable = molocExecutable;
    }

    @Override
    public Double execute(Path inputFile) {
        String ligandName = FilenameUtils.removeExtension(String.valueOf(inputFile.getFileName()));
        String receptorName = FilenameUtils.removeExtension(String.valueOf(Paths.get(receptorFilePath).getFileName()));
        mol3d(inputFile);
        Path eneFilePath = Paths.get(String.format("%s_%s.ene", ligandName, receptorName));
        File eneFile = inputFile.resolveSibling(String.valueOf(eneFilePath)).toFile();
        FileInputStream fastaFileInputStream = null;
        try {
            fastaFileInputStream = new FileInputStream(eneFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        List<Double> conformerScores = EneFileParser.parseEneFile(fastaFileInputStream, eneFile.getName());
        System.out.println("conformerScores = " + conformerScores);
        if (conformerScores.size() > 0) {
            return Collections.min(conformerScores);
        }
        return 0.0;
    }

    private void mol3d(Path inputFile) {
        // Initialize string line
        String line = null;

        // Build command
        String command = String.format("%s -e \"%s\" \"-w0.01\" -E \"%s\"",
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
