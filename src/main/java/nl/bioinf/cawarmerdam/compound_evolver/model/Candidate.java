/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.marvin.calculations.HBDAPlugin;
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

    private static Double maxHydrogenBondDonors = null;
    private static Double maxHydrogenBondAcceptors = null;
    private static Double maxMolecularMass = null;
    private static Double maxPartitionCoefficient = null;
    private final int genomeSize;
    private HBDAPlugin hydrogenBondPlugin = new HBDAPlugin();
    private List<Integer> genotype;
    private Molecule phenotype;
    private Double score;

    Candidate(List<Integer> genotype, Molecule phenotype) {
        this.genotype = genotype;
        this.phenotype = phenotype;
        genomeSize = this.genotype.size();
        try {
            hydrogenBondPlugin.setMolecule(this.phenotype);
        } catch (PluginException e) {
            throw new RuntimeException("Could not set molecule in plugin: " + e.toString());
        }
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
        if (maxHydrogenBondDonors != null) {
            if(!isHydrogenBondDonorCountValid()) return false;
        }
        if (maxHydrogenBondAcceptors != null) {
            if(!isHydrogenBondAcceptorCountValid()) return false;
        }
        if (maxMolecularMass != null) {
            if(!isMolecularMassValid()) return false;
        }
        if (maxPartitionCoefficient != null) {
            if(!isPartitionCoefficientValid()) return false;
        }
        return true;
    }

    private boolean isPartitionCoefficientValid() {
        return true;
    }

    private boolean isMolecularMassValid() {
        double mass = this.phenotype.getExactMass();
        return (mass <= maxMolecularMass);
    }

    private boolean isHydrogenBondAcceptorCountValid() {
        int acceptorAtomCount = hydrogenBondPlugin.getAcceptorAtomCount();
        return (acceptorAtomCount <= maxHydrogenBondAcceptors);
    }

    private boolean isHydrogenBondDonorCountValid() {
        int donorAtomCount = hydrogenBondPlugin.getDonorAtomCount();
        return (donorAtomCount <= maxHydrogenBondDonors);
    }
}
