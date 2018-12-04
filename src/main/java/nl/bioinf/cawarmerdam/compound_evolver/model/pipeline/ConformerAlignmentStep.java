package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.alignment.Alignment;
import chemaxon.marvin.alignment.AlignmentException;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConformerAlignmentStep implements PipelineStep<Candidate, Candidate> {

    private Molecule referenceMolecule;

    public ConformerAlignmentStep(Path anchor) throws PipelineException {
        this.referenceMolecule = importReferenceMolecule(anchor);
    }

    /**
     * Method responsible for importing the reference molecule from a given path.
     *
     * @param anchor The anchor file path.
     * @return the imported molecule
     * @throws PipelineException if the given anchor could not be imported.
     */
    private Molecule importReferenceMolecule(Path anchor) throws PipelineException {
        MolImporter importer = null;
        try {
            importer = new MolImporter(anchor.toFile());

            return importer.read();

        } catch (IOException e) {
            throw new PipelineException("Could not read reference fragment", e);        }
    }

    @Override
    public Candidate execute(Candidate candidate) throws PipelineException {
        // Get the path of the file with the conformers
        Path conformersFilePath = candidate.getConformersFile();
        // Create the filename of the resulting file
        String fixedConformerFileName = "fixed-" + conformersFilePath.getFileName();
        // Sets the fixed conformer file path to the candidate
        candidate.setFixedConformersFile(conformersFilePath.resolveSibling(fixedConformerFileName));

        // Align and export the conformers
        List<Molecule> alignedMolecules = alignConformersFromPath(conformersFilePath);
        exportAlignedConformers(candidate.getFixedConformersFile(), alignedMolecules);
        return candidate;
    }

    /**
     * Method exporting a list of molecules to the given file path.
     *
     * @param fixedConformersPath The file path that the list of molecules should be written to.
     * @param alignedMolecules The list of molecule to write.
     * @throws PipelineException if the molecules could not be exported.
     */
    private void exportAlignedConformers(Path fixedConformersPath, List<Molecule> alignedMolecules)
            throws PipelineException {

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

    /**
     * Method aligning conformers that are read from the given path.
     *
     * @param conformersPath The path that the conformers are read from.
     * @return the list of aligned conformers.
     * @throws PipelineException if the conformers could not be read or aligned.
     */
    private List<Molecule> alignConformersFromPath(Path conformersPath) throws PipelineException {
        List<Molecule> alignedMolecules = new ArrayList<>();

        // Create importer for file
        MolImporter importer = null;
        try {
            importer = new MolImporter(conformersPath.toFile());

            // read the first molecule from the file
            Molecule m = importer.read();
            while (m != null) {

                Alignment alignment = constructAlignment();

                // Align molecules
                alignment.addMolecule(m, false, true);
                alignment.align();
                alignedMolecules.add(alignment.getMoleculeWithAlignedCoordinates(1));

                // read the next molecule from the input file
                m = importer.read();
            }
            importer.close();
        } catch (IOException e) {
            throw new PipelineException("Could not read conformers for alignment", e);
        } catch (AlignmentException e) {
            throw new PipelineException("Could add conformers for alignment", e);
        }
        return alignedMolecules;
    }

    /**
     * Method making new alignment instance with the reference molecule.
     *
     * @return the alignment with a reference molecule added.
     * @throws PipelineException if the reference fragment could not be added.
     */
    private Alignment constructAlignment() throws PipelineException {
        Alignment alignment = new Alignment();

        try {
            alignment.addMolecule(referenceMolecule, false, false);
        } catch (AlignmentException e) {
            throw new PipelineException("The reference fragment could not be set");
        }
        return alignment;
    }
}
