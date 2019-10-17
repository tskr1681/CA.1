package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.calculations.Ring;
import chemaxon.calculations.TopologyAnalyser;
import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.HBDAPlugin;
import chemaxon.marvin.calculations.TPSAPlugin;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule;

import java.util.stream.DoubleStream;

public class QuantitativeDrugEstimateCalculator {
    private static final double[] MW_PARAMS = {2.817d, 392.575d, 290.749d, 2.420d, 49.223d, 65.371d, 104.981d};
    private static final double[] ALOGP_PARAMS = {3.173d, 137.862d, 2.535d, 4.581d, 0.823d, 0.576d, 131.319d};
    private static final double[] HBD_PARAMS = {1.619d, 1010.051d, 0.985d, 1.0E-12d, 0.714d, 0.921d, 258.163d};
    private static final double[] HBA_PARAMS = {2.949d, 160.461d, 3.615d, 4.436d, 0.290d, 1.301d, 148.776d};
    private static final double[] PSA_PARAMS = {1.877d, 125.223d, 62.908d, 87.834d, 12.020d, 28.513d, 104.569d};
    private static final double[] ROTB_PARAMS = {0.010d, 272.412d, 2.558d, 1.566d, 1.272d, 2.758d, 105.442d};
    private static final double[] AROM_PARAMS = {3.218d, 957.737d, 2.275d, 1.0E-12d, 1.318d, 0.376d, 312.337d};
    private static final double[] ALERTS_PARAMS = {0.010d, 1199.094d, -0.090d, 1.0E-12d, 0.186d, 0.875d, 417.725d};

    private static logPPlugin lpPlugin = new logPPlugin();
    private static HBDAPlugin hbdaPlugin = new HBDAPlugin();
    private static TPSAPlugin tpsaPlugin = new TPSAPlugin();
    private static TopologyAnalyser topologyAnalyser = new TopologyAnalyser();

    private static final double[] weights = {0.66d, 0.46d, 0.05d, 0.61d, 0.06d, 0.65d, 0.48d, 0.95d};

    //Don't allow instantiation of the class
    private QuantitativeDrugEstimateCalculator() {
    }

