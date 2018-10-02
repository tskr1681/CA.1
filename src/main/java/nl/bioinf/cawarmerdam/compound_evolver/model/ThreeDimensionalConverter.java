package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolConverter;
import chemaxon.formats.MolExporter;
import chemaxon.jep.function.In;
import chemaxon.struc.Molecule;

import java.io.*;

public class ThreeDimensionalConverter implements PipelineStep<Molecule, OutputStream> {
    @Override
    public OutputStream execute(Molecule value) {
        return null;
    }

    private void generateThreeDimensionalMolecule(String smilesMolecule, int i) {
        MolConverter.Builder molBuilder = new MolConverter.Builder();
        InputStream inputStream = new ByteArrayInputStream(smilesMolecule.getBytes());
        OutputStream outputStream = new ByteArrayOutputStream();
        molBuilder.addInput(inputStream, "smiles");
        molBuilder.setOutput(outputStream, "sdf");
        molBuilder.setOutputFlags(MolExporter.TEXT);
        molBuilder.clean(3);
        System.out.println("smilesMolecule = " + smilesMolecule);
        try {
            MolConverter mc = molBuilder.build();
            mc.convert();
            mc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
