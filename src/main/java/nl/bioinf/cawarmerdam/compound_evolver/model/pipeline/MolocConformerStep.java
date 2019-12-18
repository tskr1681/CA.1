package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolExporter;
import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.util.ConformerHelper;
import nl.bioinf.cawarmerdam.compound_evolver.util.FixArom;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class MolocConformerStep implements PipelineStep<Candidate, Candidate> {

    private final Path filePath;
    private final int conformerCount;
    private static String mcnfExecutable;
    private static String msmabExecutable;
    private static final ConformerPlugin plugin = new ConformerPlugin();

    /**
     * Constructor for three dimensional converter step.
     *
     * @param filePath The path that corresponds to the location of pipeline files for the entire run
     * @param conformerCount The amount of conformers that should be generated.
     */
    public MolocConformerStep(Path filePath, int conformerCount, String mcnfExecutable, String msmabExecutable) {
        this.filePath = filePath;
        this.conformerCount = conformerCount;
        setMcnfExecutable(mcnfExecutable);
        setMsmabExecutable(msmabExecutable);
    }

    public static void setMcnfExecutable(String mcnfExecutable) {
        if (MolocConformerStep.mcnfExecutable == null)
            MolocConformerStep.mcnfExecutable = mcnfExecutable;
    }

    public static void setMsmabExecutable(String msmabExecutable) {
        if (MolocConformerStep.msmabExecutable == null)
            MolocConformerStep.msmabExecutable = msmabExecutable;
    }

    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        Path conformerFilePath = getConformerFileName(candidate);
        try {
            Molecule[] conformers = createConformers(candidate);
            MolExporter exporter = new MolExporter(conformerFilePath.toString(), "sdf");
            if (conformers != null) {
                for (Molecule conformer : conformers) {
                    exporter.write(conformer);
                }
            }
            exporter.close();
        } catch (IOException e) {
            throw new PipelineException("Could not create conformers", e);
        }
        candidate.setConformersFile(conformerFilePath);
        return candidate;
    }

    /**
     * Method that retrieves the path for the conformers that are generated.
     *
     * @param candidate The candidate for which the file is meant.
     * @return the path for the conformers file.
     */
    private Path getConformerFileName(Candidate candidate) {
        return Paths.get(filePath.toString(),
                String.valueOf(candidate.getIdentifier()),
                "conformers.sd").toAbsolutePath();
    }

    /**
     * Method that creates a set amount of conformers for the given molecule.
     *
     * @param candidate The molecule to create conformers from.
     * @return a list of conformers
     */
    private Molecule[] createConformers(Candidate candidate) throws PipelineException {
        try {
            return runMoloc(getConformerFileName(candidate), makeSmiles(candidate), this.conformerCount);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static Molecule[] runMoloc(Path filename, Path smiles, int conformerCount) throws PipelineException {
        try {
            String tempDir = filename.getParent().resolve("temp.sd").toString();
            // Build process
            ProcessBuilder builder = new ProcessBuilder(
                    msmabExecutable,
                    "-s",
                    "-o",
                    tempDir,
                    smiles.toString()
            );

            // Start the process
            Process p = builder.start();

            p.waitFor();
            ProcessBuilder builder2 = new ProcessBuilder(
                    mcnfExecutable,
                    "-w0",
                    "-c3",
                    "-k" + conformerCount,
                    "-o",
                    filename.getFileName().toString(),
                    "temp.sd"
            );
            builder2.directory(filename.getParent().toFile());
            // Start the process
            Process p2 = builder2.start();
            p2.waitFor();

        } catch (InterruptedException | IOException e) {

            // Throw pipeline exception
            throw new PipelineException("Energy minimization with Moloc failed.", e);
        }
        fixSdf(filename);
        conformerCount = 0;
        try {
            conformerCount = ConformerHelper.getConformerCount(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Molecule[] conformers = new Molecule[conformerCount];
        for (int i = 0; i < conformerCount; i++) {
            conformers[i] = ConformerHelper.getConformer(filename, i);
        }
        return conformers;
    }

    /**
     * Generates a temporary smiles file and returns the location of said file
     * @param candidate the candidate to generate the smiles file for
     * @return the location of the generated smiles file
     * @throws IOException when writing the smiles file fails
     */
    private Path makeSmiles(Candidate candidate) throws IOException {
        Path smilesPath = getConformerFileName(candidate).getParent().resolve("temp.smiles");
        BufferedWriter writer = new BufferedWriter(new FileWriter(smilesPath.toFile()));
        Molecule m = candidate.getPhenotype();
        FixArom.fixArom(m);
        writer.write(MolExporter.exportToFormat(m, "smiles"));
        writer.flush();
        writer.close();
        return smilesPath;
    }

    private static void fixSdf(Path sdf) {
        ArrayList<String> lines = new ArrayList<>();
        String line;
        try
        {
            File f1 = sdf.toFile();
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null)
            {
                if (line.endsWith("Moloc")) {
                    line = line.substring(0,6) + " 0  0  0  0  0  0  0  0999 V2000";
                }
                if (line.strip().split(" +").length == 6) {
                    line = line + "  0";
                }
                lines.add(line + "\n");
            }
            fr.close();
            br.close();
            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);
            for (String l : lines) {
                out.write(l);
            }
            out.flush();
            out.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
