/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;
import java.util.Optional;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Generation {
    private List<Candidate> candidateList;
    private int number;

    Generation(List<Candidate> candidateList, int number) {
        this.number = number;
        this.candidateList = candidateList;
    }

    public List<Candidate> getCandidateList() {
        return candidateList;
    }

    public double getFittestCandidate() {
        Optional<Double> max = candidateList.stream().map(Candidate::getRawScore).max(Double::compare);
        if (max.isPresent()) {
            return max.get();
        }
        throw new RuntimeException("max not present?");
    }

    public int getNumber() {
        return number;
    }
}
