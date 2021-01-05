package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.struc.BondType;
import chemaxon.struc.Molecule;

public class FixArom {
    /**
     * Extremely hacky fix for chemaxon aromaticity weirdness. If there is an aromatic bond in a cycle, make the full cycle aromatic, and then dearomatize to get rid of weird half-aromatic stuff
     *
     * @param m the molecule to fix up
     * @return a fixed molecule
     */
    public static Molecule fixArom(Molecule m) {
        m.dearomatize();
        int[][] bonds = m.getSSSRBonds();
        for (int[] bond : bonds) {
            for (int b : bond) {
                if (m.getBond(b).getBondType() == BondType.AROMATIC) {
                    for (int c : bond) {
                        m.getBond(c).setType(BondType.AROMATIC.ordinal());
                    }
                    break;
                }
            }
        }
        m.dearomatize();
        return m;
    }
}
