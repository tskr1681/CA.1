package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.Molecule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

class RandomCompoundReactor {

    private int maxSamples;

    RandomCompoundReactor(int maxSamples)
    {
        this.maxSamples = maxSamples;
    }

    List<Candidate> randReact(List<List<Molecule>> reactantLists, List<Species> species) {

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

            // Define a list of indices corresponding to the reactants in the genome
            List<Integer> indexGenome = new ArrayList<>();

            while (iterator.hasNext()) {
                // Get random reactants for a single reaction
                List<Molecule> map = iterator.next();
                // Get a random int within range 0 (inclusive) - n-reactants (exclusive)
                int index = random.nextInt(map.size());
                // Add the new reactant in both representations to the list
                indexGenome.add(index);
            }

            // Set the reactants
            Candidate candidate = new Candidate(indexGenome);
            // Add the product and count the product if it can be made
            boolean finish = candidate.finish(reactantLists, species);
            if (finish) {
                candidates.add(candidate);
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