    // A list of possibly problematic structural descriptors, because of either toxicity or instability
    private static final String[] alertParts = new String[]{
            "C(=O)O[C,H1].C(=O)O[C,H1].C(=O)O[C,H1]",
            "n1c([F,Cl,Br,I])cccc1",
            "C(=O)[Cl,Br,I,F]",
            "C=[C!r]O",
            "N#CC(=O)",
            "C(=O)N[NH2]",
            "[CH1](=O)",
            "[R0;D2][R0;D2][R0;D2][R0;D2]",
            "[CX4][Cl,Br,I]",
            "c1nnnn1C=O",
            "c1cc([NH2])ccc1",
            "[CH2R2]1N[CH2R2][CH2R2][CH2R2][CH2R2][CH2R2]1",
            "N=[N+]=[N-]",
            "N#N",
            "[CH2R2]1N[CH2R2][CH2R2][CH2R2][CH2R2][CH2R2][CH2R2]1",
            "[cR2]1[cR2][cR2]([Nv3X3,Nv4X4])[cR2][cR2][cR2]1[cR2]2[cR2][cR2][cR2]([Nv3X3,Nv4X4])[cR2][cR2]2",
            "[C,c](=O)[CX4,CR0X3,O][C,c](=O)",
            "C12C(NC(N1)=O)CSC2",
            "[C+,c+,C-,c-]",
            "c1c([OH])c([OH,NH2,NH])ccc1",
            "[O+,o+,S+,s+]",
            "C1(=[O,N])C=CC(=[O,N])C=C1",
            "C1(=[O,N])C(=[O,N])C=CC=C1",
            "C=[C!r]C#N",
            "[OR2,NR2]@[CR2]@[CR2]@[OR2,NR2]@[CR2]@[CR2]@[OR2,NR2]",
            "c1ccc2c(c1)ccc(=O)o2",
            "N[CH2]C#N",
            "[N,O,S]C#N",
            "N#CC[OH]",
            "[CR2]1[CR2][CR2][CR2][CR2][CR2][CR2]1",
            "[CR2]1[CR2][CR2]cc[CR2][CR2]1",
            "[CR2]1[CR2][CR2][CR2][CR2][CR2][CR2][CR2]1",
            "[CR2]1[CR2][CR2]cc[CR2][CR2][CR2]1",
            "[cR2]1[cR2]c([N+0X3R0,nX3R0])c([N+0X3R0,nX3R0])[cR2][cR2]1",
            "[cR2]1[cR2]c([N+0X3R0,nX3R0])[cR2]c([N+0X3R0,nX3R0])[cR2]1",
            "[cR2]1[cR2]c([N+0X3R0,nX3R0])[cR2][cR2]c1([N+0X3R0,nX3R0])",
            "[N!R]=[N!R]",
            "[C,c](=O)[C,c](=O)",
            "SS",
            "[CX2R0][NX3R0]",
            "C(=O)Onnn",
            "C1(=O)OCC1",
            "c1cc([Cl,Br,I,F])cc([Cl,Br,I,F])c1[Cl,Br,I,F]",
            "c1ccc([Cl,Br,I,F])c([Cl,Br,I,F])c1[Cl,Br,I,F]",
            "[Hg,Fe,As,Sb,Zn,Se,se,Te,B,Si]",
            "[NX3R0,NX4R0,OR0,SX2R0][CX4][NX3R0,NX4R0,OR0,SX2R0]",
            "C1NC(=O)NC(=O)1",
            "N[NH2]",
            "[OH]c1ccc([OH,NH2,NH])cc1",
            "C(=O)N[OH]",
            "C=[N!R]",
            "N=[CR0][N,n,O,S]",
            "I",
            "N=C=O",
            "[$([CH2]),$([CH][CX4]),$(C([CX4])[CX4])]=[$([CH2]),$([CH][CX4]),$(C([CX4])[CX4])]",
            "C=C=O",
            "S1C=CSC1=S",
            "C=!@CC=[O,S]",
            "[$([CH]),$(CC)]#CC(=O)[C,c]",
            "[$([CH]),$(CC)]#CS(=O)(=O)[C,c]",
            "C=C(C=O)C=O",
            "[$([CH]),$(CC)]#CC(=O)O[C,c]",
            "[NX2,nX3][OX1]",
            "s1c(S)nnc1NC=O",
            "NC[F,Cl,Br,I]",
            "[NX3,NX4][F,Cl,Br,I]",
            "n[OH]",
            "[N+](=O)[O-]",
            "[#7]-N=O",
            "[C,c]=N[OH]",
            "[C,c]=NOC=O",
            "[OR0,NR0][OR0,NR0]",
            "[CX4](F)(F)[CX4](F)F",
            "OO",
            "c1ccccc1OC(=O)[#6]",
            "c1ccccc1OC(=O)O",
            "P",
            "[cR,CR]~C(=O)NC(=O)~[cR,CR]",
            "a1aa2a3a(a1)A=AA=A3=AA=A2",
            "a21aa3a(aa1aaaa2)aaaa3",
            "a31a(a2a(aa1)aaaa2)aaaa3",
            "[CR0]=[CR0][CR0]=[CR0]",
            "[s,S,c,C,n,N,o,O]~[nX3+,NX3+](~[s,S,c,C,n,N])~[s,S,c,C,n,N]",
            "[s,S,c,C,n,N,o,O]~[n+,N+](~[s,S,c,C,n,N,o,O])(~[s,S,c,C,n,N,o,O])~[s,S,c,C,n,N,o,O]",
            "[*]=[N+]=[*]",
            "O1CCCCC1OC2CCC3CCCCC3C2",
            "[Si][F,Cl,Br,I]",
            "c1ccccc1C=Cc2ccccc2",
            "[SX3](=O)[O-,OH]",
            "[C,c]S(=O)(=O)O[C,c]",
            "S(=O)(=O)[O-,OH]",
            "S(=O)(=O)C#N",
            "[SX2]O",
            "OS(=O)(=O)[O-]",
            "[SX2H0][N]",
            "c12ccccc1(SC(S)=N2)",
            "c12ccccc1(SC(=S)N2)",
            "[C,c]=S",
            "SC=O",
            "[S-]",
            "[SH]",
            "*1[O,S,N]*1",
            "OS(=O)(=O)C(F)(F)F",
            "[SiR0,CR0](c1ccccc1)(c2ccccc2)(c3ccccc3)",
            "C#C"
    };

