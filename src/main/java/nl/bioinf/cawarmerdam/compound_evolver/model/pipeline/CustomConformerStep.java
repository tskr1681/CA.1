package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.util.FixArom;
import org.apache.tika.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CustomConformerStep implements PipelineStep<Candidate, Candidate> {
    private final Path wrapper;
    private final Path script;
    private final Path anchor;
    private final Path pipeline;
    private final int confs;
    private final double rmsd;

    public CustomConformerStep(Path pipeline, Path wrapper, Path script, Path anchor, int confs, double rmsd) {
        this.wrapper = wrapper;
        this.script = script;
        this.confs = confs;
        this.anchor = anchor;
        this.pipeline = pipeline;
        this.rmsd = rmsd;
    }

    /**
     * Method that executes the task of the considered pipeline step.
     *
     * @param value The input, and possibly, simultaneously the output of the previous pipeline step's execute method.
     * @return the output, and possibly, simultaneously the input of the following pipeline step's execute method.
     * @throws PipelineException if a pipeline related exception occurred.
     */
    @Override
    public Candidate execute(Candidate value) throws PipelineException {
        run(value);
        return value;
    }

    private void run(Candidate candidate) throws PipelineException {
        ProcessBuilder builder = new ProcessBuilder();

        try {
            candidate.setConformersFile(getConformerFileName(candidate));
            candidate.setFixedConformersFile(getConformerFileName(candidate));
            FixArom.fixArom(candidate.getPhenotype());
            builder.command(
                    this.wrapper.toString(),
                    "python",
                    this.script.toString(),
                    candidate.getPhenotypeSmiles(),
                    this.anchor.toString(),
                    String.valueOf(confs),
                    candidate.getFixedConformersFile().toString(),
                    String.valueOf(rmsd)
            );
            Process p = builder.start();
            p.waitFor();
            if (IOUtils.toString(p.getErrorStream()).contains("ImportError") || IOUtils.toString(p.getInputStream()).contains("ImportError") || !candidate.getConformersFile().toFile().exists()) {
                throw new PipelineException("RDKit wrapper is having issues!");
            }
        } catch (IOException | InterruptedException e) {
            throw new PipelineException("Custom conformer script failed");
        }
    }

    /**
     * Method that retrieves the path for the conformers that are generated.
     *
     * @param candidate The candidate for which the file is meant.
     * @return the path for the conformers file.
     */
    private Path getConformerFileName(Candidate candidate) {
        return Paths.get(pipeline.toString(),
                String.valueOf(candidate.getIdentifier()),
                "conformers.sd").toAbsolutePath();
    }

}
