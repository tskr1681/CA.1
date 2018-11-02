package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;

import java.util.*;

public class RandomCompoundReactor {

    private Reactor reactor;
    private int maxSamples;

    public RandomCompoundReactor(Reactor reactor, int maxSamples)
    {
        this.reactor = reactor;
        this.maxSamples = maxSamples;
    }

    List<Candidate> randReact(List<List<Molecule>> reactantLists)
            throws ReactionException {
        // Amount of products generated
        int nSampled = 0;
        Random random = new Random();
        List<Candidate> candidates = new ArrayList<>();
        // Try to generate products while the number of products generated is
        // lower than the maximum number of products wanted
        while (nSampled < maxSamples) {
            Iterator<List<Molecule>> iterator = reactantLists.iterator();
            List<Molecule> reactants = new ArrayList<>();
            List<Integer> indexGenome = new ArrayList<>();
            Molecule[] products;
            while (iterator.hasNext()) {
                // Get random reactants for a single reaction
                List<Molecule> map = iterator.next();
                int index = random.nextInt(map.size());
                indexGenome.add(index);
                Molecule reactant = map.get(index);
                reactants.add(reactant);
            }
            Molecule[] molecules = reactants.toArray(new Molecule[0]);
            // Set the reactants
            reactor.setReactants(molecules);
            // Add the product and count the product if it can be made
            if ((products = reactor.react()) != null) {
                candidates.add(new Candidate(indexGenome, products[0]));
                nSampled++;
            }
        }
        return candidates;

    }
}
