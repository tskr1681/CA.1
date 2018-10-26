package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolConverter;
import chemaxon.formats.MolExporter;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VinaEnergyMinimizationStep extends EnergyMinimizationStep {
    private final String prepareLigandExecutable;
    private Path receptorFilePath;
    private String vinaExecutable;

    public VinaEnergyMinimizationStep(String forcefield, Path receptorFilePath, String vinaExecutable) {
        super(forcefield);
        this.receptorFilePath = receptorFilePath;
        this.vinaExecutable = vinaExecutable;
        this.prepareLigandExecutable = "\"C:\\Program Files (x86)\\MGLTools-1.5.6\\python.exe\" " +
                "\"C:\\Program Files (x86)\\MGLTools-1.5.6\\Lib\\site-packages\\AutoDockTools\\Utilities24\\prepare_ligand4.py\"";
    }

    @Override
    public Double execute(Path inputFile) {
        String ligandName = FilenameUtils.removeExtension(String.valueOf(inputFile.getFileName()));
        String receptorName = FilenameUtils.removeExtension(String.valueOf(receptorFilePath.getFileName()));
        // Convert ligand to pdb
        Path pdbPath = convertToPdb(inputFile);
        // Prepare ligand to pdbqt
        Path pdbqtPath = prepareLigand(pdbPath);
        vina(pdbqtPath);
        return 0.0;
    }

    private Path prepareLigand(Path pdbPath) {
        String fileName = pdbPath.getFileName().toString();
        Path outputFile = Paths.get(FilenameUtils.removeExtension(pdbPath.toString()) + ".pdbqt");
        String command = String.format("%s -l %s -o %s",
                prepareLigandExecutable, pdbPath.toString(), outputFile.toString());

        String line = null;
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
        return outputFile;
    }

//    private Path convertToPdb(Path inputFile) {
//        // Create an OBConversion object.
//        String fileName = inputFile.getFileName().toString();
//        Path outputFile = Paths.get(FilenameUtils.removeExtension(fileName) + "pdb");
//        OBConversion conv = new OBConversion(fileName, outputFile.toString());
//        // Set the input format.
//        if (conv.SetInAndOutFormats("sdf", "pdb")) {
//            conv.Convert();
//            return outputFile;
//        }
//        throw new PipeLineException(String.format("ligand in '%s' could not be converted to '%s'.",
//                fileName, outputFile.toString()));
//    }

    private Path convertToPdb(Path inputFile) {
        String fileName = inputFile.getFileName().toString();
        Path outputFile = Paths.get(FilenameUtils.removeExtension(inputFile.toString()) + ".pdb");
        MolConverter.Builder mcbld = new MolConverter.Builder();
        mcbld.addInput(String.valueOf(inputFile), "");
        mcbld.setOutput(String.valueOf(outputFile), "pdb");
        mcbld.setOutputFlags(MolExporter.TEXT);
        MolConverter mc = null;
        try {
            mc = mcbld.build();
            while(mc.convert());
            mc.close();
            return outputFile;
        } catch (IOException e) {
            throw new PipeLineException(String.format("ligand in '%s' could not be converted to '%s':%s",
                    fileName, outputFile.toString(), e.getMessage()));
        }
    }

    private void vina(Path inputFile) {
        // Initialize string line
        String line = null;

        // Build command
        String command = String.format("%s --receptor \"%s\" --ligand \"%s\" " +
                        "--center_x \"%s\" --center_y \"%s\" --center_z \"%s\" " +
                        "--size_x \"%s\" --size_y \"%s\" --size_z \"%s\" --exhaustiveness 1",
                vinaExecutable,
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
