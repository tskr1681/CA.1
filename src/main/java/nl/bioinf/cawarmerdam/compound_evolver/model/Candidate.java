/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.marvin.calculations.HBDAPlugin;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Candidate implements Comparable<Candidate>{

    private static Double MAX_HYDROGEN_BOND_DONORS = null;
    private static Double MAX_HYDROGEN_BOND_ACCEPTORS = null;
    private static Double MAX_MOLECULAR_MASS = null;
    private static Double MAX_PARTITION_COEFFICIENT = null;
    private final int genomeSize;
    private HBDAPlugin hydrogenBondPlugin = new HBDAPlugin();
    private logPPlugin logPPlugin = new logPPlugin();
    private List<Integer> genotype;
    private Molecule phenotype;
    private Double score;

    Candidate(List<Integer> genotype, Molecule phenotype) {
        this.genotype = genotype;
        this.phenotype = phenotype;
        this.genomeSize = this.genotype.size();
        if (MAX_HYDROGEN_BOND_ACCEPTORS != null || MAX_HYDROGEN_BOND_DONORS != null) {
            try {
                this.hydrogenBondPlugin.setMolecule(this.phenotype);
                this.hydrogenBondPlugin.setExcludeSulfur(true);
                this.hydrogenBondPlugin.setExcludeHalogens(true);
                this.hydrogenBondPlugin.run();
            } catch (PluginException e) {
                throw new RuntimeException("Could not set molecule in plugin: " + e.toString());
            }
        }
        if (MAX_PARTITION_COEFFICIENT != null) {
            try {
                this.logPPlugin.setMolecule(this.phenotype);
                this.logPPlugin.run();
            } catch (PluginException e) {
                throw new RuntimeException("Could not set molecule in plugin: " + e.toString());
            }
        }
    }

    public static void setMaxHydrogenBondDonors(Double maxHydrogenBondDonors) {
        MAX_HYDROGEN_BOND_DONORS = maxHydrogenBondDonors;
    }

    public static void setMaxHydrogenBondAcceptors(Double maxHydrogenBondAcceptors) {
        MAX_HYDROGEN_BOND_ACCEPTORS = maxHydrogenBondAcceptors;
    }

    public static void setMaxMolecularMass(Double maxMolecularMass) {
        MAX_MOLECULAR_MASS = maxMolecularMass;
    }

    public static void setMaxPartitionCoefficient(Double maxPartitionCoefficient) {
        MAX_PARTITION_COEFFICIENT = maxPartitionCoefficient;
    }

    /**
     * Getter for the score of this candidate
     * @return the score of this candidate
     */
    public Double getScore() {
        return score;
    }

    /**
     * Setter for the score attribute
     * @param score the score of this candidate
     */
    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Getter for the genotype
     * @return a list of alleles
     */
    public List<Integer> getGenotype() {
        return genotype;
    }

    /**
     * Getter for the phenotype
     * @return the phenotype
     */
    public Molecule getPhenotype() {
        return phenotype;
    }

    /**
     * Performs crossover between this parent and another parent
     * for each gene either the allele from this parent is chosen or
     * the allele from the other parent is chosen.
     * @param other parent to perform crossover with
     * @return the recombined genome.
     */
    List<Integer> crossover(Candidate other) {
        // Get crossover points to do uniform crossing over
        boolean[] crossoverPoints = generateCrossoverPoints();
        // Select the allele from this candidates genotype if true,
        // otherwise select hte allele from the other candidate
        // Return the new genotype
        return IntStream.range(0, genomeSize)
        .mapToObj(i -> crossoverPoints[i]? this.getGenotype().get(i) : other.getGenotype().get(i))
        .collect(Collectors.toList());
    }

    /**
     * Generates crossover points.
     * @return an array of booleans with the length of the genome
     */
    private boolean[] generateCrossoverPoints() {
        Random random = new Random();
        boolean[] arr = new boolean[genomeSize];
        // Get a random boolean for every gene in the genome
        for(int i = 0; i < genomeSize; i++) {
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
        return -Double.compare(this.getScore(), o.getScore());
    }

    public void setEnvironment(double pHLower, double pHUpper, double pHStep) {
        hydrogenBondPlugin.setpHLower(pHLower);
        hydrogenBondPlugin.setpHUpper(pHUpper);
        hydrogenBondPlugin.setpHStep(pHStep);
    }

    public boolean isValid() {
        if (MAX_HYDROGEN_BOND_DONORS != null) {
            if(!isHydrogenBondDonorCountValid()) return false;
        }
        if (MAX_HYDROGEN_BOND_ACCEPTORS != null) {
            if(!isHydrogenBondAcceptorCountValid()) return false;
        }
        if (MAX_MOLECULAR_MASS != null) {
            if(!isMolecularMassValid()) return false;
        }
        if (MAX_PARTITION_COEFFICIENT != null) {
            if(!isPartitionCoefficientValid()) return false;
        }
        return true;
    }

    private boolean isPartitionCoefficientValid() {
        double logPTrue = this.logPPlugin.getlogPTrue();
        return (logPTrue <= MAX_PARTITION_COEFFICIENT);
    }

    private boolean isMolecularMassValid() {
        double mass = this.phenotype.getExactMass();
        return (mass <= MAX_MOLECULAR_MASS);
    }

    private boolean isHydrogenBondAcceptorCountValid() {
        int acceptorAtomCount = hydrogenBondPlugin.getAcceptorAtomCount();
        return (acceptorAtomCount <= MAX_HYDROGEN_BOND_ACCEPTORS);
    }

    private boolean isHydrogenBondDonorCountValid() {
        int donorAtomCount = hydrogenBondPlugin.getDonorAtomCount();
        return (donorAtomCount <= MAX_HYDROGEN_BOND_DONORS);
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "genotype=" + genotype +
                ", score=" + score +
                '}';
    }
}
