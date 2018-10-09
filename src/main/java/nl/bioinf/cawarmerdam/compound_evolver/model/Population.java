package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;

import java.util.*;

public class Population implements Iterable<Candidate> {

    private List<Candidate> candidateList;
    private int populationSize;

    public Population(List<Candidate> candidateList) {
        this.candidateList = candidateList;
    }

    public void crossover() {
        Reactor reactor = new Reactor();
        // Create list of offspring
        List<Candidate> offspring = new ArrayList<>();
        // Shuffle parents
        Collections.shuffle(this.candidateList);
        int offspringSize = 10;
        // Loop to fill offspring list to offspring size
        for (int i = 0; offspring.size() < offspringSize; i++) {
            // Get the index of two parents to perform crossover between the two
            int firstParentIndex = i % this.candidateList.size();
            int otherParentIndex = (i + 1) % this.candidateList.size();
            // Get the two parents
            Candidate firstParent = this.candidateList.get(firstParentIndex);
            Candidate otherParent = this.candidateList.get(otherParentIndex);
            // Perform crossover between the two
            Molecule[] childGenome = firstParent.crossover(otherParent);
            try {
                reactor.setReactants(childGenome);
                Molecule[] products;
                if ((products = reactor.react()) != null) {
                    offspring.add(new Candidate(childGenome, products[0]));
                }
            } catch (ReactionException e) {
                e.printStackTrace();
            }
        }
        this.candidateList = offspring;
    }

    public void mutate() {

    }

    @Override
    public Iterator<Candidate> iterator() {
        return this.candidateList.iterator();
    }
}
