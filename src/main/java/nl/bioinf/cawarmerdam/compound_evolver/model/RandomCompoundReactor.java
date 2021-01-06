/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class that creates a random set of candidates from the given reactions and reactants
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
class RandomCompoundReactor {
    private final int maxSamples;

    /**
     * Constructor of the random compound reactor.
     *
     * @param maxSamples how many candidates have to be sampled.
     */
    RandomCompoundReactor(int maxSamples) {
        this.maxSamples = maxSamples;
    }

    /**
     * Method that generates random candidates.
     *
     * @param reactantLists a list of lists of reactants.
     * @param species       a list with species that contain reactions and how reactants map to the reaction.
     * @return the list of generated candidates.
     */
    List<Candidate> randReact(List<List<String>> reactantLists, List<Species> species, AtomicLong currentValue) {

        // Amount of products generated
        int nSampled = 0;
        Random random = new Random();
        random.setSeed(currentValue.get());
        System.out.println("Randreact seed: " + currentValue.get());
        List<Candidate> candidates = new ArrayList<>();

        // Set startTime
        long startTime = System.currentTimeMillis();

        // Try to generate products while the number of products generated is
        // lower than the maximum number of products wanted
        while (nSampled < maxSamples) {
            // Get a list of randomly selected reactants that can be a genome
            List<Integer> indexGenome = selectRandomIndexGenome(random, reactantLists);

            // Set the reactants
            Candidate candidate = new Candidate(indexGenome, currentValue.incrementAndGet());
            // Add the product and count the product if it can be made
            boolean finish = candidate.finish(reactantLists, species);
            if (finish) {
                candidates.add(candidate);
                nSampled++;
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            // Throw an exception if there still is nothing sampled after 10000 milliseconds.
            if (duration > 10000 && nSampled <= 0) {
                throw new RuntimeException(String.format("Reactants did not react in %d ms. Are they in order?", duration));
            }
        }
        return candidates;

    }

    /**
     * Method that randomly picks a reactant from every list of reactants.
     *
     * @param random        An instance of the random class.
     * @param reactantLists The list of lists of reactants.
     * @return a randomly combined genome.
     */
    private List<Integer> selectRandomIndexGenome(Random random, List<List<String>> reactantLists) {
        // Get iterator from reactant lists
        Iterator<List<String>> iterator = reactantLists.iterator();
        // Define a list of indices corresponding to the reactants in the genome
        List<Integer> indexGenome = new ArrayList<>();

        while (iterator.hasNext()) {
            // Get random reactants for a single reaction
            List<String> map = iterator.next();
            // Get a random int within range 0 (inclusive) - n-reactants (exclusive)
            int index = random.nextInt(map.size());
            // Add the new reactant in both representations to the list
            indexGenome.add(index);
        }
        return indexGenome;
    }
}
