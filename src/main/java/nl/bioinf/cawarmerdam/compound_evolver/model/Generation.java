/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;
import java.util.Optional;

/**
 * A class that holds a list of candidates and the generation number.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Generation {
    private List<Candidate> candidateList;
    private int number;

    /**
     * Constructor for a generation instance
     *
     * @param candidateList The list of candidates that comprise this generation.
     * @param number The generation number of this generation.
     */
    Generation(List<Candidate> candidateList, int number) {
        this.number = number;
        this.candidateList = candidateList;
    }

    /**
     * Getter for the candidates in this generation.
     *
     * @return a list of candidates.
     */
    public List<Candidate> getCandidateList() {
        return candidateList;
    }

    /**
     * Getter for the fittest candidate in this generation.
     *
     * @return the fittest candidate in this generation
     */
    public double getFittestCandidate() {
        Optional<Double> max = candidateList.stream().map(Candidate::getFitness).max(Double::compare);
        if (max.isPresent()) {
            return max.get();
        }
        throw new RuntimeException("max not present?");
    }

    /**
     * Getter for the number of this generation, aka the iteration of evolution.
     *
     * @return the number of this generation.
     */
    public int getNumber() {
        return number;
    }
}
