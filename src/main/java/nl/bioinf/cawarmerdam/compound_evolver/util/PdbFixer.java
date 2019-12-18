package nl.bioinf.cawarmerdam.compound_evolver.util;

import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.PipelineException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PdbFixer {

    public static void runFixer(String lepro_exe, Path input) throws PipelineException {
        String line;
        List<String> fixer_output = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    lepro_exe,
                    String.valueOf(input)
                    );
            // Build process with the command
            builder.directory(input.getParent().toFile());
            Process p = builder.start();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            while ((line = stdInput.readLine()) != null) {
                fixer_output.add(line);
            }
            while ((line = stdError.readLine()) != null) {
                fixer_output.add(line);
            }
            System.out.println("fixer_output = " + fixer_output);


        } catch (IOException e) {

            // Throw pipeline exception
            throw new PipelineException("Preparing receptor with PDBFixer failed.", e);
        }
    }
}
