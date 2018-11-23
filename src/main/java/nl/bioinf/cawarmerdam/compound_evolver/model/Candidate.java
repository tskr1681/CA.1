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
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Candidate implements Comparable<Candidate> {

    private final long identifier;
    private static AtomicLong currentValue = new AtomicLong(0L);
    private Double maxHydrogenBondDonors = null;
    private Double maxHydrogenBondAcceptors = null;
    private Double maxMolecularMass = null;
    private Double maxPartitionCoefficient = null;
    private final int genomeSize;
    private HBDAPlugin hydrogenBondPlugin = new HBDAPlugin();
    private logPPlugin logPPlugin = new logPPlugin();
    private List<Integer> genotype;
    private Molecule phenotype;
    private Double rawScore;
    private double normFitness;
    private double ligandEfficiency;
    private CompoundEvolver.FitnessMeasure fitnessMeasure;
    private String rejectionMessage;

    Candidate(List<Integer> genotype, Molecule phenotype) {
        this.genotype = genotype;
        this.phenotype = phenotype;
        this.genomeSize = this.genotype.size();
        this.identifier = currentValue.getAndIncrement();
    }

    public long getIdentifier() {
        return identifier;
    }

    public void setMaxHydrogenBondDonors(Double maxHydrogenBondDonors) {
        this.maxHydrogenBondDonors = maxHydrogenBondDonors;
    }

    public void setMaxHydrogenBondAcceptors(Double maxHydrogenBondAcceptors) {
        this.maxHydrogenBondAcceptors = maxHydrogenBondAcceptors;
    }

    public void setMaxMolecularMass(Double maxMolecularMass) {
        this.maxMolecularMass = maxMolecularMass;
    }

    public void setMaxPartitionCoefficient(Double maxPartitionCoefficient) {
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

    public Double getFitness() {
        Double fitness;
        if (this.fitnessMeasure == CompoundEvolver.FitnessMeasure.AFFINITY) {
            return (-rawScore);
        } else if (this.fitnessMeasure == CompoundEvolver.FitnessMeasure.LIGAND_EFFICIENCY) {
            return (-ligandEfficiency);
        } else {
            throw new RuntimeException("Not implemented");
        }
    }

    public void setNormFitness(double minFitness, double maxFitness) {
        this.normFitness = -(getFitness() - minFitness) / (maxFitness - minFitness);
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
     * Setter for the rawScore attribute
     *
     * @param rawScore the rawScore of this candidate
     */
    public void setRawScore(double rawScore) {
        this.rawScore = rawScore;
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

    public void calculateLigandEfficiency() throws PluginException {
        this.ligandEfficiency = this.getRawScore() / this.getHeavyAtomCount();
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
     * @param other parent to perform crossover with
     * @return the recombined genome.
     */
    List<Integer> crossover(Candidate other) {
        // Get crossover points to do uniform crossing over
        boolean[] crossoverPoints = generateCrossoverPoints();
        // Select the allele from this candidates genotype if true,
        // otherwise select the allele from the other candidate
        // Return the new genotype
        return IntStream.range(0, genomeSize)
                .mapToObj(i -> crossoverPoints[i] ? this.getGenotype().get(i) : other.getGenotype().get(i))
                .collect(Collectors.toList());
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
        Random random = new Random();
        boolean[] arr = new boolean[genomeSize];
        // Get a random boolean for every gene in the genome
        for (int i = 0; i < genomeSize; i++) {
            arr[i] = random.nextBoolean();
        }
        return arr;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return Objects.equals(getGenotype(), candidate.getGenotype());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGenotype());
    }

    @Override
    public int compareTo(Candidate o) {
        return -Double.compare(this.getNormFitness(), o.getNormFitness());
    }

    public void setEnvironment(double pHLower, double pHUpper, double pHStep) {
        hydrogenBondPlugin.setpHLower(pHLower);
        hydrogenBondPlugin.setpHUpper(pHUpper);
        hydrogenBondPlugin.setpHStep(pHStep);
    }

    boolean isValid() {
        calculateLipinskiValues();
        if (maxHydrogenBondDonors != null) {
            if (!isHydrogenBondDonorCountValid()) return false;
        }
        if (maxHydrogenBondAcceptors != null) {
            if (!isHydrogenBondAcceptorCountValid()) return false;
        }
        if (maxMolecularMass != null) {
            if (!isMolecularMassValid()) return false;
        }
        if (maxPartitionCoefficient != null) {
            return isPartitionCoefficientValid();
        }
        return true;
    }

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
        if (maxPartitionCoefficient != null) {
            try {
                this.logPPlugin.setMolecule(this.phenotype);
                this.logPPlugin.run();
            } catch (PluginException e) {
                throw new RuntimeException("Could not set molecule in plugin: " + e.toString());
            }
        }
    }

    private boolean isPartitionCoefficientValid() {
        double logPTrue = this.logPPlugin.getlogPTrue();
        boolean valid = logPTrue <= maxPartitionCoefficient;
        // Write why this molecule was invalid to create a descriptive error
        if (valid) this.rejectionMessage = String.format("PartitionCoefficient was %s, should be <= %s",
                logPTrue,
                maxPartitionCoefficient);
        return valid;
    }

    private boolean isMolecularMassValid() {
        double mass = this.phenotype.getExactMass();
        boolean valid = mass <= maxMolecularMass;
        // Write why this molecule was invalid to create a descriptive error
        if (valid) this.rejectionMessage = String.format("Molecular mass was %s, should be <= %s",
                mass,
                maxMolecularMass);
        return valid;
    }

    private boolean isHydrogenBondAcceptorCountValid() {
        int acceptorAtomCount = hydrogenBondPlugin.getAcceptorAtomCount();
        boolean valid = acceptorAtomCount <= maxHydrogenBondAcceptors;
        // Write why this molecule was invalid to create a descriptive error
        if (valid) this.rejectionMessage = String.format("Maximum hydrogen bond acceptor count was %s, should be <= %s",
                acceptorAtomCount,
                maxHydrogenBondAcceptors);
        return valid;
    }

    private boolean isHydrogenBondDonorCountValid() {
        int donorAtomCount = hydrogenBondPlugin.getDonorAtomCount();
        boolean valid = donorAtomCount <= maxHydrogenBondDonors;
        // Write why this molecule was invalid to create a descriptive error
        if (valid) this.rejectionMessage = String.format("Maximum hydrogen bond donor count was %s, should be <= %s",
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
    public int getHeavyAtomCount() throws PluginException {
        ElementalAnalyserPlugin plugin = new ElementalAnalyserPlugin();
        plugin.setMolecule(this.phenotype);

        plugin.run();

        // Get the amount of atoms and substract the amount of hydrogen atoms because heavy atom count is
        // not implemented in the Chemaxon API
        int allAtomCount = plugin.getAllAtomCount();
        int hydrogenAtomCount = plugin.getAtomCount(1);
        return allAtomCount - hydrogenAtomCount;
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "genotype=" + genotype +
                ", rawScore=" + rawScore +
                '}';
    }

    public void setFitnessMeasure(CompoundEvolver.FitnessMeasure fitnessMeasure) {
        this.fitnessMeasure = fitnessMeasure;
    }

    public String getRejectionMessage() {
        return rejectionMessage;
    }
}
