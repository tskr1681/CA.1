package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolExporter;
import chemaxon.struc.Molecule;
import org.openbabel.OBAlign;
import org.openbabel.OBMol;
import org.openbabel.vectorVector3;

public class ConformerPlacementStep implements PipelineStep<Molecule,vectorVector3> {

    private OBAlign obAlign;

    public ConformerPlacementStep(OBMol referenceMolecule) {
        obAlign = new OBAlign();
        obAlign.SetRefMol(referenceMolecule);
    }

    @Override
    public vectorVector3 execute(Molecule targetMolecule) {
//        obAlign.SetTargetMol(targetMolecule);
        obAlign.Align();
        return obAlign.GetAlignment();
    }
}
