package nl.bioinf.cawarmerdam.compound_evolver.util;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SimilaritySelector {
    public static List<List<String>> getVariedReactants(List<List<String>> reactants, Path variation_executable, Path rdkit_wrapper, Path pipeline_location, long seed) throws IOException {
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < reactants.size(); i++) {
            File in_file = pipeline_location.resolve(i + ".smiles").toFile();
            File out_file = in_file.toPath().resolveSibling(i + "_out.smiles").toFile();
            System.out.println("in_file = " + in_file);
            in_file.createNewFile();
            FileWriter writer = new FileWriter(in_file);
            BufferedWriter buffer = new BufferedWriter(writer);
            for (String reactant : reactants.get(i)) {
                buffer.write(reactant + "\n");
            }
            buffer.close();
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(
                    rdkit_wrapper.toString(),
                    "python",
                    variation_executable.toString(),
                    in_file.getAbsolutePath(),
                    out_file.getAbsolutePath(),
                    String.valueOf(seed)
            );
            builder.inheritIO();
            Process p = builder.start();
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            FileReader reader = new FileReader(out_file);
            BufferedReader bufferedReader = new BufferedReader(reader);
            out.add(bufferedReader.lines().map(s -> s.replace("\n", "")).collect(Collectors.toList()));
        }

        return out;
    }
}
