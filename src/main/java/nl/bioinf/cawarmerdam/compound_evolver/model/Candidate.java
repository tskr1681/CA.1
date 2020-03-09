/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolExporter;
import chemaxon.marvin.calculations.ElementalAnalyserPlugin;
import chemaxon.marvin.calculations.HBDAPlugin;
import chemaxon.marvin.calculations.IUPACNamingPlugin;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.EnumColor;
import nl.bioinf.cawarmerdam.compound_evolver.util.BBBScoreCalculator;
import nl.bioinf.cawarmerdam.compound_evolver.util.QuantitativeDrugEstimateCalculator;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Candidate implements Comparable<Candidate> {

    private static AtomicLong currentValue = new AtomicLong(0L);
    private final long identifier;
    private final int genomeSize;
    private CompoundEvolver.FitnessMeasure fitnessMeasure;
    private Path conformersFile;
    private Path fixedConformersFile;
    private Path scoredConformersFile;
    private Double rawScore;
    private Double ligandLipophilicityEfficiency;
    private Double maxHydrogenBondDonors = null;
    private Double maxHydrogenBondAcceptors = null;
    private Double maxMolecularMass = null;
    private Double maxPartitionCoefficient = null;
    private final HBDAPlugin hydrogenBondPlugin = new HBDAPlugin();
    private final logPPlugin logPPlugin = new logPPlugin();
    private List<Integer> genotype;
    private Molecule phenotype;
    private String rejectionMessage;
    private boolean isScored;
    private double normFitness;
    private double ligandEfficiency;
    private Species species;
    private final Random random = new Random();
    private List<Double> conformerScores;
    private Path minimizationOutputFilePath;
    private double minQED;
    private EnumColor color;
    private double minBBB;
    private Molecule[] reactants;

    /**
     * Constructor for candidate instance.
     *
     * @param genotype The genotype that corresponds to the candidate.
     */
    public Candidate(List<Integer> genotype) {
        this.genotype = genotype;
        this.genomeSize = this.genotype.size();
        this.identifier = currentValue.getAndIncrement();
    }

    /**
     * Constructor for candidate instance with dynamic species.
     *
     * @param genotype The genotype that corresponds to the candidate.
     * @param species  A list of possible species to select from
     */
    public Candidate(List<Integer> genotype, Species species) {
        this(genotype);
        this.species = species;

    }

    /**
     * Method responsible for completing this constructed candidate. It uses this candidates construction to
     * determine if the candidate is valid. The reactants that where chosen might not be viable, in which case
     * the rejection message field will be assigned, and the method will return false.
     * <p>
     * This method will use the species that this candidate was initiated with. Will run a runtime exception if
     * the species field is null.
     *
     * @param reactantLists The entire pool of reactants for every reactant in the current experiment.
     * @return true if this candidate was viable and valid, false if not.
     */
    boolean finish(List<List<Molecule>> reactantLists) {
        if (this.species == null) throw new RuntimeException("Species was not specified");
        return finish(reactantLists, this.species);
    }

    /**
     * Method responsible for completing this constructed candidate. It uses this candidates construction to
     * determine if the candidate is valid. The reactants that where chosen might not be viable, in which case
     * the rejection message field will be assigned, and the method will return false.
     * <p>
     * Multiple species should be supplied in this method. The species are checked one by one until a working
     * scheme was encountered.
     *
     * @param reactantLists The entire pool of reactants for every reactant in the current experiment.
     * @param species       The list of species to try.
     * @return true if this candidate was viable and valid, false if not.
     */
    boolean finish(List<List<Molecule>> reactantLists, List<Species> species) {
        for (Species singleSpecies : species) {
            boolean isFinished = finish(reactantLists, singleSpecies);
            if (isFinished) {
                this.species = singleSpecies;
                return true;
            }
        }
        return false;
    }

    /**
     * Method responsible for completing this constructed candidate. It uses this candidates construction to
     * determine if the candidate is valid. The reactants that where chosen might not be viable, in which case
     * the rejection message field will be assigned, and the method will return false.
     *
     * @param reactantLists The entire pool of reactants for every reactant in the current experiment.
     * @param species       The species to use for this candidate.
     * @return true if this candidate was viable and valid, false if not.
     */
    private boolean finish(List<List<Molecule>> reactantLists, Species species) {
        // get Reactants from the indices
        Molecule[] reactants = species.getReactantsSubset(getReactantsFromIndices(reactantLists));
        try {
            // Not sure of the exact cause, but this is needed to prevent random, otherwise unexplainable errors
            Reactor reaction = SerializationUtils.clone(species.getReaction());
            reaction.restart();
            // Setup for multithreading, which in case is used for a timeout

            List<Molecule> phenotypes;
            phenotypes = react(reaction, reactants);
            if (phenotypes != null && phenotypes.size() != 0) {
                this.phenotype = phenotypes.get((int) (Math.random() * phenotypes.size()));
                if (this.isValid()) {
                    this.reactants = reactants;
                    return true;
                } else {
                    return false;
                }
            }
        } catch (ReactionException | IllegalArgumentException | IndexOutOfBoundsException e) {
            this.rejectionMessage = "Reactor produced an error";
            e.printStackTrace();
            return false;
        }
        System.out.println("Reaction with the following reactants failed: " +
                Arrays.stream(reactants).map(molecule -> molecule.toFormat("smiles"))
                        .collect(Collectors.joining("; ")));
        this.rejectionMessage = "Reactor could not produce a reactions product";
        return false;
    }

    @SuppressWarnings("deprecation")
    private List<Molecule> react(Reactor reaction, Molecule[] reactants) throws ReactionException {
        reaction.setReactants(reactants);
        Callable<List<Molecule>> task = () -> {
            try {
                List<Molecule> temp = new ArrayList<>();
                Molecule[] product;
                while ((product = reaction.react())!=null) {
                    temp.add(product[0]);
                }
                return temp;
            } catch (Exception e) {
                System.out.println("e.getMessage() = " + e.getMessage());
                return null;
            }
        };
        FutureTask<List<Molecule>> future = new FutureTask<>(task);
        Thread t = new Thread(future);
        t.start();
        // Try to get the result, unless it takes more than 5 seconds, in which case we stop the thread and return false
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException ex) {
            this.rejectionMessage = ex.getMessage() != null ? ex.getMessage() : Arrays.toString(ex.getStackTrace());
            System.out.println("Reactor produced the following error: " + this.rejectionMessage);
            future.cancel(true);
            t.stop();
            return null;
        }
    }

    /**
     * A method selecting reactants from the reactant lists with the given list of indices.
     *
     * @param reactantLists, a list of lists of reactants
     * @return an array of reactants from the reactant lists.
     */
    private List<Molecule> getReactantsFromIndices(List<List<Molecule>> reactantLists) {
        return IntStream.range(0, reactantLists.size())
                .mapToObj(i -> reactantLists.get(i).get(this.genotype.get(i)))
                .collect(Collectors.toList());
    }

    /**
     * Getter for the identifier field.
     *
     * @return the identifier.
     */
    public long getIdentifier() {
        return identifier;
    }

    /**
     * Gets the reactants used for this candidate
     * @return the reactants
     */
    public Molecule[] getReactants() {
        return reactants;
    }

    /**
     * Setter for the maximum hydrogen bond donors that are allowed for this candidate.
     *
     * @param maxHydrogenBondDonors The maximum amount of hydrogen bond donors.
     */
    void setMaxHydrogenBondDonors(Double maxHydrogenBondDonors) {
        this.maxHydrogenBondDonors = maxHydrogenBondDonors;
    }

    /**
     * Setter for the maximum hydrogen bond acceptors that are allowed for this candidate.
     *
     * @param maxHydrogenBondAcceptors The maximum amount of hydrogen bond acceptors.
     */
    void setMaxHydrogenBondAcceptors(Double maxHydrogenBondAcceptors) {
        this.maxHydrogenBondAcceptors = maxHydrogenBondAcceptors;
    }

    /**
     * Getter for the minimum QED (quantitive estimate of druglikeness)
     * @return the minimum QED
     */
    public double getMinQED() {
        return minQED;
    }

    /**
     * Setter for the minimum QED (quantitive estimate of druglikeness)
     * @param minQED the minimum QED
     */
    public void setMinQED(double minQED) {
        this.minQED = minQED;
    }

    /**
     * Getter for the minimum BBB score (Blood-Brain Barrier score)
     * @return the minimum BBB score
     */
    public double getMinBBB() {
        return minBBB;
    }

    /**
     * Setter for the minimum BBB score (Blood-Brain Barrier score)
     * @param minBBB the minimum BBB score
     */
    public void setMinBBB(double minBBB) {
        this.minBBB = minBBB;
    }

    public EnumColor getColor() {
        return color;
    }

    public void setColor(EnumColor color) {
        this.color = color;
    }

    /**
     * Setter for the maximum molecular mass that are allowed for this candidate.
     *
     * @param maxMolecularMass The maximum molecular mass.
     */
    void setMaxMolecularMass(Double maxMolecularMass) {
        this.maxMolecularMass = maxMolecularMass;
    }

    /**
     * Setter for the maximum partition coefficient that is allowed for this candidate.
     *
     * @param maxPartitionCoefficient The maximum partition coefficient.
     */
    void setMaxPartitionCoefficient(Double maxPartitionCoefficient) {
        this.maxPartitionCoefficient = maxPartitionCoefficient;
    }

    /**
     * Getter for the rawScore of this candidate
     *
     * @return the rawScore of this candidate
     */
    public Double getRawScore() {
        return rawScore;
    }

    /**
     * Setter for the rawScore attribute
     *
     * @param rawScore the rawScore of this candidate
     */
    public void setRawScore(double rawScore) {
        this.rawScore = rawScore;
        this.isScored = true;
    }

    /**
     * Getter for the fitness.
     * <p>
     * This value depends on the fitness measure that is set:
     * Negative of the raw score in case the fitness measure 'affinity',
     * Negative of the ligand efficiency in case the fitness measure 'ligand efficiency',
     * Negative of the ligand lipophilicity efficiency in case the fitness measure 'ligand lipophilicity efficiency'
     *
     * @return the fitness according to the fitness measure that is set.
     */
    public Double getFitness() {
        switch (this.fitnessMeasure) {
            case AFFINITY:
                return (-rawScore);
            case LIGAND_EFFICIENCY:
                return (-ligandEfficiency);
            case LIGAND_LIPOPHILICITY_EFFICIENCY:
                return (-ligandLipophilicityEfficiency);
            default:
                throw new RuntimeException("Not implemented");
        }
    }

    /**
     * Method that calculates the normalized fitness given a minimum fitness and maximum fitness
     *
     * @param minFitness The minimum fitness in the population.
     * @param maxFitness The maximum fitness in the population.
     */
    public void calcNormFitness(double minFitness, double maxFitness) {
        this.normFitness = (getFitness() - minFitness) / (maxFitness - minFitness);
    }

    /**
     * Getter for the normFitness of this candidate.
     * Higher is better.
     *
     * @return the normFitness of this candidate.
     */
    public Double getNormFitness() {
        return this.normFitness;
    }

    /**
     * Setter for the normFitness of this candidate.
     * Higher is better.
     */
    public void setNormFitness(double normFitness) {
        this.normFitness = normFitness;
    }

    /**
     * Getter for the ligand efficiency.
     * Returns the rawScore divided by the number of heavy atoms.
     *
     * @return ligand efficiency
     */
    public Double getLigandEfficiency() {
        return ligandEfficiency;
    }

    /**
     * Method for calculating and setting the ligand efficiency.
     *
     * @throws PluginException if the heavy atom count could not be determined.
     */
    public void calculateLigandEfficiency() throws PluginException {
        this.ligandEfficiency = this.getRawScore() / this.getHeavyAtomCount();
    }

    /**
     * Getter for the ligand lipophilicity efficiency.
     *
     * @return The ligand lipophilicity efficiency.
     */
    public Double getLigandLipophilicityEfficiency() {
        return ligandLipophilicityEfficiency;
    }

    /**
     * Method responsible for calculating and setting the ligand lipophilicity efficiency.
     */
    public void calculateLigandLipophilicityEfficiency() {
        double kcalToJouleConstant = 4.186798188;
        this.ligandLipophilicityEfficiency = Math.log(-this.getRawScore() * kcalToJouleConstant) - logPPlugin.getlogPTrue();
    }

    /**
     * Getter for the genotype
     *
     * @return a list of alleles
     */
    public List<Integer> getGenotype() {
        return genotype;
    }

    /**
     * Getter for the phenotype
     *
     * @return the phenotype
     */
    public Molecule getPhenotype() {
        return phenotype;
    }

    /**
     * Getter for the name of the phenotype
     *
     * @return phenotype name
     * @throws PluginException if the name could not be obtained
     */
    public String getPhenotypeName() throws PluginException {
        // Initialize plugin
        IUPACNamingPlugin plugin = new IUPACNamingPlugin();

        // Set molecule and run the plugin
        plugin.setMolecule(this.getPhenotype());
        plugin.run();

        // Return the preferred IUPAC name. Can also be the traditional name
        return plugin.getPreferredIUPACName();
    }

    public String getPhenotypeSmiles() throws IOException {
        return MolExporter.exportToFormat(this.getPhenotype(), "smiles");
    }

    /**
     * Performs crossover between this parent and another parent
     * for each gene either the allele from this parent is chosen or
     * the allele from the other parent is chosen.
     *
     * @param other                       parent to perform crossover with
     * @param interspeciesCrossoverMethod Specifies how to use crossover when different species are encountered.
     * @return the recombined genome.
     */
    ImmutablePair<Species, List<Integer>> crossover(
            Candidate other,
            Population.InterspeciesCrossoverMethod interspeciesCrossoverMethod) {

        // Return null if the interspecies crossover method is set to not do crossover with different species
        if (interspeciesCrossoverMethod == Population.InterspeciesCrossoverMethod.NONE &&
                (!this.species.equals(other.species))) return null;

        // Get either the species from this or other
        int candidateChoice = random.nextInt(2);
        Species randomSpecies = (candidateChoice == 0 ? this.species : other.species);
        // Get the indices of the reactant pool that are in both this species and the other species
        List<Integer> intersectionOfIndices = this.species.reactantIndexIntersection(other.species);

        // Get crossover points to do uniform crossing over
        boolean[] crossoverPoints = generateCrossoverPoints();

        List<Integer> reactantGenome = new ArrayList<>();
        for (int i = 0; i < genomeSize; i++) {
            // For each gene in the genome decide if crossover should be applied by checking if
            // the current method for interspecies crossover is set to complete or if the
            // reactant is used by both the candidate species
            if (interspeciesCrossoverMethod == Population.InterspeciesCrossoverMethod.COMPLETE ||
                    intersectionOfIndices.contains(i)) {
                // Select the allele from this candidates genotype if true,
                // otherwise select the allele from the other candidate
                reactantGenome.add(crossoverPoints[i] ? this.getGenotype().get(i) : other.getGenotype().get(i));

            } else { // Reactant not used by both of the candidates species
                // Select the allele from this candidate if this candidates species was chosen,
                // otherwise select the allele from the other candidate
                reactantGenome.add(candidateChoice == 0 ? this.getGenotype().get(i) : other.getGenotype().get(i));
                // candidate choice is either 0 (this) or 1 (other)
            }
        }

        // Return the new genotype
        return new ImmutablePair<>(randomSpecies, reactantGenome);
        // Should probably also return the child generated from the inverse of the crossover points:
        // P1 = 2, 4, 8
        // P2 = 1, 6, 7
        // Crossover points = 1, 0, 1
        // O1 = 2, 6, 8
        // O2 = 1, 4, 7
    }

    /**
     * Generates crossover points.
     *
     * @return an array of booleans with the length of the genome
     */
    private boolean[] generateCrossoverPoints() {
        boolean[] arr = new boolean[genomeSize];
        // Get a random boolean for every gene in the genome
        for (int i = 0; i < genomeSize; i++) {
            arr[i] = random.nextBoolean();
        }
        return arr;
    }

    /**
     * Method checking if this candidate is valid concerning the set maximum amount of
     * hydrogen bond donors and acceptors, the maximum molecular mass and the maximum partition coefficient.
     *
     * @return true if valid, false if not.
     */
    private boolean isValid() {
        calculateLipinskiValues();
        if (maxHydrogenBondDonors != null) {
            System.out.println("Rejecting candidate " + this.getIdentifier() + "for a hydrogen bond donor count over the maximum.");
            if (!isHydrogenBondDonorCountValid()) return false;
        }
        if (maxHydrogenBondAcceptors != null) {
            System.out.println("Rejecting candidate " + this.getIdentifier() + "for a hydrogen bond acceptor count over the maximum.");
            if (!isHydrogenBondAcceptorCountValid()) return false;
        }
        if (maxMolecularMass != null) {
            System.out.println("Rejecting candidate " + this.getIdentifier() + "for a molecular weight over the maximum.");
            if (!isMolecularMassValid()) return false;
        }
        if (maxPartitionCoefficient != null) {
            System.out.println("Rejecting candidate " + this.getIdentifier() + "for a partition coefficient over the maximum.");
            if (!isPartitionCoefficientValid()) return false;
        }
        if (minQED != 0) {
            System.out.println("Rejecting candidate " + this.getIdentifier() + "for a QED under the minimum.");
            if ((QuantitativeDrugEstimateCalculator.getQED(this.phenotype) < minQED)) return false;
        }
        if (minBBB != 0) {
            System.out.println("Rejecting candidate " + this.getIdentifier() + "for a BBB score under the minimum.");
            if (BBBScoreCalculator.getBBB(this.phenotype) < minBBB) return false;
        }
        return true;
    }

    /**
     * Method that calculates hydrogen bond values.
     */
    private void calculateLipinskiValues() {
        if (maxHydrogenBondAcceptors != null || maxHydrogenBondDonors != null) {
            try {
                this.hydrogenBondPlugin.setMolecule(this.phenotype);
                this.hydrogenBondPlugin.setExcludeSulfur(true);
                this.hydrogenBondPlugin.setExcludeHalogens(true);
                this.hydrogenBondPlugin.run();
            } catch (PluginException e) {
                throw new RuntimeException("Could not set molecule in plugin: " + e.toString());
            }
        }
        this.runLogPPluginIfNotRan();
    }

    /**
     * Method that calculates the partition coefficient.
     */
    private void runLogPPluginIfNotRan() {
        try {
            this.logPPlugin.setMolecule(this.phenotype);
            this.logPPlugin.run();
        } catch (PluginException e) {
            throw new RuntimeException("Could not set molecule in plugin: " + e.toString());
        }
    }

    /**
     * Checks if the partition coefficient is valid concerning the set maximum.
     * <p>
     * Sets the rejection message of this candidate if this is not valid.
     *
     * @return true if the partition coefficient is valid, false if not.
     */
    private boolean isPartitionCoefficientValid() {
        double logPTrue = this.logPPlugin.getlogPTrue();
        boolean valid = logPTrue <= maxPartitionCoefficient;
        // Write why this molecule was invalid to create a descriptive error
        if (!valid) this.rejectionMessage = String.format("PartitionCoefficient was %s, should be <= %s",
                logPTrue,
                maxPartitionCoefficient);
        return valid;
    }

    /**
     * Checks if the molecular mass is valid concerning the set maximum.
     * <p>
     * Sets the rejection message of this candidate if this is not valid.
     *
     * @return true if the molecular mass is valid, false if not.
     */
    private boolean isMolecularMassValid() {
        double mass = this.phenotype.getExactMass();
        boolean valid = mass <= maxMolecularMass;
        // Write why this molecule was invalid to create a descriptive error
        if (!valid) this.rejectionMessage = String.format("Molecular mass was %s, should be <= %s",
                mass,
                maxMolecularMass);
        return valid;
    }

    /**
     * Checks if the hydrogen bond acceptor count is valid concerning the set maximum.
     * <p>
     * Sets the rejection message of this candidate if this is not valid.
     *
     * @return true if the hydrogen bond acceptor count is valid, false if not.
     */
    private boolean isHydrogenBondAcceptorCountValid() {
        int acceptorAtomCount = hydrogenBondPlugin.getAcceptorAtomCount();
        boolean valid = acceptorAtomCount <= maxHydrogenBondAcceptors;
        // Write why this molecule was invalid to create a descriptive error
        if (!valid) this.rejectionMessage = String.format("Hydrogen bond acceptor count was %s, should be <= %s",
                acceptorAtomCount,
                maxHydrogenBondAcceptors);
        return valid;
    }

    /**
     * Checks if the hydrogen bond donor count is valid concerning the set maximum.
     * <p>
     * Sets the rejection message of this candidate if this is not valid.
     *
     * @return true if the hydrogen bond donor count is valid, false if not.
     */
    private boolean isHydrogenBondDonorCountValid() {
        int donorAtomCount = hydrogenBondPlugin.getDonorAtomCount();
        boolean valid = donorAtomCount <= maxHydrogenBondDonors;
        // Write why this molecule was invalid to create a descriptive error
        if (!valid) this.rejectionMessage = String.format("Hydrogen bond donor count was %s, should be <= %s",
                donorAtomCount,
                maxHydrogenBondDonors);
        return valid;
    }

    /**
     * Calculates the heavy atom count for the phenotype
     *
     * @return the number of heavy atoms in the phenotype
     * @throws PluginException if the number of heavy atoms could not be determined
     */
    private int getHeavyAtomCount() throws PluginException {
        ElementalAnalyserPlugin plugin = new ElementalAnalyserPlugin();
        plugin.setMolecule(this.phenotype);

        plugin.run();

        // Get the amount of atoms and substract the amount of hydrogen atoms because heavy atom count is
        // not implemented in the Chemaxon API
        int allAtomCount = plugin.getAllAtomCount();
        int hydrogenAtomCount = plugin.getAtomCount(1);
        return allAtomCount - hydrogenAtomCount;
    }

    /**
     * Setter for the fitness measure.
     *
     * @param fitnessMeasure The measure to determine fitness with.
     */
    public void setFitnessMeasure(CompoundEvolver.FitnessMeasure fitnessMeasure) {
        this.fitnessMeasure = fitnessMeasure;
    }

    /**
     * Getter for the message that describes why this candidate was not viable or valid.
     *
     * @return the rejection message.
     */
    public String getRejectionMessage() {
        return rejectionMessage;
    }

    /**
     * Getter for the conformers file path.
     *
     * @return the path to the conformers file.
     */
    public Path getConformersFile() {
        return conformersFile;
    }

    /**
     * Setter for the conformers file path.
     *
     * @param conformersFile The path to the conformers file.
     */
    public void setConformersFile(Path conformersFile) {
        this.conformersFile = conformersFile;
    }

    /**
     * Getter for the fixed conformers file path.
     *
     * @return the path to the fixed conformers file.
     */
    public Path getFixedConformersFile() {
        return fixedConformersFile;
    }

    /**
     * Setter for the conformers file path.
     *
     * @param fixedConformersFile The path to the conformers file.
     */
    public void setFixedConformersFile(Path fixedConformersFile) {
        this.fixedConformersFile = fixedConformersFile;
    }

    /**
     * Getter for the scored conformers file path.
     *
     * @return the path to the fixed conformers file.
     */
    public Path getScoredConformersFile() {
        return scoredConformersFile;
    }

    /**
     * Setter for the conformers file path.
     *
     * @param scoredConformersFile The path to the conformers file.
     */
    public void setScoredConformersFile(Path scoredConformersFile) {
        this.scoredConformersFile = scoredConformersFile;
    }


    /**
     * Getter for if this candidate is scored.
     *
     * @return if this candidate is scored.
     */
    public boolean isScored() {
        return isScored;
    }

    /**
     * Getter for the species this candidate currently belongs to.
     *
     * @return the species this candidate currently belongs to.
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Getter for the conformer scores for this candidate.
     *
     * @return the scores of every conformer in a list.
     */
    public List<Double> getConformerScores() {
        return this.conformerScores;
    }

    /**
     * Setter for the conformer scores for this candidate.
     *
     * @param conformerScores the scores of every conformer in a list.
     */
    public void setConformerScores(List<Double> conformerScores) {

        this.conformerScores = conformerScores;
    }

    /**
     * Getter for the minimization output file path.
     *
     * @return the minimization output file path.
     */
    public Path getMinimizationOutputFilePath() {
        return minimizationOutputFilePath;
    }

    /**
     * Setter for the minimization output file path.
     *
     * @param minimizationOutputFilePath The minimization output file path.
     */
    public void setMinimizationOutputFilePath(Path minimizationOutputFilePath) {
        this.minimizationOutputFilePath = minimizationOutputFilePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return genotype.equals(candidate.getGenotype());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGenotype());
    }

    @Override
    public int compareTo(Candidate o) {
        return Double.compare(this.getNormFitness(), o.getNormFitness());
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "genotype=" + genotype +
                ", normFitness=" + normFitness +
                ", id=" + identifier +
                '}';
    }
}
