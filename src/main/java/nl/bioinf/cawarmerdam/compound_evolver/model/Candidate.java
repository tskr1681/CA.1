/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.Molecule;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Candidate {

    private final int genomeSize;
    private Molecule[] genotype;
    private Molecule phenotype;
    private double score;

    Candidate(Molecule[] genotype, Molecule phenotype) {
        this.genotype = genotype;
        this.phenotype = phenotype;
        genomeSize = this.genotype.length;
    }

    /**
     * Getter for the score of this candidate
     * @return The score of this candidate
     */
    public double getScore() {
        return score;
    }

    /**
     * Setter for the score attribute
     * @param score The score of this candidate
     */
    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Getter for the genotype
     * @return List of alleles
     */
    public Molecule[] getGenotype() {
        return genotype;
    }

    /**
     * Getter for the phenotype
     * @return The phenotype
     */
    public Molecule getPhenotype() {
        return phenotype;
    }

    Molecule[] crossover(Candidate other) {
        // Get crossover points to do uniform crossing over
        boolean[] crossoverPoints = generateCrossoverPoints();
        // Select the allele from this candidates genotype if true,
        // otherwise select hte allele from the other candidate
        // Return the new genotype
        return IntStream.range(0, genomeSize)
        .mapToObj(i -> crossoverPoints[i]? this.getGenotype()[i]:other.getGenotype()[i])
        .toArray(Molecule[]::new);
    }

    private boolean[] generateCrossoverPoints() {
        Random random = new Random();
        boolean[] arr = new boolean[genomeSize];
        for(int i = 0; i < genomeSize; i++) {
            arr[i] = random.nextBoolean();
        }
        return arr;
    }
}
