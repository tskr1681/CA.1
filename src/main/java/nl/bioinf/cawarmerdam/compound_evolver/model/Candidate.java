/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.Molecule;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Candidate {

    private Molecule[] genotype;
    private Molecule phenotype;
    private double score;

    Candidate(Molecule[] genotype, Molecule phenotype) {
        this.genotype = genotype;
        this.phenotype = phenotype;
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
}
