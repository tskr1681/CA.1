package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.struc.Molecule;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class SimilarityHelper {

    private final Path helper_exe;
    private final Path rdkit_wrapper;
    private boolean debug = false;

    public SimilarityHelper(Path helper_exe, Path rdkit_wrapper) {
        this.helper_exe = helper_exe;
        this.rdkit_wrapper = rdkit_wrapper;
    }

    /**
     * Gets a list of similarities from a smiles file to a reference molecule at a specific index.
     * @param reference_index The index in the file of the reference molecule to compare to
     * @param reactants A smiles file of reactants
     * @param size The amount of smiles in the file
     * @return A list of similarities compared to the reference
     */
    public double[] similarityList(int reference_index, File reactants, int size) {
        double[] similarities = new double[size];
        ProcessBuilder builder = new ProcessBuilder(
                this.rdkit_wrapper.toString(),
                "python",
                this.helper_exe.toString(),
                reactants.toString(),
                String.valueOf(reference_index)
        );

        try {
            Process out = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(out.getInputStream()));
            String line;
            int i = 0;
            while (((line = reader.readLine()) != null) && i < size) {
                similarities[i] = Double.parseDouble(line);
                i++;
            }

            if (debug) {
                reader = new BufferedReader(new InputStreamReader(out.getErrorStream()));
                while (((line = reader.readLine()) != null)) {
                    System.out.println("Similarity helper produced the following error: " + line);
                }
                reader.close();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return similarities;
    }

    public static double similarity(Molecule m1, Molecule m2) {
        //TODO make this an actual similarity function
        return 1-(m1.getMass() - m2.getMass())/(m1.getMass()+m2.getMass());
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
