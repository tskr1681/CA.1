/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.descriptors.*;
import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;

import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static java.util.stream.DoubleStream.of;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Population implements Iterable<Candidate> {

    private final Reactor reaction;
    private final List<List<Molecule>> reactantLists;
    private double[][][] alleleSimilarities;
    private List<Candidate> candidateList;
    private int populationSize;
    private double mutationRate;

    private double selectionFraction;
    private SelectionMethod selectionMethod;
    private MutationMethod mutationMethod;

    public enum SelectionMethod {CLEAR, FITNESS_PROPORTIONATE_SELECTION, TRUNCATED_SELECTION}

    public enum MutationMethod {DISTANCE_DEPENDENT, DISTANCE_INDEPENDENT}

    public Population(List<List<Molecule>> reactantLists, Reactor reaction, int initialGenerationSize) {
        this.reaction = reaction;
        this.reactantLists = reactantLists;
        this.populationSize = initialGenerationSize;
        this.mutationRate = 0.1;
        this.selectionFraction = 0.5;
        this.selectionMethod = SelectionMethod.FITNESS_PROPORTIONATE_SELECTION;
        this.mutationMethod = MutationMethod.DISTANCE_DEPENDENT;
        this.candidateList = new RandomCompoundReactor(this.reaction, initialGenerationSize).execute(this.reactantLists);
        this.alleleSimilarities = new double[reactantLists.size()][][];
        computeAlleleSimilarities();
    }

    public void setSelectionMethod(SelectionMethod selectionMethod) {
        this.selectionMethod = selectionMethod;
    }

    public double getSelectionFraction() {
        return selectionFraction;
    }

    public void setSelectionFraction(double selectionFraction) {
        this.selectionFraction = selectionFraction;
    }

    public void setMutationMethod(MutationMethod mutationMethod) {
        this.mutationMethod = mutationMethod;
    }

    public SelectionMethod getSelectionMethod() {
        return selectionMethod;
    }

    public MutationMethod getMutationMethod() {
        return mutationMethod;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    public double getMutationRate() {
        return mutationRate;
    }

    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
    }

    /**
     * Computes allele similarities by using the Tanimoto dissimilarity functionality provided by the Chemaxon API
     * <a href="https://docs.chemaxon.com/display/docs/Similarity+search">Similarity search</a>
     * The similarity of a compound to itself is set so its fraction of the total similarities for the compound to
     * other compounds and itself is equal to the mutation rate. Like so:
     *
     *
     * Mutation rate | 0,1 |      |
     * --------------|-----|------|------
     *               |     |      |
     * Similarities  |   1 |  0,2 |  0,3
     * Compensated   | 4,5 |  0,2 |  0,3
     * Fraction      | 0,9 | 0,04 | 0,06
     *
     */
    private void computeAlleleSimilarities() {
        // Loop through every reactant list (Acids, Amines, etc...)
        for (int i1 = 0; i1 < reactantLists.size(); i1++) {
            List<Molecule> reactants = reactantLists.get(i1);
            // Create a 2d matrix for the current reactant list
            alleleSimilarities[i1] = new double[reactants.size()][reactants.size()];
            // Loop through every reactant in the reactant list
            for (int i = 0; i < reactants.size(); i++) {
                // Loop nested through reactants in the reactant list until the j is equal to i
                // This means that the diagonal in the matrix was encountered.
                // We stop here because the similarity values at the diagonal (location i,i) should always be 1
                for (int j = 0; j < i; j++) {
                    // Assign and set the similarity score by deducting the tanimoto dissimilarity from 1
                    double tanimoto = (double) 1 - getTanimoto(reactants.get(i), reactants.get(j));
                    alleleSimilarities[i1][i][j] = tanimoto;
                    alleleSimilarities[i1][j][i] = tanimoto;
                }
            }
            // Loop through every reactant in the reactant list to set values at the diagonal
            for (int i = 0; i < alleleSimilarities[i1].length; i++) {
                // Get the sum of all similarity values for the current reactant
                // excluding the i,i location (because this is set to zero (0))
                double weightsSum = DoubleStream.of(alleleSimilarities[i1][i]).sum();
                // Take the mutation rate into account with the following calculation
                alleleSimilarities[i1][i][i] = weightsSum / mutationRate - weightsSum;
            }
        }
    }

    /**
     * Get the tanimoto dissimilarity score between the first molecule and the second molecule
     * @param firstMolecule the first molecule to compare
     * @param secondMolecule the second molecule to compare
     * @return the tanimoto dissimilarity score as a float
     */
    private float getTanimoto(Molecule firstMolecule, Molecule secondMolecule) {
        // Get chemical fingerprints
        ChemicalFingerprint firstFingerprint = new ChemicalFingerprint(new CFParameters());
        ChemicalFingerprint secondFingerprint = new ChemicalFingerprint(new CFParameters());
        // Try to get the tanimoto dissimilarity score
        try {
            firstFingerprint.generate(firstMolecule);
            secondFingerprint.generate(secondMolecule);
            return firstFingerprint.getTanimoto(secondFingerprint);
        } catch (MDGeneratorException e) {
            // Return a dissimilarity of 1 if the fingerprints could not be generated
            return 1;
        }
    }

    /**
     * Overloaded produceOffspring method with default parameter value population size set to the instance field
     * population size
     */
    public void produceOffspring() {
        produceOffspring(this.populationSize);
    }

    private void produceOffspring(int offspringSize) {
        // Create list of offspring
        List<Candidate> offspring = new ArrayList<>();
        // Perform crossing over
        // Shuffle parents
        Collections.shuffle(this.candidateList);
        // Select parents
        selectParents();
        // Loop to fill offspring list to offspring size
        for (int i = 0; offspring.size() < offspringSize; i++) {
            System.out.println("i = " + i);
            // Get the recombined genome by crossing over
            List<Integer> recombinedGenome = getRecombinedGenome(i);
            // Mutate the recombined genome
            mutate(recombinedGenome);
            try {
                // get Reactants from the indices
                Molecule[] reactants = getReactantsFromIndices(recombinedGenome);
                reaction.setReactants(reactants);
                Molecule[] products;
                // Try to produce a product
                if ((products = reaction.react()) != null) {
                    offspring.add(new Candidate(recombinedGenome, products[0]));
                }
            } catch (ReactionException e) {
                e.printStackTrace();
            }
        }
        // Possibly introduce new individuals
    }

    /**
     * Select the parents according to the method that is set
     * If the method flag was set to cleared, do nothing.
     */
    private void selectParents() {
        // Select the parents according to the method that is set
        if (this.selectionMethod == SelectionMethod.FITNESS_PROPORTIONATE_SELECTION) {
            candidateList = this.fitnessProportionateSelection();
        } else if (this.selectionMethod == SelectionMethod.TRUNCATED_SELECTION) {
            throw new RuntimeException("Truncated selection not implemented!");
        }
        // Do nothing if the flag is cleared
    }

    private Molecule[] getReactantsFromIndices(List<Integer> recombinedGenome) {
        return IntStream.range(0, reactantLists.size())
                .mapToObj(i -> reactantLists.get(i).get(recombinedGenome.get(i)))
                .toArray(Molecule[]::new);
    }

    private List<Candidate> fitnessProportionateSelection() {
        // Assert that the candidates have a score
        assert (candidateList.size() > 0) && candidateList.get(0).getScore() != null;
        List<Candidate> selectedParents = new ArrayList<>();
        // Select the amount of parents corresponding to the total parents multiplied by the selection rate
        for (int i = 0; i < candidateList.size() * this.selectionFraction; i++) {
            selectedParents.add(candidateList.get(rouletteSelect()));
        }
        return selectedParents;
    }

    private int rouletteSelect() {
        // Create a stream to get the score of every candidate and convert this to a double
        return makeWeightedChoice(candidateList.stream()
                .map(Candidate::getScore)
                .mapToDouble(v -> v)
                .toArray());
    }

    private int makeWeightedChoice(double[] weights) {
        double weightsSum = 0;
        // Get sum of fitness scores
        weightsSum = DoubleStream.of(weights).sum();
        // get a random value
        double value = new Random().nextDouble() * weightsSum;
        // locate the random value based on the weights
        for (int i = 0; i < weights.length; i++) {
            value -= weights[i];
            if (value < 0) return i;
        }
        // when rounding errors occur, we return the last item's index
        return weights.length - 1;
    }

    private List<Integer> getRecombinedGenome(int i) {
        // Get the index of two parents to perform crossover between the two
        int firstParentIndex = i % this.candidateList.size();
        int otherParentIndex = (i + 1) % this.candidateList.size();
        // Get the two parents
        Candidate firstParent = this.candidateList.get(firstParentIndex);
        Candidate otherParent = this.candidateList.get(otherParentIndex);
        // Perform crossover between the two
        return firstParent.crossover(otherParent);
    }

    private void mutate(List<Integer> genome) {
        // Loop through each allele
        String format = "%10s -> %10s";
        System.out.println(String.format(format, "curr", "new"));
        for (int i = 0; i < genome.size(); i++) {
            int allele = genome.get(i);
            int reactantIndex = makeWeightedChoice(alleleSimilarities[i][allele]);
            genome.set(i, reactantIndex);
            System.out.println(String.format("%3s (%1.2f) -> %3s (%1.2f)", allele, alleleSimilarities[i][allele][allele] / DoubleStream.of(alleleSimilarities[i][allele]).sum(), reactantIndex, alleleSimilarities[i][allele][reactantIndex] / DoubleStream.of(alleleSimilarities[i][allele]).sum()));
        }
    }

    @Override
    public Iterator<Candidate> iterator() {
        return this.candidateList.iterator();
    }
}
