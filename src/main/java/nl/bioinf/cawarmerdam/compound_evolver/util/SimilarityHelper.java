package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.descriptors.MDGeneratorException;
import chemaxon.descriptors.PFParameters;
import chemaxon.descriptors.PharmacophoreFingerprint;
import chemaxon.struc.Molecule;

public class SimilarityHelper {

    public static double similarity(Molecule m1, Molecule m2) {
        //TODO analyse the results of this. Do they seem decent for our purposes?
        PharmacophoreFingerprint firstFingerprint = new PharmacophoreFingerprint(new PFParameters());
        PharmacophoreFingerprint secondFingerprint = new PharmacophoreFingerprint(new PFParameters());
        // Try to get the tanimoto dissimilarity score
        try {
            firstFingerprint.generate(m1);
            secondFingerprint.generate(m2);
            return 1-firstFingerprint.getTanimoto(secondFingerprint);
        } catch (MDGeneratorException e) {
            // Return a dissimilarity of 1 if the fingerprints could not be generated
            return 1;
        }
    }

}
