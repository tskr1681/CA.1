package nl.bioinf.cawarmerdam.compound_evolver.model;

import nl.bioinf.cawarmerdam.compound_evolver.io.EneFileParser;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class MolocEnergyMinimizationStep extends EnergyMinimizationStep {

    private Path receptorFilePath;
    private String molocExecutable;

    public MolocEnergyMinimizationStep(String forcefield, Path receptorFilePath, String molocExecutable, String esprntoExecutable) throws PipelineException {
        super(forcefield);
        this.receptorFilePath = convertToMabFile(receptorFilePath, esprntoExecutable);
        this.molocExecutable = molocExecutable;
    }

    @Override
    public Double execute(Path inputFile) throws PipelineException {
        String ligandName = FilenameUtils.removeExtension(String.valueOf(inputFile.getFileName()));
        String receptorName = FilenameUtils.removeExtension(String.valueOf(receptorFilePath.getFileName()));
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

    private void mol3d(Path inputFile) throws PipelineException {
        // Initialize string line
        String line = null;

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

        } catch (InterruptedException | IOException e) {

            // Throw pipeline exception
            throw new PipelineException("Energy minimization with Smina failed.", e);
        }
    }

    private Path convertToMabFile(Path receptorFile, String esprntoExecutable) throws PipelineException {
        Path mabFilePath = receptorFile.resolveSibling(
                FilenameUtils.removeExtension(receptorFile.getFileName().toString()) +
                        "_e.mab");

        String line = null;

        try {
            // Build process
            ProcessBuilder builder = new ProcessBuilder(
                    esprntoExecutable,
                    "-pM", receptorFile.toString());

            // Print process
            System.out.println("pb.toString() = " + builder.command().toString());

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
            return mabFilePath;

        } catch (InterruptedException | IOException e) {

            // Format exception message
            String exceptionMessage = String.format(
                    "The conversion of file '%s' to '%s' failed.",
                    receptorFile.getFileName(),
                    mabFilePath.getFileName());

            // Throw pipeline exception
            throw new PipelineException(exceptionMessage, e);
        }
    }
}
