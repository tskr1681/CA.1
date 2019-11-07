package nl.bioinf.cawarmerdam.compound_evolver.util;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultiReceptorHelper {
    private MultiReceptorHelper() {}

    public static double[] getFitnessList(@NotNull List<List<Candidate>> candidates) {
        if (candidates.size() == 1) {
            // Just the same thing that was done previously, get fitness for each candidate
            return candidates.get(0).stream().mapToDouble(Candidate::getNormFitness).toArray();
        } else {
            List<Double> fitnesses = new ArrayList<>();
            for (int i = 0; i < candidates.get(0).size(); i++) {
                double fitness = 1;
                for (List<Candidate> candidate : candidates) {
                    fitness *= candidate.get(i).getNormFitness();
                }
                fitnesses.add(Math.exp(fitness-1));
            }
            return fitnesses.stream().mapToDouble(Double::doubleValue).toArray();
        }
    }

    public static List<Candidate> getCandidatesWithFitness(List<List<Candidate>> candidates) {
        double[] fitnesslist = getFitnessList(candidates);
        List<Candidate> output = new ArrayList<>();
        for(int i = 0; i < candidates.get(0).size(); i++) {
            candidates.get(0).get(i).setNormFitness(fitnesslist[i]);
            output.add(candidates.get(0).get(i));
        }
        return output;
    }
}
