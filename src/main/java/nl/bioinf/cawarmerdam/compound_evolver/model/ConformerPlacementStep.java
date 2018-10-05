package nl.bioinf.cawarmerdam.compound_evolver.model;

import org.openbabel.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConformerPlacementStep implements PipelineStep<Path, Path> {

    private String obSmartsPattern;
    private String referenceMolecule;
    private String obfitExecutable;
    private static Path subPath = Paths.get("fixed");

    public ConformerPlacementStep(String referenceMolecule, String smartsPattern, String obfitExecutable) {
        this.referenceMolecule = referenceMolecule;
        this.obfitExecutable = obfitExecutable;
        this.obSmartsPattern = smartsPattern;
    }

    private OBMol convertToOBMol(String inputStringMolecule) {
        OBConversion conversion = new OBConversion();
        OBMol mol = new OBMol();
        conversion.SetInFormat("sdf");
        conversion.ReadString(mol, inputStringMolecule);
        return mol;
    }

    @Override
    public Path execute(Path targetMolecule) throws PipeLineException {
        // Set target molecule
//        obAlign.SetTargetMol(convertToOBMol(targetMolecule));
//        obAlign.Align();
//        return obAlign.GetAlignment();
        Path outFile = targetMolecule.resolveSibling(
                Paths.get(subPath.toString(), targetMolecule.getFileName().toString()));
        try {
            obFit(targetMolecule.toString(), outFile.toString());
        } catch (ConformerPlacementError conformerPlacementError) {
            throw new PipeLineException(conformerPlacementError.toString());
        }
        return outFile;
    }

    private void obFit(String conformerLib, String outFile) throws ConformerPlacementError {
        // Initialize string line
        String line = null;

        // Build command
        String command = String.format("%s %s %s %s > %s",
                obfitExecutable,
                obSmartsPattern,
                referenceMolecule,
                conformerLib,
                outFile);

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
            System.out.println("exception happened - here's what I know: ");
            System.out.println("e = " + e.toString());
            throw new ConformerPlacementError(e.toString());
        }
    }

    private class ConformerPlacementError extends Throwable {
        ConformerPlacementError(String s) {
        }
    }
}
