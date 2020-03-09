package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.formats.MolExporter;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class NNHelper {
    private final Path nn_path;
    private final Path nn_model;
    private final Path conda_wrapper;
    private final Path pipeline_path;

    public NNHelper(Path nn_path, Path nn_model, Path conda_wrapper, Path pipeline_path) {
        this.nn_path = nn_path;
        this.nn_model = nn_model;
        this.conda_wrapper = conda_wrapper;
        this.pipeline_path = pipeline_path;
    }

    public void getMoleculeScores(List<Candidate> candidates) throws IOException, InterruptedException {
        File test_path = pipeline_path.resolve("test.csv").toFile();
        File preds_path = pipeline_path.resolve("preds.csv").toFile();
        if (!test_path.exists()) {
            test_path.createNewFile();
        }
        FileWriter writer = new FileWriter(test_path);
        writer.write("smiles\n");
        for (Candidate c:candidates) {
            writer.write(MolExporter.exportToFormat(c.getPhenotype(), "smiles") + "\n");
        }
        writer.close();
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(
                conda_wrapper.toString(),
                "python",
                nn_path.toString(),
                "--test_path",
                test_path.toString(),
                "--checkpoint_path",
                nn_model.toString(),
                "--preds_path",
                preds_path.toString()
        );
        Process p = builder.inheritIO().start();
        p.waitFor();
        Scanner pred_scanner = new Scanner(preds_path);
        pred_scanner.nextLine();
        for(Candidate c:candidates) {
            String data = pred_scanner.nextLine();
            if (data.equals("")) {
                data = pred_scanner.nextLine();
            }
            c.setRawScore(Double.parseDouble(data.split(",")[1]));
            System.out.println(data);
        }
    }

}
