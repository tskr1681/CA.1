package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.Molecule;
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

    public MolocEnergyMinimizationStep(String forcefield, Path receptorFilePath, Path anchorFilePath, String molocExecutable, String esprntoExecutable) throws PipelineException {
        super(forcefield, anchorFilePath);
        this.receptorFilePath = convertToMabFile(receptorFilePath, esprntoExecutable);
        this.molocExecutable = molocExecutable;
    }

    @Override
    public Void execute(Candidate candidate) throws PipelineException {
        Path inputFile = candidate.getFixedConformersFile();
        String ligandName = FilenameUtils.removeExtension(String.valueOf(inputFile.getFileName()));
        String receptorName = FilenameUtils.removeExtension(String.valueOf(receptorFilePath.getFileName()));
        mol3d(inputFile);
        List<Double> conformerScores = getConformerScores(inputFile, ligandName, receptorName);
        double score = 0.0;
        if (conformerScores.size() > 0) {
            score = Collections.min(conformerScores);
            File minimizedConformersFile = inputFile.resolveSibling(String.format("%s_3d.sd", ligandName)).toFile();
            Path newMinimizedConformersFilePath = inputFile.resolveSibling(String.format("%s_3d.sdf", ligandName));
            minimizedConformersFile.renameTo(newMinimizedConformersFilePath.toFile());
            Molecule bestConformer = getBestConformer(newMinimizedConformersFilePath, conformerScores.indexOf(score));
            candidate.setCommonSubstructureToAnchorRmsd(calculateLeastAnchorRmsd(bestConformer));
            exportConformer(bestConformer, inputFile.resolveSibling("best-conformer.sdf"));
        }
        candidate.setRawScore(score);
        return null;
    }

    private List<Double> getConformerScores(Path inputFile, String ligandName, String receptorName) throws PipelineException {
        // Get the ene file path: according to documentation and experience the ene file path will
        // be created in the dir where the .sd file is also created.
        //
        // The name is only OFTEN <ligandName>_<receptorName>.ene
        Path eneFilePath = Paths.get(String.format("%s_%s.ene", ligandName, receptorName));
        File eneFile = inputFile.resolveSibling(String.valueOf(eneFilePath)).toFile();

        // Return the output from the parser if no error occurred.
        try {
            FileInputStream eneFileInputStream = new FileInputStream(eneFile);
            return EneFileParser.parseEneFile(eneFileInputStream, eneFile.getName());
        } catch (FileNotFoundException e) {
            throw new PipelineException("Could not read ENE score file from moloc", e);
        }
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
