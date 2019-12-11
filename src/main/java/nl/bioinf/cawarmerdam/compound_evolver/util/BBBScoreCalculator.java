package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.calculations.Ring;
import chemaxon.calculations.TopologyAnalyser;
import chemaxon.marvin.calculations.HBDAPlugin;
import chemaxon.marvin.calculations.TPSAPlugin;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.marvin.calculations.pKaPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;

import java.util.Arrays;
import java.util.OptionalDouble;

public class BBBScoreCalculator {

    private static final logPPlugin lpPlugin = new logPPlugin();
    private static final HBDAPlugin hbdaPlugin = new HBDAPlugin();
    private static final TPSAPlugin tpsaPlugin = new TPSAPlugin();
    private static final TopologyAnalyser topologyAnalyser = new TopologyAnalyser();
    private static final pKaPlugin pkaCalc = new pKaPlugin();


    //Don't allow instantiation of the class
    private BBBScoreCalculator() {
    }

    private static double arom_val(int arom) {
        switch (arom) {
            case 0:
                return 0.336376d;
            case 1:
                return 0.816016d;
            case 2:
                return 1;
            case 3:
                return 0.691115d;
            case 4:
                return 0.199399d;
            default:
                return 0.0d;
        }
    }

    private static double HA_val(double HA) {
        return HA <= 5 || HA > 45 ? 0 : (1 / 0.624231) * (0.0000443 * Math.pow(HA, 3) - 0.004556 * Math.pow(HA, 2) + 0.12775 * HA - 0.463);
    }

    private static double MWHBN_val(double mwhbn) {
        return mwhbn <= 0.05d || mwhbn > 0.45d ? 0 : (1 / 0.72258) * (26.733 * Math.pow(mwhbn, 3) - 31.495 * Math.pow(mwhbn, 2) + 9.5202 * mwhbn - 0.1358);
    }

    private static double TPSA_val(double tpsa) {
        return tpsa == 0 || tpsa > 120 ? 0 : (1 / 0.9598) * (-0.0067 * tpsa + 0.9598);
    }

    private static double PKa_val(double PKa) {
        return PKa <= 3 || PKa > 11 ? 0 : (1 / 0.597488) * (0.00045068 * Math.pow(PKa, 4) - 0.016331 * Math.pow(PKa, 3) + 0.18618 * Math.pow(PKa, 2) - 0.71043 * PKa + 0.8579);
    }

    /**
     * Calculates the Blood Brain Barrier Score for a given molecule, based on this paper: <href>https://pubs.acs.org/doi/10.1021/acs.jmedchem.9b01220</href>
     *
     * @param m The molecule to get the BBB score for
     * @return the BBB score for the molecule, or 0 if an error is thrown while trying to calculate the BBB score
     */
    public static double getBBB(Molecule m) {
        Ring r = new Ring();
        r.setMolecule(m);

        // Set up plugins
        try {
            lpPlugin.setMolecule(m);

            lpPlugin.run();

            hbdaPlugin.setMolecule(m);
            hbdaPlugin.setExcludeSulfur(true);
            hbdaPlugin.setExcludeHalogens(true);
            hbdaPlugin.run();

            tpsaPlugin.setMolecule(m);
            tpsaPlugin.run();

            topologyAnalyser.setMolecule(m);

            pkaCalc.setMolecule(m);
            pkaCalc.run();

            // All variables
            double mr = m.getMass();
            double hbd = hbdaPlugin.getDonorAtomCount();
            double hba = hbdaPlugin.getAcceptorAtomCount();
            double psa = tpsaPlugin.getSurfaceArea();
            double mwhbn = Math.pow(mr, -0.5) * (hba + hbd);

            double[] acidicpKaValues = pkaCalc.getMacropKaValues(pKaPlugin.ACIDIC);
            double[] basicpKaValues = pkaCalc.getMacropKaValues(pKaPlugin.BASIC);
            OptionalDouble maxBasicpKa = basicpKaValues != null ? Arrays.stream(basicpKaValues).max() : OptionalDouble.empty();
            OptionalDouble minAcidicpKa = acidicpKaValues != null ? Arrays.stream(acidicpKaValues).min() : OptionalDouble.empty();
            double pKa;
            if (maxBasicpKa.isPresent() && maxBasicpKa.getAsDouble() >= 5) {
                pKa = maxBasicpKa.getAsDouble();
            } else if (minAcidicpKa.isPresent() && minAcidicpKa.getAsDouble() < 9) {
                pKa = minAcidicpKa.getAsDouble();
            } else {
                pKa = 8.11d;
            }

            int arom = r.aromaticRingCount();
            double bbb_score = arom_val(arom) + HA_val(m.getAtomCount() - m.getExplicitHcount()) +
                    MWHBN_val(mwhbn) * 1.5d + TPSA_val(psa) * 2.0d + PKa_val(pKa) * 0.5d;
            return bbb_score;
        } catch (PluginException e) {
            e.printStackTrace();
            return 0.0d;
        }
    }
}
