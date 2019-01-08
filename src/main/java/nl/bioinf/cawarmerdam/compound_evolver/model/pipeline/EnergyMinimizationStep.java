package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import com.chemaxon.search.mcs.MaxCommonSubstructure;
import com.chemaxon.search.mcs.McsSearchOptions;
import com.chemaxon.search.mcs.McsSearchResult;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.ExclusionShape;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class EnergyMinimizationStep implements PipelineStep<Candidate, Candidate> {

    private String forceField;
    private Path anchorFilePath;

    public EnergyMinimizationStep(String forcefield, Path anchorFilePath) {
        this.forceField = forcefield;
        this.anchorFilePath = anchorFilePath;
    }

    static Path convertFileWithProcessBuilder(Path mabFilePath, ProcessBuilder builder) throws IOException, InterruptedException {
        final Process p = builder.start();

        p.waitFor();

        if (Files.exists(mabFilePath)) {
            throw new IOException();
        }

        return mabFilePath;
    }
}
