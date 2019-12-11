package nl.bioinf.cawarmerdam.compound_evolver.util;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultiReceptorHelper {
    private MultiReceptorHelper() {}

    /**
     * Gets a list of fitnesses for each candidate for multireceptor handling. Combines the scores as follows:
     * For each candidate, the fitness is e^(prod fitness) where prod is the product.
     * @param candidates A nested list of candidates to get a list of fitnesses for
     * @return a list of fitnesses, one for each candidate
     */
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

    /**
     * Gets a list of fitnesses for each candidate for multireceptor handling. Combines the scores as follows:
     * For each candidate, the fitness is e^(f_1) - Sum(e^f_i) for i from 2 to the total amount of candidates.
     * @param candidates A nested list of candidates to get a list of fitnesses for
     * @return a list of fitnesses, one for each candidate
     */
    public static double[] getFitnessListSelectivity(@NotNull List<List<Candidate>> candidates) {
        if (candidates.size() == 1) {
            // Just the same thing that was done previously, get fitness for each candidate
            return candidates.get(0).stream().mapToDouble(Candidate::getNormFitness).toArray();
        } else {
            List<Double> fitnesses = new ArrayList<>();
            for (int i = 0; i < candidates.get(0).size(); i++) {
                double fitness = 1;
                for (int j = 0; j < candidates.size(); j++) {
                    if (j == 0) {
                        fitness = Math.exp(candidates.get(0).get(i).getNormFitness());
                    } else {
                        fitness -= Math.exp(candidates.get(j).get(i).getNormFitness());
                    }
                }
                fitnesses.add(Math.exp(fitness-1));
            }
            return fitnesses.stream().mapToDouble(Double::doubleValue).toArray();
        }
    }

    /**
     * Gets a list of candidates with the fitnesses set according to the above methods
     * @param candidates the list of candidates to assign fitnesses to
     * @param selective should we check for selectivity or for effect on multiple receptors.
     * @return A list of candidates with updated fitnesses
     */
    public static List<Candidate> getCandidatesWithFitness(List<List<Candidate>> candidates, boolean selective) {
        double[] fitnesslist;
        if (!selective) {
            fitnesslist = getFitnessList(candidates);
        } else {
            fitnesslist = getFitnessListSelectivity(candidates);
        }
        List<Candidate> output = new ArrayList<>();
        for(int i = 0; i < candidates.get(0).size(); i++) {
            candidates.get(0).get(i).setNormFitness(fitnesslist[i]);
            output.add(candidates.get(0).get(i));
        }
        return output;
    }
}
