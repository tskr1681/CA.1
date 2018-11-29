package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class RandomCompoundReactor {

    private Reactor reactor;
    private int maxSamples;

    public RandomCompoundReactor(Reactor reactor, int maxSamples)
    {
        this.reactor = reactor;
        this.maxSamples = maxSamples;
    }

    public List<Candidate> randReact(List<List<Molecule>> reactantLists)
            throws ReactionException {
        // Amount of products generated
        int nSampled = 0;
        Random random = new Random();
        List<Candidate> candidates = new ArrayList<>();

        // Set startTime
        long startTime = System.currentTimeMillis();

        // Try to generate products while the number of products generated is
        // lower than the maximum number of products wanted
        while (nSampled < maxSamples) {
            // Get iterator from reactant lists
            Iterator<List<Molecule>> iterator = reactantLists.iterator();

            // Define a list of reactants, aka the genome of a candidate
            List<Molecule> reactants = new ArrayList<>();
            // Define a list of indices corresponding to the reactants in the genome
            List<Integer> indexGenome = new ArrayList<>();

            while (iterator.hasNext()) {
                // Get random reactants for a single reaction
                List<Molecule> map = iterator.next();
                // Get a random int within range 0 (inclusive) - n-reactants (exclusive)
                int index = random.nextInt(map.size());
                // Add the new reactant in both representations to the list
                indexGenome.add(index);
                Molecule reactant = map.get(index);
                reactants.add(reactant);
            }

            // Create an array of molecules, reactants, from the ArrayList instance
            Molecule[] molecules = reactants.toArray(new Molecule[0]);

            // Define an array that can contain reaction products
            Molecule[] products;

            // Set the reactants
            reactor.setReactants(molecules);
            // Add the product and count the product if it can be made
            if ((products = reactor.react()) != null) {
                candidates.add(new Candidate(indexGenome, products[0]));
                nSampled++;
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            if (duration > 10000 && nSampled <= 0) {
                throw new RuntimeException(String.format("Reactants did not react in %d ms. Are they in order?", duration));
            }
        }
        return candidates;

    }
}
