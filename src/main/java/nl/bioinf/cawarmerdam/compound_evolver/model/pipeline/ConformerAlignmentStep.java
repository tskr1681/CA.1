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
        Path conformersFilePath = candidate.getConformersFile();
        String fixedConformerFileName = "fixed-" + conformersFilePath.getFileName();
        candidate.setFixedConformersFile(conformersFilePath.resolveSibling(fixedConformerFileName));
        alignConformers(conformersFilePath, candidate.getFixedConformersFile());
        return candidate;
    }

    private void alignConformers(Path conformersPath, Path fixedConformersPath) throws PipelineException {
        List<Molecule> alignedMolecules = addConformersToAlignment(conformersPath);

        exportAlignedConformers(fixedConformersPath, alignedMolecules);
    }

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
            throw new PipelineException("Could not write aligned conformers", e);
        }
    }

    private List<Molecule> addConformersToAlignment(Path conformersPath) throws PipelineException {
        List<Molecule> alignedMolecules = new ArrayList<>();

        Alignment alignment = constructAlignment();

        // Create importer for file
        MolImporter importer = null;
        try {
            importer = new MolImporter(conformersPath.toFile());

            // read the first molecule from the file
            Molecule m = importer.read();
            while (m != null) {

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
