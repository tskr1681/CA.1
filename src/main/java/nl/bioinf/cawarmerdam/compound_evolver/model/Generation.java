package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Generation {
    private List<Candidate> candidateList;

    Generation(List<Candidate> candidateList) {
        this.candidateList = candidateList;
    }

    public List<Candidate> getCandidateList() {
        return candidateList;
    }

    public double getFittestCandidate() {
        Optional<Double> max = candidateList.stream().map(Candidate::getScore).max(Double::compare);
        if (max.isPresent()) {
            return max.get();
        }
        throw new RuntimeException("max not present?");
    }
}
