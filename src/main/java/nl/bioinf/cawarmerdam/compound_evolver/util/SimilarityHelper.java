package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.struc.Molecule;

public class SimilarityHelper {

    public static double similarity(Molecule m1, Molecule m2) {
        //TODO make this an actual similarity function
        return 1-(m1.getMass() - m2.getMass())/(m1.getMass()+m2.getMass());
    }
}
