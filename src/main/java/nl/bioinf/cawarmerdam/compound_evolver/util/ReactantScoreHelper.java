package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.reaction.AtomIdentifier;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.util.iterator.MoleculeIterator;
import com.chemaxon.search.mcs.MaxCommonSubstructure;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReactantScoreHelper {

    public static logPPlugin logPPlugin = new logPPlugin();

    public static List<Double> getReactantScores(Candidate candidate, CompoundEvolver.FitnessMeasure measure) throws IOException {
        if (candidate.getScoredConformersFile() == null) {
            return null;
        }
        List<Double> reactantscores = new ArrayList<>();
        Map<MolAtom, AtomIdentifier> atommap = candidate.getAtommap();
        Molecule[] reactants = candidate.getReactants();
        Molecule phenotype = candidate.getPhenotype();
        MolImporter importer = new MolImporter(candidate.getScoredConformersFile().toFile());
        Molecule scored = getScorpScores(importer);
        if (scored == null) {
            return null;
        }
        // Find the max common substructure, aka the mapping from unscored to scored molecule
        MaxCommonSubstructure substructure = MaxCommonSubstructure.newInstance();
        substructure.setQuery(phenotype);
        substructure.setTarget(scored);
        int[] mapping = substructure.find().getAtomMapping();
        // Go through all the atoms in the phenotype
        for (int i = 0; i < phenotype.atoms().size(); i++) {
            MolAtom atom = phenotype.atoms().get(i);
            AtomIdentifier identifier = atommap.get(atom);

            // It should have an identifier, it should be mapped, and it should be part of the reactants
            if (identifier != null && mapping[i] != -1 && identifier.getMoleculeIndex() != -1) {
                //get atom score
                MolAtom scoredatom = scored.atoms().get(mapping[i]);
                double score = scoredatom.getProperty("score") == null ? 0.0D : (double) scoredatom.getProperty("score");

                //get matching reactant atom and set score
                MolAtom reactantatom = reactants[identifier.getMoleculeIndex()].getAtom(identifier.getAtomIndex());
                if (reactantatom != null) {
                    reactantatom.putProperty("score", score);
                }
            }
        }

        for (Molecule reactant : reactants) {
            double normalizationfactor = measure == CompoundEvolver.FitnessMeasure.LIGAND_EFFICIENCY ? (reactant.getAtomCount() - reactant.getExplicitHcount()) : 1.0D;

            double score = reactant.atoms().stream().mapToDouble(molAtom ->
                    molAtom.getProperty("score") == null ? 0.0D : ((double) molAtom.getProperty("score") / normalizationfactor)
            ).sum();

            score = measure == CompoundEvolver.FitnessMeasure.LIGAND_LIPOPHILICITY_EFFICIENCY ? calculateLigandLipophilicityEfficiency(reactant, score) : score;
            // Sum all the scores for each reactant
            reactantscores.add(score);
        }
        return reactantscores;
    }

    public static double calculateLigandLipophilicityEfficiency(Molecule reactant, double score) {
        final double kcalToJouleConstant = 4.186798188;
        try {
            logPPlugin.setMolecule(reactant);
            logPPlugin.run();
            return Math.log(-score * kcalToJouleConstant) - logPPlugin.getlogPTrue();
        } catch (PluginException e) {
            e.printStackTrace();
        }
        return 0.0D;
    }

    public static Molecule getScorpScores(MolImporter importer) {
        double best = Double.NEGATIVE_INFINITY;
        Molecule best_mol = null;
        for (MoleculeIterator it = importer.getMoleculeIterator(); it.hasNext(); ) {
            Molecule m = it.next();
            if (m.properties().get("TOTAL") != null) {
                double score = Double.parseDouble(m.properties().get("TOTAL").getPropValue().toString());
                if (score > best) {
                    best = score;
                    best_mol = m;
                    String[] contacts = m.properties().get("CONTACTS").getPropValue().toString().split("\\n");
                    for (String contact : contacts) {
                        String[] contents = contact.replace("'", "").split(",");
                        double atom_score = Double.parseDouble(contents[contents.length - 1]);
                        int temp = 100;
                        for (int i = 0; i < 10; i++) {
                            temp = contents[0].indexOf(String.valueOf(i)) < temp && contents[0].contains(String.valueOf(i)) ? contents[0].indexOf(String.valueOf(i)) : temp;
                        }
                        int atom_index = Integer.parseInt(contents[0].substring(temp)) - 1;
                        m.atoms().get(atom_index).putProperty("score", atom_score);
                    }
                }
            }
        }
        return best_mol;
    }
}
