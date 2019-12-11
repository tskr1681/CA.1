package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.util.Random;
import java.util.stream.Collectors;

public class FuzzyScoreStep implements PipelineStep<Candidate, Candidate> {
    private final Random rand;

    public FuzzyScoreStep() {
        rand = new Random();
    }

    @Override
    public Candidate execute(Candidate candidate) {
        double fuzzyfactor = rand.nextDouble();
        candidate.setConformerScores(candidate.getConformerScores().stream().map(d -> d*fuzzyfactor).collect(Collectors.toList()));
        EnumColor color = EnumColor.RED;
        for (EnumColor c:EnumColor.values()) {
            if (fuzzyfactor >= c.MIN && fuzzyfactor <= c.MAX) {
                color = c;
                break;
            }
        }
        candidate.setColor(color);
        return candidate;
    }
}