    /**
     * Gets the amount of possibly bad structural components in a molecule, as described in <href>https://doi.org/10.1002/cmdc.200700139</href>
     * @param m the molecule to get the alerts/bad structural components from
     * @return the amount of alerts
     * @throws SearchException when searching for a structural component fails
     */
    private static int getAlerts(Molecule m) throws SearchException {
        MolSearch s = new MolSearch();
        int alerts = 0;
        s.setTarget(m);
        for (String a : alertParts) {
            s.setQuery(a);
            alerts += s.getMatchCount();
        }
        return alerts;
    }

    /**
     * A copy of the asymmetric sigmoidal function as described in <href>https://www.nature.com/articles/nchem.1243#Sec7</href>
     * @param params the parameters for the function
     * @return the value of the ads for the given parameters
     */
    private static double ads(double x, double[] params) {
        double a, b, c, d, e, f, dx_max;
        a = params[0];
        b = params[1];
        c = params[2];
        d = params[3];
        e = params[4];
        f = params[5];
        dx_max = params[6];
        return ((a + (b / (1 + Math.exp(-1 * (x - c + d / 2) / e)) * (1 - 1 / (1 + Math.exp(-1 * (x - c - d / 2) / f))))) / dx_max);
    }

    /**
     * Calculates the Quantitive Estimate of Druglikeness (QED) for a given molecule, based on this paper: <href>https://www.nature.com/articles/nchem.1243#Sec7</href>
     * It should be noted that the values this function outputs are slightly different than those described in the paper,
     * because of a different implementation of the logP algorithm.
     * @param m The molecule to get the QED for
     * @return the QED value for the molecule, or 0 if an error is thrown while trying to calculate the QED
     */
    public static double getQED(Molecule m) {
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

        // All variables
        double mr = m.getMass();
        double alogp = lpPlugin.getlogPTrue();
        double hbd = hbdaPlugin.getDonorAtomCount();
        double hba = hbdaPlugin.getAcceptorAtomCount();
        double psa = tpsaPlugin.getSurfaceArea();
        double rotb = topologyAnalyser.rotatableBondCount();
        double arom = r.aromaticRingCount();
        double alerts = getAlerts(m);
        System.out.println("arom = " + arom);
        System.out.println("mr = " + mr);
        System.out.println("alogp = " + alogp);
        System.out.println("hbd = " + hbd);
        System.out.println("hba = " + hba);
        System.out.println("psa = " + psa);
        System.out.println("rotb = " + rotb);
        System.out.println("alerts = " + alerts);

        double qed = Math.exp((weights[0] * Math.log(ads(mr, MW_PARAMS)) +
                weights[1] * Math.log(ads(alogp, ALOGP_PARAMS)) +
                weights[2] * Math.log(ads(hbd, HBD_PARAMS)) +
                weights[3] * Math.log(ads(hba, HBA_PARAMS)) +
                weights[4] * Math.log(ads(psa, PSA_PARAMS)) +
                weights[5] * Math.log(ads(rotb, ROTB_PARAMS)) +
                weights[6] * Math.log(ads(arom, AROM_PARAMS)) +
                weights[7] * Math.log(ads(alerts, ALERTS_PARAMS)))/DoubleStream.of(weights).sum());

        System.out.println("qed = " + qed);
        return qed;
        } catch (PluginException | SearchException e) {
            e.printStackTrace();
            return 0.0d;
        }
    }

    public static void main(String[] args) throws MolFormatException {
        getQED(MolImporter.importMol("n3c1c(ncn1[C@H]2/C=C\\[C@@H](CO)C2)c(nc3N)NC4CC4"));
    }
}
