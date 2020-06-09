package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.formats.MolImporter;
import chemaxon.reaction.AtomIdentifier;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.util.iterator.MoleculeIterator;
import com.chemaxon.search.mcs.MaxCommonSubstructure;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReactantScoreHelper {

    public static List<Double> getReactantScores(Candidate candidate) throws IOException {
        System.out.println("Scored file = " + candidate.getScoredConformersFile());
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
        System.out.println("mapping = " + Arrays.toString(mapping));
        // Go through all the atoms in the phenotype
        for (int i = 0; i < phenotype.atoms().size(); i++) {
            MolAtom atom = phenotype.atoms().get(i);
            AtomIdentifier identifier = atommap.get(atom);

            // It should have an identifier, it should be mapped, and it should be part of the reactants
            if (identifier != null && mapping[i] != -1 && identifier.getMoleculeIndex() != -1) {
                //get atom score
                MolAtom scoredatom = scored.atoms().get(mapping[i]);
                System.out.println("scoredatom = " + scoredatom);
                double score = scoredatom.getProperty("score") == null ? 0.0D : (double) scoredatom.getProperty("score");

                //get matching reactant atom and set score
                MolAtom reactantatom = reactants[identifier.getMoleculeIndex()].getAtom(identifier.getAtomIndex());
                if (reactantatom != null) {
                    reactantatom.putProperty("score", score);
                }
            }
        }

        for (Molecule reactant : reactants) {
            // Sum all the scores for each reactant
            reactantscores.add(reactant.atoms().stream().mapToDouble(molAtom ->
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
            System.out.println("m.properties().getKeys() = " + Arrays.toString(m.properties().getKeys()));
            if (m.properties().get("TOTAL") != null) {
                System.out.println("m.properties().get(\"TOTAL\").getPropValue() = " + m.properties().get("TOTAL").getPropValue());
                double score = Double.parseDouble(m.properties().get("TOTAL").getPropValue().toString());
                if (score > best) {
                    best = score;
                    best_mol = m;
                    String[] contacts = m.properties().get("CONTACTS").getPropValue().toString().split("\\n");
                    for (String contact : contacts) {
                        String[] contents = contact.replace("'", "").split(",");
                        double atom_score = Double.parseDouble(contents[contents.length - 1]);
                        System.out.println("contents = " + Arrays.toString(contents));
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

//    public static void main(String[] args) {
//        try {
//            Reactor reactor = new Reactor();
//            Molecule reaction = new MolImporter("D:\\Hanze\\Stage\\CompoundEvolver\\compound-evolver\\test\\SarsUgi4CR.mrv").read();
//            reactor.setReaction(reaction);
//            List<Molecule> reactants = new ArrayList<>();
//            new MolImporter("D:\\Hanze\\Stage\\CompoundEvolver\\compound-evolver\\test\\reactants.smiles").getMoleculeIterator().forEachRemaining(reactants::add);
//            Molecule[] reactantarray = new Molecule[reactants.size()];
//            reactantarray = reactants.toArray(reactantarray);
//            reactor.setReactants(reactantarray);
//
//
//            Candidate candidate = new Candidate(new ArrayList<>());
//            candidate.phenotype = reactor.react()[0];
//            candidate.atommap = reactor.getReactionMap();
//            candidate.reactants = reactantarray;
//            candidate.setScoredConformersFile(Paths.get("D:\\Hanze\\Stage\\CompoundEvolver\\compound-evolver\\test\\smina_scorp.sdf"));
//            System.out.println(getReactantScores(candidate));
//            for (Molecule reactant : candidate.reactants) {
//                System.out.println(reactant.toFormat("smiles"));
//            }
//        } catch (IOException | ReactionException exception) {
//            exception.printStackTrace();
//        }
//
//    }
}
