package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.formats.MolImporter;
import chemaxon.reaction.AtomIdentifier;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.util.iterator.MoleculeIterator;
import com.chemaxon.search.mcs.MaxCommonSubstructure;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReactantScoreHelper {

    public static Map<Molecule, Double> getReactantScores(Candidate candidate) throws IOException {
        Map<Molecule, Double> reactantscores = new HashMap<>();
        Map<MolAtom, AtomIdentifier> atommap = candidate.getAtommap();
        Molecule[] reactants = candidate.getReactants();
        Molecule phenotype = candidate.getPhenotype();
        MolImporter importer = new MolImporter(candidate.getScoredConformersFile().toFile());
        Molecule scored = getScorpScores(importer);

        MaxCommonSubstructure substructure = MaxCommonSubstructure.newInstance();
        substructure.setQuery(phenotype);
        substructure.setTarget(scored);
        int[] mapping = substructure.find().getAtomMapping();
        for (int i = 0; i < phenotype.atoms().size(); i++) {
            MolAtom atom = phenotype.atoms().get(i);
            AtomIdentifier identifier = atommap.get(atom);
            if (identifier != null) {
                //get atom score
                double score = (double) scored.atoms().get(mapping[i]).getProperty("score");

                //get matching reactant atom and set score
                reactants[identifier.getMoleculeIndex()].getAtom(identifier.getAtomIndex()).putProperty("score", score);
            }
        }

        for (Molecule reactant : reactants) {
            reactantscores.put(reactant , reactant.atoms().stream().mapToDouble(molAtom ->
                    molAtom.getProperty("score") == null ? 0.0D : (double) molAtom.getProperty("score")
            ).sum());
        }
        return reactantscores;
    }

    public static Molecule getScorpScores(MolImporter importer) {
        double best = Double.NEGATIVE_INFINITY;
        Molecule best_mol = null;
        for (MoleculeIterator it = importer.getMoleculeIterator(); it.hasNext(); ) {
            Molecule m = it.next();
            double score = Double.parseDouble(m.properties().get("TOTAL").getPropValue().toString());
            if (score > best) {
                best = score;
                best_mol = m;
                String[] contacts = m.properties().get("CONTACTS").getPropValue().toString().split("\\n");
                for (String contact : contacts) {
                    String[] contents = contact.replace("'", "").split(",");
                    double atom_score = Double.parseDouble(contents[contents.length - 1]);
                    int atom_index = Integer.parseInt(contents[0].substring(2)) - 1;
                    m.atoms().get(atom_index).putProperty("score", atom_score);
                }
            }
        }
        return best_mol;
    }

    public static void main(String[] args) {
        try {
            MolImporter importer = new MolImporter("D:\\Hanze\\Stage\\CompoundEvolver\\fixed-conformers_3d_scorp.sdf");
            for (MolAtom atom : getScorpScores(importer).atoms()) {
                System.out.println("atom.getProperty(\"score\") = " + atom.getProperty("score"));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }
}
