package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolExporter;
import chemaxon.marvin.calculations.ConformerPlugin;
import chemaxon.struc.Molecule;
import org.openbabel.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.hsqldb.lib.tar.TarHeaderField.size;

public class ConformerPlacementStep implements PipelineStep<String,vectorVector3> {

    private final OBSmartsPattern sp;
    private OBAlign obAlign;

    public ConformerPlacementStep(OBMol referenceMolecule) {
        obAlign = new OBAlign();
        obAlign.SetRefMol(referenceMolecule);
        this.sp = new OBSmartsPattern();
        sp.Init("CNN");
    }

    private OBMol convertToOBMol(String inputStringMolecule) {
        OBConversion conversion = new OBConversion();
        OBMol mol = new OBMol();
        conversion.SetInFormat("sdf");
        conversion.ReadString(mol, inputStringMolecule);
        return mol;
    }

    @Override
    public vectorVector3 execute(String targetMolecule) {
        // Set target molecule
        obAlign.SetTargetMol(convertToOBMol(targetMolecule));
        obAlign.Align();
        return obAlign.GetAlignment();

    }

    public void obFit(OBMol molmv) throws Exception {
    }
}
