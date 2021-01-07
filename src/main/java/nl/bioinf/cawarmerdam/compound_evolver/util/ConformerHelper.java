package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.PipelineException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ConformerHelper {
    private ConformerHelper() {
    }

    /**
     * Reads a conformer from a file at the specified index.
     *
     * @param minimizedConformersFilePath The path to the file that holds multiple conformers.
     * @param conformerIndex              The index at which the conformer resides (zero-indexed).
     * @return the conformers molecule instance.
     * @throws PipelineException if the conformer could not be obtained, or the minimized conformer file could not be
     *                           imported.
     */
    @Nullable
    public static Molecule getConformer(Path minimizedConformersFilePath, int conformerIndex) throws PipelineException {
        try {
            // Create importer for file
            MolImporter importer = new MolImporter(minimizedConformersFilePath.toFile());

            // Current conformer index
            int currentConformerIndex = 0;

            // read the first molecule from the file
            Molecule m = importer.read();
            while (m != null && !(conformerIndex == currentConformerIndex)) {
                m = readSafe(importer);
                currentConformerIndex += 1;
            }
            importer.close();
            // The while loop should exit whenever the considered conformer index is reached. If the two indices
            // are not equal something went wrong (index is probably out of range) and an exception should be thrown.
            if (conformerIndex != currentConformerIndex) {
//                throw new PipelineException(String.format("Could not obtain conformer %d from '%s'",
//                        conformerIndex,
//                        minimizedConformersFilePath.getFileName()));
                System.err.println("Conformer index not found: " + conformerIndex + ". Current index: " + currentConformerIndex);
                return null;
            }
            // Return the molecule
            return m;
        } catch (IOException e) {
            e.printStackTrace();
            throw new PipelineException("Could not import minimized file", e);
        }
    }

    /**
     * Gets the amount of conformers in a conformer file
     *
     * @param conformerPath the file to get the conformer amount from
     * @return the amount of conformers
     * @throws IOException when the conformer file can't be read
     */
    public static int getConformerCount(Path conformerPath) throws IOException {
        MolImporter importer = new MolImporter(conformerPath.toFile());

        // Current conformer index
        int currentConformerIndex = 0;

        // read the first molecule from the file
        Molecule m = importer.read();
        while (m != null) {
            m = readSafe(importer);
            currentConformerIndex += 1;


        }
        importer.close();
        return currentConformerIndex;
    }

    private static Molecule readSafe(MolImporter importer) throws IOException {
        // read the next molecule from the input file
        try {
            return importer.read();
        } catch (MolFormatException e) {
            try {
                Thread.sleep(2000);
                return importer.read();
            } catch (InterruptedException | MolFormatException e2) {
                System.err.println("Error occured when loading conformer from file: ");
                System.err.println("minimizedConformersFilePath = " + importer.getFileName());
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Method exporting a list of molecules to the given file path.
     *
     * @param fixedConformersPath The file path that the list of molecules should be written to.
     * @param alignedMolecules    The list of molecule to write.
     * @throws PipelineException if the molecules could not be exported.
     */
    public static void exportConformers(Path fixedConformersPath, List<Molecule> alignedMolecules) throws PipelineException {
        // Create exporter
        MolExporter exporter;
        try {
            // Initialize exporter
            exporter = new MolExporter(fixedConformersPath.toString(), "sdf");

            // Loop through the conformers by using conformer count
            for (Molecule molecule : alignedMolecules) {
                // Do plus 1 to only pick conformers and not the reference
                exporter.write(molecule);
            }
            // Close the exporter
            exporter.close();
        } catch (IOException e) {
            throw new PipelineException("Could not export aligned conformers", e);
        }
    }

}
