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
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.DoubleStream.of;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Population implements Iterable<Candidate> {

    private final Reactor reaction;
    private final List<List<Molecule>> reactantLists;
    private final Random random;
    private SelectionMethod selectionMethod;
    private MutationMethod mutationMethod;
    private List<Candidate> candidateList;
    private double[][][] alleleSimilarities;
    private double mutationRate;
    private double selectionFraction;
    private double crossoverRate;
    private double randomImmigrantRate;
    private double elitistRate;
    private int populationSize;
    private int generation;

    private enum ReproductionMethod {CROSSOVER, ELITIST, RANDOM_IMMIGRANT, CLEAR}

    public enum SelectionMethod {CLEAR, FITNESS_PROPORTIONATE_SELECTION, TRUNCATED_SELECTION}

    public enum MutationMethod {DISTANCE_DEPENDENT, DISTANCE_INDEPENDENT}

    public Population(List<List<Molecule>> reactantLists, Reactor reaction, int initialGenerationSize) {
        this.random = new Random();
        this.reaction = reaction;
        this.reactantLists = reactantLists;
        this.populationSize = initialGenerationSize;
        this.generation = 0;
        this.mutationRate = 0.1;
        this.selectionFraction = 0.6;
        this.crossoverRate = 0;
        this.elitistRate = 0.5;
        this.randomImmigrantRate = 0.0;
        this.selectionMethod = SelectionMethod.FITNESS_PROPORTIONATE_SELECTION;
        this.mutationMethod = MutationMethod.DISTANCE_INDEPENDENT;
        this.candidateList = new RandomCompoundReactor(this.reaction, initialGenerationSize).execute(this.reactantLists);
    }

    public double getRandomImmigrantRate() {
        return randomImmigrantRate;
    }

    public void setRandomImmigrantRate(double randomImmigrantRate) {
        this.randomImmigrantRate = randomImmigrantRate;
    }

    public double getElitistRate() {
        return elitistRate;
    }

    public void setElitistRate(double elitistRate) {
        this.elitistRate = elitistRate;
    }

    /**
     * Setter for selection method.
     *
     * @param selectionMethod for use in selecting new offspring.
     */
    public void setSelectionMethod(SelectionMethod selectionMethod) {
        this.selectionMethod = selectionMethod;
    }

    /**
     * Getter for the selection fraction.
     *
     * @return fraction off population that will be selected.
     */
    public double getSelectionFraction() {
        return selectionFraction;
    }

    /**
     * Setter for the selection fraction.
     *
     * @param selectionFraction, the fraction off the population that will be selected.
     */
    public void setSelectionFraction(double selectionFraction) {
        this.selectionFraction = selectionFraction;
    }

    /**
     * Setter for the mutation method.
     *
     * @param mutationMethod for use in introducing mutations.
     */
    public void setMutationMethod(MutationMethod mutationMethod) {
        this.mutationMethod = mutationMethod;
    }

    /**
     * Getter for the selection method.
     *
     * @return the method that is set to select new offspring.
     */
    public SelectionMethod getSelectionMethod() {
        return selectionMethod;
    }

    /**
     * Getter for the mutation method.
     *
     * @return the method that is set to introduce mutations.
     */
    public MutationMethod getMutationMethod() {
        return mutationMethod;
    }

    /**
     * Getter for the population size.
     *
     * @return the population size.
     */
    public int getPopulationSize() {
        return populationSize;
    }

    /**
     * Setter for the population size.
     *
     * @param populationSize, the size to set the amount of individuals to in newer generations.
     */
    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    /**
     * Getter for the mutation rate.
     *
     * @return the mutation rate.
     */
    public double getMutationRate() {
        return mutationRate;
    }

    /**
     * Setter for the mutation rate.
     *
     * @param mutationRate, the rate at which to introduce new mutations in a gene.
     */
    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
    }

    /**
     * Getter for the crossover rate. This is the probability that crossover will be performed in two parents or
     * if new offspring will be generated by other means.
     *
     * @return the crossover rate.
     */
    public double getCrossoverRate() {
        return crossoverRate;
    }

    /**
     * Setter for te crossover rate. This is the probability that crossover will be performed in two parents or
     * if new offspring will be generated by other means.
     *
     * @param crossoverRate, the probability that crossover will be performed
     */
    public void setCrossoverRate(double crossoverRate) {
        this.crossoverRate = crossoverRate;
    }

    /**
     * Computes allele similarities by using the Tanimoto dissimilarity functionality provided by the Chemaxon API
     * <a href="https://docs.chemaxon.com/display/docs/Similarity+search">Similarity search</a>.
     * The similarity of a compound to itself is set so its fraction of the total similarities for the compound to.
     * other compounds and itself is equal to the mutation rate. Like so:
     * <p>
     * <p>
     * Mutation rate | 0,1 |      |
     * --------------|-----|------|------
     * |     |      |
     * Similarities  |   1 |  0,2 |  0,3
     * Compensated   | 4,5 |  0,2 |  0,3
     * Fraction      | 0,9 | 0,04 | 0,06
     */
    public void computeAlleleSimilarities() {
        this.alleleSimilarities = new double[reactantLists.size()][][];
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
     * Get the tanimoto dissimilarity score between the first molecule and the second molecule.
     *
     * @param firstMolecule  the first molecule to compare.
     * @param secondMolecule the second molecule to compare.
     * @return the tanimoto dissimilarity score as a float.
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
     * population size.
     */
    public void produceOffspring() {
        produceOffspring(this.populationSize);
    }

    /**
     * A method responsible for producing offspring.
     *
     * @param offspringSize the amount of candidates the offspring will consist off.
     */
    private void produceOffspring(int offspringSize) {
        // Create list of offspring
        List<Candidate> offspring = new ArrayList<>();
        // Perform crossing over
        // Shuffle parents
        Collections.shuffle(this.candidateList);
        // Select parents
        selectParents();
        ReproductionMethod offspringChoice = ReproductionMethod.CLEAR;
        // Loop to fill offspring list to offspring size
        for (int i = 0; offspring.size() < offspringSize; i++) {
//            System.out.println("i = " + i);
            // Get some genomes by crossing over according to crossover probability
            if (offspringChoice == ReproductionMethod.CLEAR) {
                offspringChoice = ReproductionMethod.values()[makeWeightedChoice(new double[]{
                        this.crossoverRate,
                        this.elitistRate,
                        this.randomImmigrantRate})];
            }
//            System.out.println("offspringChoice = " + offspringChoice);
            Candidate newOffspring = ProduceOffspringIndividual(offspringChoice, i);
            if (newOffspring != null) {
                offspring.add(newOffspring);
                offspringChoice = ReproductionMethod.CLEAR;
            }
        }
        candidateList = offspring;
        generation++;
    }

    private Candidate ProduceOffspringIndividual(ReproductionMethod offspringChoice, int i) {
        if (offspringChoice == ReproductionMethod.CROSSOVER) {
            // Get the recombined genome by crossing over
            List<Integer> newGenome = getRecombinedGenome(i);
            // Mutate the recombined genome
            mutate(newGenome);
            return finalizeOffspring(newGenome);
        } else if (offspringChoice == ReproductionMethod.ELITIST) {
            // Get the recombined genome by crossing over
            List<Integer> newGenome = this.candidateList.get(i % this.candidateList.size()).getGenotype();
            // Mutate the recombined genome
//            System.out.println(this.candidateList.get(i % this.candidateList.size()));
            mutate(newGenome);
            return finalizeOffspring(newGenome);
        } else if (offspringChoice == ReproductionMethod.RANDOM_IMMIGRANT) {
            // Introduce a random immigrant
            return introduceRandomImmigrant();
        }
        return null;
    }

    private Candidate finalizeOffspring(List<Integer> newGenome) {
        try {
            // get Reactants from the indices
            Molecule[] reactants = getReactantsFromIndices(newGenome);
            reaction.setReactants(reactants);
            Molecule[] products;
            // Try to produce a product
            if ((products = reaction.react()) != null) {
                Candidate newCandidate = new Candidate(newGenome, products[0]);
                if (newCandidate.isValid()) {
                    return newCandidate;
                }
            }
        } catch (ReactionException e) {
            // Should log this
            e.printStackTrace();
        }
        return null;
    }

    /**
     * A method that creates a random new immigrant.
     *
     * @return a new individual (random immigrant).
     */
    private Candidate introduceRandomImmigrant() {
        return new RandomCompoundReactor(this.reaction, 1).execute(this.reactantLists).get(0);
    }

    /**
     * Select the parents according to the method that is set.
     * If the method flag was set to cleared, do nothing.
     */
    private void selectParents() {
        // Assert that the candidates have a score
        assert (candidateList.size() > 0) && candidateList.get(0).getScore() != null;
        // Select the parents according to the method that is set
        List<Double> scores = candidateList.stream()
                .map(Candidate::getScore).collect(Collectors.toList());
        System.out.println("Parents         = " + scores);
        if (this.selectionMethod == SelectionMethod.FITNESS_PROPORTIONATE_SELECTION) {
            candidateList = this.fitnessProportionateSelection();
        } else if (this.selectionMethod == SelectionMethod.TRUNCATED_SELECTION) {
            candidateList = this.truncatedSelection();
        }
        scores = candidateList.stream()
                .map(Candidate::getScore).collect(Collectors.toList());
        System.out.println("SelectedParents = " + scores);
        // Do nothing if the flag is cleared
    }

    /**
     * A method selecting reactants from the reactant lists with the given list of indices.
     *
     * @param recombinedGenome, a list of indices as long as the reactant lists list.
     * @return an array of reactants from the reactant lists.
     */
    private Molecule[] getReactantsFromIndices(List<Integer> recombinedGenome) {
        return IntStream.range(0, reactantLists.size())
                .mapToObj(i -> reactantLists.get(i).get(recombinedGenome.get(i)))
                .toArray(Molecule[]::new);
    }

    /**
     * A selection method that selects individuals probabilistically by using the fitness score.
     * The amount of individuals selected is the amount of candidates multiplied by the selection fraction and rounded
     * up to the nearest integer.
     *
     * @return the selected individuals
     */
    private List<Candidate> fitnessProportionateSelection() {
        List<Candidate> selectedParents = new ArrayList<>();
        // Select the amount of parents corresponding to the total parents multiplied by the selection rate
        for (int i = 0; i < candidateList.size() * this.selectionFraction; i++) {
            selectedParents.add(candidateList.get(rouletteSelect()));
        }
        return selectedParents;
    }

    /**
     * A selection method that selects the individuals with the best score.
     * The amount of individuals selected is the amount of candidates multiplied by the selection fraction and rounded
     * up to the nearest integer.
     *
     * @return the selected individuals
     */
    private List<Candidate> truncatedSelection() {
        Collections.sort(this.candidateList);
        return this.candidateList.subList(0, (int) Math.ceil(this.candidateList.size() * this.selectionFraction));
    }

    /**
     * Method that selects an individual probabilistically based on the fitness score.
     *
     * @return the index of the individual selected.
     */
    private int rouletteSelect() {
        // Create a stream to get the score of every candidate and convert this to a double
        return makeWeightedChoice(candidateList.stream()
                .map(Candidate::getScore)
                .mapToDouble(v -> v)
                .toArray());
    }

    /**
     * Chooses an item in a array of weights.
     *
     * @param weights the weights to choose from.
     * @return the index of the item in the array that was chosen.
     */
    private int makeWeightedChoice(double[] weights) {
        double weightsSum = 0;
        // Get sum of fitness scores
        weightsSum = DoubleStream.of(weights).sum();
        // get a random value
        double value = this.random.nextDouble() * weightsSum;
        // locate the random value based on the weights
        for (int i = 0; i < weights.length; i++) {
            value -= weights[i];
            if (value < 0) return i;
        }
        // when rounding errors occur, we return the last item's index
        return weights.length - 1;
    }

    /**
     * Recombines genomes of two individuals.
     *
     * @param i the index to get individuals from.
     * @return the recombined genome as a list
     */
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

    @Override
    public String toString() {
        List<Double> scores = candidateList.stream()
                .map(Candidate::getScore)
                .collect(Collectors.toList());
        OptionalDouble average = scores.stream().mapToDouble(v -> v).average();
        return String.format(
                "Generation %d, individual count = %d %n" +
                        " agv | min | max %n %3.0f | %3.0f | %3.0f ",
                generation, candidateList.size(),
                average.isPresent() ? average.getAsDouble() : Double.NaN,
                Collections.min(scores), Collections.max(scores));
    }

    /**
     * Introduce mutations in the genome according to the mutation rate and the set mutation method.
     *
     * @param genome to introduce mutations in.
     */
    private void mutate(List<Integer> genome) {
        // Loop through each allele
        String format = "%10s -> %10s";
//        System.out.println(String.format(format, "curr", "new"));
        for (int i = 0; i < genome.size(); i++) {
            int allele = genome.get(i);
            int reactantIndex = getMutationSubstitute(i, allele);
            genome.set(i, reactantIndex);
            if (alleleSimilarities != null) {
                System.out.println(String.format("%3s (%1.2f) -> %3s (%1.2f)", allele, alleleSimilarities[i][allele][allele] / DoubleStream.of(alleleSimilarities[i][allele]).sum(), reactantIndex, alleleSimilarities[i][allele][reactantIndex] / DoubleStream.of(alleleSimilarities[i][allele]).sum()));
            } else {
                System.out.println(String.format("%3s        -> %3s       ", allele, reactantIndex));
            }
        }
    }

    /**
     * Method that introduces mutations with either a distance dependent method
     * or a distance independent method.
     *
     * @param i,     the index of the reactant list
     * @param allele the index of the allele in the reactant list
     * @return the index of the chosen reactant from the reactant list
     */
    private int getMutationSubstitute(int i, int allele) {
        if (this.mutationMethod == MutationMethod.DISTANCE_DEPENDENT) {
            // Check if similarity matrix was calculated.
            System.out.println("this.alleleSimilarities = " + Arrays.deepToString(this.alleleSimilarities));
            if (this.alleleSimilarities == null) {
                throw new RuntimeException("Allele similarity matrices should be computed " +
                    "for distance dependant mutation!");
            }
            return makeWeightedChoice(alleleSimilarities[i][allele]);
        } else if (this.mutationMethod == MutationMethod.DISTANCE_INDEPENDENT) {
            return makeChoice(reactantLists.get(i).size(), allele);
        } else {
            throw new RuntimeException("Mutation method not set!");
        }
    }

    /**
     * Method that chooses either the allele index that is given
     * or another index that can range from 0 to size - 1, excluding the given allele index.
     *
     * @param size,  the size of the array or list to choose from.
     * @param allele the current allele index.
     * @return the index of choice.
     */
    private int makeChoice(int size, int allele) {
        // Only choose for mutation when the random double is below the mutation rate so when the mutation rate
        // is 0 the condition is always true and when the mutation rate is 1 the condition is always false.
        if (this.random.nextDouble() < this.mutationRate) {

            int index = this.random.nextInt(size + 1);
            if (index == size) index = allele;
            return index;
        } else
            return allele;
    }

    public Stream<Candidate> stream() {
        return candidateList.stream();
    }

    public int size() {
        return candidateList.size();
    }

    @Override
    public Iterator<Candidate> iterator() {
        return this.candidateList.iterator();
    }
}
