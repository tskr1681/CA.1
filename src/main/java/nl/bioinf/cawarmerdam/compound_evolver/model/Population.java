/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.descriptors.*;
import chemaxon.jep.function.In;
import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jaxen.util.SingletonList;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Population implements Iterable<Candidate> {

    private final List<String> offspringRejectionMessages = new ArrayList<>();
    private final List<List<Molecule>> reactantLists;
    private final Random random;
    private List<Species> species;
    private double maxAnchorMinimizedRmsd;
    private SelectionMethod selectionMethod;
    private MutationMethod mutationMethod;
    private InterspeciesCrossoverMethod interspeciesCrossoverMethod;
    private SpeciesDeterminationMethod speciesDeterminationMethod;
    private List<Candidate> candidateList;
    private double[][][] alleleSimilarities;
    private double mutationRate;
    private double selectionFraction;
    private double crossoverRate;
    private double randomImmigrantRate;
    private double elitismRate;
    private int populationSize;
    private int generationNumber;
    private int tournamentSize;
    private Double maxHydrogenBondAcceptors = null;
    private Double maxHydrogenBondDonors = null;
    private Double maxMolecularMass = null;
    private Double maxPartitionCoefficient = null;
    private Integer tooDistantConformerCounter = 0;

    /**
     * Constructor for population.
     *
     * @param reactantLists Lists of reactants in a list.
     * @param species List of possible species.
     * @param speciesDeterminationMethod The method that determines which species to use.
     * @param initialGenerationSize The generation or population size.
     * @throws MisMatchedReactantCount if the amount of reactants given is unequal to the amount of reactants
     * in the reaction.
     * @throws ReactionException if a reaction could not be performed.
     */
    public Population(
            List<List<Molecule>> reactantLists,
            List<Species> species,
            SpeciesDeterminationMethod speciesDeterminationMethod,
            int initialGenerationSize) throws MisMatchedReactantCount, ReactionException {

        this.random = new Random();
        this.reactantLists = reactantLists;
        this.populationSize = initialGenerationSize;
        this.generationNumber = 0;
        this.mutationRate = 0.1;
        this.selectionFraction = 0.4;
        this.crossoverRate = 0.8;
        this.elitismRate = 0.1;
        this.randomImmigrantRate = 0.1;
        this.maxAnchorMinimizedRmsd = 1;
        this.tournamentSize = 2;
        this.speciesDeterminationMethod = speciesDeterminationMethod;
        this.interspeciesCrossoverMethod = InterspeciesCrossoverMethod.COMPLETE;
        this.selectionMethod = SelectionMethod.FITNESS_PROPORTIONATE_SELECTION;
        this.mutationMethod = MutationMethod.DISTANCE_INDEPENDENT;
        this.species = species;
        initializePopulation();
    }

    /**
     * Constructor for population with dynamic species determination as default.
     *
     * @param reactantLists Lists of reactants in a list.
     * @param species List of possible species.
     * @param initialGenerationSize The generation or population size.
     * @throws MisMatchedReactantCount if the amount of reactants given is unequal to the amount of reactants
     * in the reaction.
     * @throws ReactionException if a reaction could not be performed.
     */
    public Population(
            List<List<Molecule>> reactantLists,
            List<Species> species,
            int initialGenerationSize) throws MisMatchedReactantCount, ReactionException {
        this(reactantLists, species, SpeciesDeterminationMethod.DYNAMIC, initialGenerationSize);
    }


    /**
     * Getter for the interspecies crossover method.
     *
     * @return the interspecies crossover method.
     */
    public InterspeciesCrossoverMethod getInterspeciesCrossoverMethod() {
        return interspeciesCrossoverMethod;
    }

    /**
     * Setter for the interspecies crossover method.
     *
     * @param interspeciesCrossoverMethod The interspecies crossover method.
     */
    public void setInterspeciesCrossoverMethod(InterspeciesCrossoverMethod interspeciesCrossoverMethod) {
        this.interspeciesCrossoverMethod = interspeciesCrossoverMethod;
    }

    /**
     * Getter for the species determination method.
     *
     * @return the species determination method.
     */
    public SpeciesDeterminationMethod getSpeciesDeterminationMethod() {
        return speciesDeterminationMethod;
    }

    /**
     * Setter for the species determination method.
     *
     * @param speciesDeterminationMethod The species determination method.
     */
    public void setSpeciesDeterminationMethod(SpeciesDeterminationMethod speciesDeterminationMethod) {
        this.speciesDeterminationMethod = speciesDeterminationMethod;
    }

    /**
     * Getter for the maximum allowed rmsd between the anchor and
     * its matching substructure in the candidates best conformer.
     *
     * @return the maximum allowed rmsd
     */
    public double getMaxAnchorMinimizedRmsd() {
        return maxAnchorMinimizedRmsd;
    }

    /**
     * Setter for the maximum allowed rmsd between the anchor and
     * its matching substructure in the candidates best conformer.
     *
     * @param maxAnchorMinimizedRmsd The maximum allowed rmsd
     */
    public void setMaxAnchorMinimizedRmsd(double maxAnchorMinimizedRmsd) {
        this.maxAnchorMinimizedRmsd = maxAnchorMinimizedRmsd;
    }

    /**
     * Setter for the maximum hydrogen bond acceptors that a new candidate must comply with.
     *
     * @param maxHydrogenBondAcceptors, the maximum hydrogen bond acceptors that new candidates must comply with.
     */
    public void setMaxHydrogenBondAcceptors(double maxHydrogenBondAcceptors) {
        this.maxHydrogenBondAcceptors = maxHydrogenBondAcceptors;
    }

    /**
     * Setter for the maximum hydrogen bond donors that a new candidate must comply with.
     *
     * @param maxHydrogenBondDonors, the maximum hydrogen bond donors that new candidates must comply with.
     */
    public void setMaxHydrogenBondDonors(double maxHydrogenBondDonors) {
        this.maxHydrogenBondDonors = maxHydrogenBondDonors;
    }

    /**
     * Setter for the maximum molecular mass that a new candidate must comply with.
     *
     * @param maxMolecularMass, the maximum molecular mass that new candidates must comply with.
     */
    public void setMaxMolecularMass(double maxMolecularMass) {
        this.maxMolecularMass = maxMolecularMass;
    }

    /**
     * Setter for the maximum partition coefficient that a new candidate must comply with.
     *
     * @param maxPartitionCoefficient, the maximum partition coefficient that new candidates must comply with.
     */
    public void setMaxPartitionCoefficient(double maxPartitionCoefficient) {
        this.maxPartitionCoefficient = maxPartitionCoefficient;
    }

    /**
     * Initializes a population from the reactant lists and the reactions.
     *
     * @throws MisMatchedReactantCount if the number of reactants does not match the number of reactants required
     *                                 in the reactions.
     */
    private void initializePopulation() throws MisMatchedReactantCount {
        int individualsPerSpecies = this.populationSize / this.species.size();
        this.candidateList = new ArrayList<>();

        if (this.speciesDeterminationMethod == SpeciesDeterminationMethod.FIXED) {
            for (Species species : this.species) {
                int reactantCount = species.getReaction().getReactantCount();

                // Get only the right reactants
                List<List<Molecule>> reactants = species.getReactantListsSubset(this.reactantLists);

                if (reactantCount != reactants.size()) {
                    // Throw exception
                    throw new MisMatchedReactantCount(reactantCount, reactants.size());
                }
                // Make sure only the right reactants are passed on
                this.candidateList.addAll(new RandomCompoundReactor(individualsPerSpecies)
                        .randReact(this.reactantLists, Collections.singletonList(species)));
            }
        } else if (this.speciesDeterminationMethod == SpeciesDeterminationMethod.DYNAMIC) {
            this.candidateList.addAll(new RandomCompoundReactor(this.populationSize)
                    .randReact(this.reactantLists, this.species));
        } else {
            // Throw exception when another determination method is selected.
            throw new RuntimeException("Species determination method '" + speciesDeterminationMethod.toString() +
                    "' is not yet implemented!");
        }
    }

    /**
     * Getter for the current generation.
     *
     * @return the current generation
     */
    public Generation getCurrentGeneration() {
        return new Generation(candidateList, generationNumber);
    }

    /**
     * Getter for the current generationNumber number.
     *
     * @return the current generationNumber number.
     */
    public int getGenerationNumber() {
        return generationNumber;
    }

    /**
     * Getter for the random immigrant rate. The random immigrant rate should be seen in relation to the
     * elitism rate and the crossover rate. When creating offspring a candidate solution is either produced
     * as a random immigrant, as the crossover product of two parents or by directly copying a selected candidate.
     *
     * @return the random immigrant rate
     */
    public double getRandomImmigrantRate() {
        return randomImmigrantRate;
    }

    /**
     * Setter for the random immigrant rate. The random immigrant rate should be seen in relation to the
     * elitism rate and the crossover rate. When creating offspring a candidate solution is either produced
     * as a random immigrant, as the crossover product of two parents or by directly copying a selected candidate.
     *
     * @param randomImmigrantRate The weight of selecting a random immigrant as offspring.
     */
    public void setRandomImmigrantRate(double randomImmigrantRate) {
        this.randomImmigrantRate = randomImmigrantRate;
    }

    /**
     * Getter for the rate at which the elitism concept is chosen for offspring. The elitism rate should be seen
     * in relation to the random immigrant rate and the crossover rate. The rates act as weights for choosing
     * the method for getting a new individual.
     *
     * @return the elitism rate.
     */
    public double getElitismRate() {
        return elitismRate;
    }

    /**
     * Setter for the rate at which the elitism concept is chosen for offspring. The elitism rate should be seen
     * in relation to the random immigrant rate and the crossover rate. The rates act as weights for choosing
     * the method for getting a new individual.
     *
     * @param elitismRate The weight that the elitism concept has in choosing an offspring production method for
     *                    an individual.
     */
    public void setElitismRate(double elitismRate) {
        this.elitismRate = elitismRate;
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
     * @param selectionFraction The fraction off the population that will be selected.
     */
    public void setSelectionFraction(double selectionFraction) {
        this.selectionFraction = selectionFraction;
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
     * Setter for selection method.
     *
     * @param selectionMethod For use in selecting new offspring.
     */
    public void setSelectionMethod(SelectionMethod selectionMethod) {
        this.selectionMethod = selectionMethod;
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
     * Setter for the mutation method.
     *
     * @param mutationMethod For use in introducing mutations.
     */
    public void setMutationMethod(MutationMethod mutationMethod) {
        this.mutationMethod = mutationMethod;
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
     * @param populationSize The size to set the amount of individuals to in newer generations.
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
     * @param mutationRate The rate at which to introduce new mutations in a gene.
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
     * @param crossoverRate The probability that crossover will be performed
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

                // Set values at the diagonal
                // Get the sum of all similarity values for the current reactant
                // excluding the i,i location (because this is set to zero (0))
                double weightsSum = DoubleStream.of(alleleSimilarities[i1][i]).sum();
                // Take the mutation rate into account with the following calculation
                alleleSimilarities[i1][i][i] = weightsSum / mutationRate - weightsSum;
            }
        }
    }

    /**
     * Initializes the matrix for allele similarities within a pool of reactants, for each pool of reactants.
     */
    public void initializeAlleleSimilaritiesMatrix() {
        this.alleleSimilarities = new double[reactantLists.size()][][];
        // Loop through every reactant list (Acids, Amines, etc...)
        for (int i1 = 0; i1 < reactantLists.size(); i1++) {
            List<Molecule> reactants = reactantLists.get(i1);
            // Create a 2d matrix for the current reactant list
            alleleSimilarities[i1] = new double[reactants.size()][reactants.size()];
        }
    }

    /**
     * Computes allele similarities for a specific allele by using the Tanimoto dissimilarity functionality provided
     * by the Chemaxon API
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
    private void computeSpecificAlleleSimilarities(int reactantsListIndex, int alleleIndex) {
        // Get reactants which it is about
        List<Molecule> reactants = reactantLists.get(reactantsListIndex);
        // Loop through reactants in the reactant list for all reactants
        // Calculate tanimoto only if the diagonal in the matrix is not encountered and the cell is not yet filled
        // We don't calculate the tanimoto for the diagonal because the similarity values at the diagonal (location i,i)
        // should always be 1
        // When computing the alleles in random fashion it is uncertain which values are already filled
        for (int j = 0; j < reactants.size(); j++) {
            if (j != alleleIndex && alleleSimilarities[reactantsListIndex][alleleIndex][j] == 0) {
                // Assign and set the similarity score by deducting the tanimoto dissimilarity from 1
                double tanimoto = (double) 1 - getTanimoto(reactants.get(alleleIndex), reactants.get(j));
                alleleSimilarities[reactantsListIndex][alleleIndex][j] = tanimoto;
                alleleSimilarities[reactantsListIndex][j][alleleIndex] = tanimoto;
            }
        }
        // Get the sum of all similarity values for the current reactant
        // excluding the i,i location (because this is set to zero (0))
        double weightsSum = DoubleStream.of(alleleSimilarities[reactantsListIndex][alleleIndex]).sum();
        // Take the mutation rate into account with the following calculation
        alleleSimilarities[reactantsListIndex][alleleIndex][alleleIndex] = weightsSum / mutationRate - weightsSum;
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
        // Current fingerprints are simple, could also use extended connectivity fingerprints (ECFPs):
        // "Compared to path-based fingerprints, ECFPs typically provide more adequate results for similarity searching,
        // which approximate the expectations of a medicinal chemist better."
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
    public void produceOffspring() throws OffspringFailureOverflow, UnSelectablePopulationException {
        produceOffspring(this.populationSize);
    }

    /**
     * A method responsible for producing offspring.
     *
     * @param offspringSize the amount of candidates the offspring will consist off.
     */
    private void produceOffspring(int offspringSize) throws OffspringFailureOverflow, UnSelectablePopulationException {
        // Create list of offspring
        List<Candidate> offspring = new ArrayList<>();

        // Shuffle parents
        Collections.shuffle(this.candidateList);
        // Select parents
        selectParents();

        // Set the offspring choice to clear. For every new individual (offspring) to create a new offspring choice
        // is selected according to the set crossover, random immigrant, and elitist parameters.
        // (A random weighted choice is performed each time. )
        ReproductionMethod offspringChoice = ReproductionMethod.CLEAR;
        // Count the number of times one offspring could not be created. (Reset when an offspring could be created)
        int failureCounter = 0;
        // Loop to fill offspring list to offspring size
        for (int i = 0; offspring.size() < offspringSize; i++) {

            // Get some genomes by crossing over according to crossover probability
            if (offspringChoice == ReproductionMethod.CLEAR) {
                offspringChoice = ReproductionMethod.values()[makeWeightedChoice(new double[]{
                        this.crossoverRate,
                        this.elitismRate,
                        this.randomImmigrantRate})];
            }

            // Try to produce offspring
            Candidate newOffspring = ProduceOffspringIndividual(offspringChoice, i);
            if (newOffspring != null) {
                // Add this new offspring and reset accumulated messages, the failure counter and reproduction method.
                offspring.add(newOffspring);
                offspringChoice = ReproductionMethod.CLEAR;
                this.offspringRejectionMessages.clear();
                failureCounter = 0;
            } else {
                // Count this failure
                failureCounter++;
                if (failureCounter >= this.candidateList.size() * 24) {
                    throw new OffspringFailureOverflow(
                            String.format("Tried to create a new candidate %s times without a viable result", failureCounter),
                            this.offspringRejectionMessages);
                }
            }
        }
        candidateList = offspring;
        generationNumber++;
    }

    /**
     * Produces a novel candidate to by applying the given reproduction method.
     *
     * @param offspringChoice, The choice of reproducing method; use crossover, elitism, or random immigrant.
     * @param i                an index of the current list of candidates at which to pick parents for new offspring.
     * @return the produced
     */
    private Candidate ProduceOffspringIndividual(ReproductionMethod offspringChoice, int i) {
//        System.out.println("offspringChoice = " + offspringChoice);
        if (offspringChoice == ReproductionMethod.CROSSOVER) {
            // Get the recombined genome by crossing over
            ImmutablePair<Species, List<Integer>> newGenome = getRecombinedGenome(i);
            if (newGenome == null) return null;
            // Mutate the recombined genome
            List<Integer> reactantGenome = newGenome.right;
            mutate(reactantGenome);
            return finalizeOffspring(reactantGenome, newGenome.left);
        } else if (offspringChoice == ReproductionMethod.ELITISM) {
            // Get the recombined genome by crossing over
            Candidate elitist = this.candidateList.get(i % this.candidateList.size());
            List<Integer> newGenome = elitist.getGenotype();
            // Mutate the recombined genome
            mutate(newGenome);
            return finalizeOffspring(newGenome, elitist.getSpecies());
        } else if (offspringChoice == ReproductionMethod.RANDOM_IMMIGRANT) {
            // Introduce a random immigrant
            return introduceRandomImmigrant();
        }
        // Throw exception when another reproduction method is wanted.
        throw new RuntimeException("Reproduction method '" + offspringChoice.toString() + "' is not yet implemented!");
    }

    /**
     * Finalize a candidate by converting the given new genome, which is a list of indices representing reactants,
     * to a full candidate with Chemaxon's Reactor API.
     *
     * @param newGenome, a list of indices representing reactants
     * @param species The species that the new offspring should belong to
     * @return the new, finalized candidate when this was created successfully. If either Reactor did not produce a
     * product or if the produced candidate was not valid null is returned. Why the offspring was rejected is added
     * to the offSpringRejectionMessages field.
     */
    private Candidate finalizeOffspring(List<Integer> newGenome, Species species) {
        Candidate newCandidate = new Candidate(newGenome, species);
        newCandidate.setMaxHydrogenBondAcceptors(this.maxHydrogenBondAcceptors);
        newCandidate.setMaxHydrogenBondDonors(this.maxHydrogenBondDonors);
        newCandidate.setMaxMolecularMass(this.maxMolecularMass);
        newCandidate.setMaxPartitionCoefficient(this.maxPartitionCoefficient);

        if (speciesDeterminationMethod == SpeciesDeterminationMethod.FIXED &&
                newCandidate.finish(this.reactantLists)) {
            return newCandidate;
        } else if (speciesDeterminationMethod == SpeciesDeterminationMethod.DYNAMIC &&
                newCandidate.finish(this.reactantLists, this.species)) {
            return newCandidate;
        }
        this.offspringRejectionMessages.add(newCandidate.getRejectionMessage());
        return null;
    }

    /**
     * A method that creates a random new immigrant.
     *
     * @return a new individual (random immigrant).
     */
    private Candidate introduceRandomImmigrant() {
        if (this.speciesDeterminationMethod == SpeciesDeterminationMethod.FIXED) {
            // Get one of the species to create an individual from
            Species randomSpecies = this.species.get(random.nextInt(this.species.size()));

            // Try to generate a new individual or candidate with these species
            return new RandomCompoundReactor(1)
                    .randReact(this.reactantLists, Collections.singletonList(randomSpecies)).get(0); // 1 new individual at index 0
        } else if (this.speciesDeterminationMethod == SpeciesDeterminationMethod.DYNAMIC) {
            return new RandomCompoundReactor(1)
                    .randReact(this.reactantLists, this.species).get(0);
        } else {
            // Throw exception when another determination method is selected.
            throw new RuntimeException("Species determination method '" + speciesDeterminationMethod.toString() +
                    "' is not yet implemented!");
        }
    }

    /**
     * Select the parents according to the method that is set.
     * If the method flag was set to cleared, do nothing.
     * <p>
     * The amount of individuals selected is the amount of candidates multiplied by the selection fraction and rounded
     * up to the nearest integer.
     *
     * @throws UnSelectablePopulationException if the population was either empty or if the first candidate has a
     *                                         fitness of 0.
     */
    private void selectParents() throws UnSelectablePopulationException {
        // Get amount of parents to multiply the selection rate with for the amount of parents to select
        int selectionSize = (int) Math.ceil(this.size() * this.selectionFraction);
        this.filterTooDeviantParents();
        // Check that the candidates exist and have a score
        if (candidateList.size() == 0) {
            throw new UnSelectablePopulationException("The population is empty");
        }
        if (candidateList.get(0).getFitness() == null) {
            throw new UnSelectablePopulationException("The first candidate score is null");
        }
        // Select the parents according to the method that is set
        if (this.selectionMethod == SelectionMethod.FITNESS_PROPORTIONATE_SELECTION) {
            candidateList = this.fitnessProportionateSelection(selectionSize);
        } else if (this.selectionMethod == SelectionMethod.TRUNCATED_SELECTION) {
            candidateList = this.truncatedSelection(selectionSize);
        } else if (this.selectionMethod == SelectionMethod.TOURNAMENT_SELECTION) {
            candidateList = this.tournamentSelection(selectionSize);
        }
        // Do nothing if the flag is cleared
    }

    /**
     * Filters the parents with an rmsd larger than the max set rmsd.
     * The rmsd stands for the deviation between the anchor and
     * its matching substructure in the candidates best conformer.
     */
    private void filterTooDeviantParents() {
        // Store size before filtering.
        int sizeBeforeFiltering = candidateList.size();
        // When the common substructure to anchor rmsd is lower or equal to the max, keep it.
        candidateList = candidateList.stream()
                .filter(parent -> parent.getCommonSubstructureToAnchorRmsd() <= this.maxAnchorMinimizedRmsd)
                .collect(Collectors.toList());
        tooDistantConformerCounter += candidateList.size() - sizeBeforeFiltering;
    }

    /**
     * A selection method that selects individuals probabilistically by using the fitness score.
     *
     * @param selectionSize The amount of individuals selected.
     * @return the selected individuals
     */
    private List<Candidate> fitnessProportionateSelection(int selectionSize) {
        List<Candidate> selectedParents = new ArrayList<>();
        // Select the amount of parents corresponding to the total parents multiplied by the selection rate
        while (selectedParents.size() < selectionSize) {
            selectedParents.add(candidateList.get(rouletteSelect()));
        }
        return selectedParents;
    }

    /**
     * A selection method that selects the individuals with the best score.
     *
     * @param selectionSize The amount of individuals selected.
     * @return the selected individuals.
     */
    private List<Candidate> truncatedSelection(int selectionSize) {
        Collections.reverse(this.candidateList);
        return this.candidateList.subList(0, selectionSize);
    }

    /**
     * A selection method that selects the best individual in randomly selected sub-lists (tournaments),
     * of size k (the tournament size), of the candidate list.
     *
     * @param selectionSize The amount of individuals selected.
     * @return the selected individuals
     */
    private List<Candidate> tournamentSelection(int selectionSize) {
        List<Candidate> selectedParents = new ArrayList<>();

        // Check if the tournament size is less than the size of the candidate list
        int localTournamentSize = Math.min(this.tournamentSize, candidateList.size());

        // Select the amount of parents corresponding to the total parents multiplied by the selection rate
        while (selectedParents.size() < selectionSize) {
            // Get the best candidate in the tournament
            selectedParents.add(Collections.max(candidateList.subList(0, localTournamentSize)));
            Collections.shuffle(candidateList);
        }
        return selectedParents;
    }

    /**
     * Method that selects an individual probabilistically based on the fitness score.
     *
     * @return the index of the individual selected.
     */
    private int rouletteSelect() {
        // Create a stream to get the score of every candidate and convert this to a double
        return makeWeightedChoice(candidateList.stream()
                .map(Candidate::getNormFitness)
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
        // Get sum of fitness scores.
        double weightsSum = DoubleStream.of(weights).sum();
        // get a random value.
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
    private ImmutablePair<Species, List<Integer>> getRecombinedGenome(int i) {
        // Get the index of two parents to perform crossover between the two
        int firstParentIndex = i % this.candidateList.size();
        int otherParentIndex = (i + 1) % this.candidateList.size();
        // Get the two parents
        Candidate firstParent = this.candidateList.get(firstParentIndex);
        Candidate otherParent = this.candidateList.get(otherParentIndex);
//        System.out.printf("%s (%s) * %s (%s)%n",
//                firstParent.getGenotype(), firstParent.getSpecies(),
//                otherParent.getGenotype(), otherParent.getSpecies());
        // Perform crossover between the two
        return firstParent.crossover(otherParent, interspeciesCrossoverMethod);
    }

    @Override
    public String toString() {
        List<Double> scores = candidateList.stream()
                .map(Candidate::getRawScore)
                .collect(Collectors.toList());
        OptionalDouble average = scores.stream().mapToDouble(v -> v).average();
        return String.format(
                "Generation %d, individual count = %d %n" +
                        " agv | min | max %n %3.0f | %3.0f | %3.0f ",
                generationNumber, candidateList.size(),
                average.isPresent() ? average.getAsDouble() : Double.NaN,
                Collections.min(scores), Collections.max(scores));
    }

    /**
     * Introduce mutations in the genome according to the mutation rate and the set mutation method.
     *
     * @param genome to introduce mutations in.
     */
    private void mutate(List<Integer> genome) {
        // Loop through each gene and get a mutation substitute
        // This can be either the current one (i) or a new one
        // The change that i is chosen is equal to 1 - mutation rate
        for (int i = 0; i < genome.size(); i++) {
            int allele = genome.get(i);
            int reactantIndex = getMutationSubstitute(i, allele);
            genome.set(i, reactantIndex);
        }
    }

    /**
     * Method that introduces mutations with either a distance dependent method
     * or a distance independent method.
     *
     * @param reactantsListIndex, the index of the reactants list
     * @param allele              the index of the allele in the reactant list
     * @return the index of the chosen reactant from the reactant list
     */
    private int getMutationSubstitute(int reactantsListIndex, int allele) {
        if (this.mutationMethod == MutationMethod.DISTANCE_DEPENDENT) {
            // Check if similarity matrix was calculated.
//            System.out.println("this.alleleSimilarities = " + Arrays.deepToString(this.alleleSimilarities));
            if (this.alleleSimilarities == null) {
                throw new RuntimeException("Allele similarity matrices should be defined " +
                        "for distance dependant mutation!");
            }
            // If the similarities for this allele with other alleles has not yet been calculated, calculate these now
            if (alleleSimilarities[reactantsListIndex][allele][allele] == 0) {
                computeSpecificAlleleSimilarities(reactantsListIndex, allele);
            }
            // Return allele substitute index
            return makeWeightedChoice(alleleSimilarities[reactantsListIndex][allele]);
        } else if (this.mutationMethod == MutationMethod.DISTANCE_INDEPENDENT) {
            return makeChoice(reactantLists.get(reactantsListIndex).size(), allele);
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

    /**
     * Method making the list of candidates publicly available as a stream.
     *
     * @return the population stream
     */
    public Stream<Candidate> stream() {
        return candidateList.stream();
    }

    /**
     * Method returning the current size of the list of candidates.
     *
     * @return the size of the list of candidates
     */
    public int size() {
        return candidateList.size();
    }

    @Override
    public Iterator<Candidate> iterator() {
        return this.candidateList.iterator();
    }

    /**
     * Reproduction methods that can be chosen.
     */
    private enum ReproductionMethod {
        CROSSOVER, ELITISM, RANDOM_IMMIGRANT, CLEAR
    }

    /**
     * Selection methods that can be chosen.
     */
    public enum SelectionMethod {
        CLEAR("Clear"),
        FITNESS_PROPORTIONATE_SELECTION("Fitness proportionate selection"),
        TRUNCATED_SELECTION("Truncated selection"),
        TOURNAMENT_SELECTION("Tournament selection");

        private final String text;

        SelectionMethod(String text) {
            this.text = text;
        }

        public static SelectionMethod fromString(String text) {
            for (SelectionMethod method : SelectionMethod.values()) {
                if (method.text.equalsIgnoreCase(text)) {
                    return method;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }

    /**
     * Mutation methods that can be chosen.
     */
    public enum MutationMethod {
        DISTANCE_DEPENDENT("Distance dependent"),
        DISTANCE_INDEPENDENT("Distance independent");

        private final String text;

        MutationMethod(String text) {
            this.text = text;
        }

        public static MutationMethod fromString(String text) {
            for (MutationMethod method : MutationMethod.values()) {
                if (method.text.equalsIgnoreCase(text)) {
                    return method;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }

        public String getText() {
            return this.text;
        }
    }

    /**
     * Methods that can be chosen for guiding crossover between candidates that belong to different species.
     */
    public enum InterspeciesCrossoverMethod {
        NONE("None"),
        AT_SPECIES_INTERSECTION("Intersection"),
        COMPLETE("Complete");

        private final String text;

        InterspeciesCrossoverMethod(String text) {
            this.text = text;
        }

        public static InterspeciesCrossoverMethod fromString(String text) {
            for (InterspeciesCrossoverMethod method : InterspeciesCrossoverMethod.values()) {
                if (method.text.equalsIgnoreCase(text)) {
                    return method;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }

        public String getText() {
            return this.text;
        }
    }

    /**
     * Methods that can be chosen for determining to which species a candidate shall belong.
     */
    public enum SpeciesDeterminationMethod {
        DYNAMIC("Dynamic"),
        FIXED("Fixed");

        private final String text;

        SpeciesDeterminationMethod(String text) {
            this.text = text;
        }

        public static SpeciesDeterminationMethod fromString(String text) {
            for (SpeciesDeterminationMethod method : SpeciesDeterminationMethod.values()) {
                if (method.text.equalsIgnoreCase(text)) {
                    return method;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }

        public String getText() {
            return this.text;
        }
    }
}
