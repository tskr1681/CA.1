/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.model.Population;

import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * Class that holds the main parameters for the genetic algorithm.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GAParameters implements Serializable {
    private long maximumAllowedDuration;
    private int populationSize;
    private int maxGenerations;
    private Double selectionRate;
    private Double mutationRate;
    private Double crossoverRate;
    private Double randomImmigrantRate;
    private Double elitistRate;
    private Double nonImprovingGenerationAmountFactor;
    private Double maxAnchorMinimizedRmsd;
    private CompoundEvolver.TerminationCondition terminationCondition;
    private CompoundEvolver.FitnessMeasure fitnessMeasure;
    private CompoundEvolver.ForceField forceField;
    private Population.MutationMethod mutationMethod;
    private Population.SelectionMethod selectionMethod;
    private Population.InterspeciesCrossoverMethod interspeciesCrossoverMethod;
    private Population.SpeciesDeterminationMethod speciesDeterminationMethod;
    private int targetCandidateCount;

    /**
     * Getter for the population size.
     *
     * @return the population size.
     */
    public int getPopulationSize() {
        return populationSize;
    }

    /**
     * Setter for the population size.
     *
     * @param populationSize The size to set the amount of individuals to in newer generations.
     */
    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    /**
     * Getter for the maximum number of generations in the evolution process.
     *
     * @return the maximum number of generations in the evolution process.
     */
    public int getMaxGenerations() {
        return maxGenerations;
    }

    /**
     * Setter for the maximum number of generations in the evolution process.
     *
     * @param maxGenerations The maximum number of generations in the evolution process.
     */
    public void setMaxGenerations(int maxGenerations) {
        this.maxGenerations = maxGenerations;
    }

    /**
     * Getter for the selection fraction.
     *
     * @return fraction off population that will be selected.
     */
    public Double getSelectionRate() {
        return selectionRate;
    }

    /**
     * Setter for the selection fraction.
     *
     * @param selectionRate The fraction off the population that will be selected.
     */
    public void setSelectionRate(double selectionRate) {
        this.selectionRate = selectionRate;
    }

    /**
     * Getter for the mutation rate.
     *
     * @return the mutation rate.
     */
    public Double getMutationRate() {
        return mutationRate;
    }

    /**
     * Setter for the mutation rate.
     *
     * @param mutationRate The rate at which to introduce new mutations in a gene.
     */
    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
    }

    /**
     * Getter for the crossover rate. This is the probability that crossover will be performed in two parents or
     * if new offspring will be generated by other means.
     *
     * @return the crossover rate.
     */
    public Double getCrossoverRate() {
        return crossoverRate;
    }

    /**
     * Setter for te crossover rate. This is the probability that crossover will be performed in two parents or
     * if new offspring will be generated by other means.
     *
     * @param crossoverRate The probability that crossover will be performed
     */
    public void setCrossoverRate(double crossoverRate) {
        this.crossoverRate = crossoverRate;
    }

    /**
     * Getter for the random immigrant rate. The random immigrant rate should be seen in relation to the
     * elitism rate and the crossover rate. When creating offspring a candidate solution is either produced
     * as a random immigrant, as the crossover product of two parents or by directly copying a selected candidate.
     *
     * @return the random immigrant rate
     */
    public Double getRandomImmigrantRate() {
        return randomImmigrantRate;
    }

    /**
     * Setter for the random immigrant rate. The random immigrant rate should be seen in relation to the
     * elitism rate and the crossover rate. When creating offspring a candidate solution is either produced
     * as a random immigrant, as the crossover product of two parents or by directly copying a selected candidate.
     *
     * @param randomImmigrantRate The weight of selecting a random immigrant as offspring.
     */
    public void setRandomImmigrantRate(double randomImmigrantRate) {
        this.randomImmigrantRate = randomImmigrantRate;
    }

    /**
     * Getter for the rate at which the elitism concept is chosen for offspring. The elitism rate should be seen
     * in relation to the random immigrant rate and the crossover rate. The rates act as weights for choosing
     * the method for getting a new individual.
     *
     * @return the elitist rate.
     */
    public Double getElitistRate() {
        if (elitistRate != null) return elitistRate;
        // If this is not explicitly set get the remaining bit from 1.
        else return 1 - getCrossoverRate() - getRandomImmigrantRate();
    }

    /**
     * Setter for the rate at which the elitism concept is chosen for offspring. The elitism rate should be seen
     * in relation to the random immigrant rate and the crossover rate. The rates act as weights for choosing
     * the method for getting a new individual.
     *
     * @param elitistRate The weight that the elitism concept has in choosing an offspring production method for
     *                    an individual.
     */
    public void setElitistRate(double elitistRate) {
        this.elitistRate = elitistRate;
    }

    /**
     * Getter for the factor that the generation amount is multiplied with to get the generation number that
     * up to which no improvement may be present to force termination.
     *
     * @return the generation multiplication factor that is used to determine the amount of generations that
     * may show no improvement before termination is forced.
     */
    public Double getNonImprovingGenerationAmountFactor() {
        return nonImprovingGenerationAmountFactor;
    }

    /**
     * Setter for the factor that the generation amount is multiplied with to get the generation number that
     * up to which no improvement may be present to force termination.
     *
     * @param nonImprovingGenerationAmountFactor, the generation multiplication factor that is used to determine
     *                                            the amount of generations that may show no improvement before
     *                                            termination is forced.
     */
    public void setNonImprovingGenerationAmountFactor(double nonImprovingGenerationAmountFactor) {
        this.nonImprovingGenerationAmountFactor = nonImprovingGenerationAmountFactor;
    }

    /**
     * Getter for the termination condition.
     *
     * @return the termination condition.
     */
    public CompoundEvolver.TerminationCondition getTerminationCondition() {
        return terminationCondition;
    }

    /**
     * Setter for the termination condition.
     *
     * @param terminationCondition The fitness measure.
     */
    public void setTerminationCondition(CompoundEvolver.TerminationCondition terminationCondition) {
        this.terminationCondition = terminationCondition;
    }

    /**
     * Getter for the fitness measure that is used.
     *
     * @return the fitness measure.
     */
    public CompoundEvolver.FitnessMeasure getFitnessMeasure() {
        return fitnessMeasure;
    }

    /**
     * Setter for the fitness measure that is used.
     *
     * @param fitnessMeasure The fitness measure.
     */
    public void setFitnessMeasure(CompoundEvolver.FitnessMeasure fitnessMeasure) {
        this.fitnessMeasure = fitnessMeasure;
    }

    /**
     * Getter for the force field that is used in scoring and minimization.
     *
     * @return the force field.
     */
    public CompoundEvolver.ForceField getForceField() {
        return forceField;
    }

    /**
     * Setter for the force field that is used in scoring and minimization.
     *
     * @param forceField The force field.
     */
    public void setForceField(CompoundEvolver.ForceField forceField) {
        this.forceField = forceField;
    }

    /**
     * Getter for the mutation method.
     *
     * @return the method that is set to introduce mutations.
     */
    public Population.MutationMethod getMutationMethod() {
        return mutationMethod;
    }

    /**
     * Setter for the mutation method.
     *
     * @param mutationMethod For use in introducing mutations.
     */
    public void setMutationMethod(Population.MutationMethod mutationMethod) {
        this.mutationMethod = mutationMethod;
    }

    /**
     * Getter for the selection method.
     *
     * @return the method that is set to select new offspring.
     */
    public Population.SelectionMethod getSelectionMethod() {
        return selectionMethod;
    }

    /**
     * Setter for selection method.
     *
     * @param selectionMethod For use in selecting new offspring.
     */
    public void setSelectionMethod(Population.SelectionMethod selectionMethod) {
        this.selectionMethod = selectionMethod;
    }

    /**
     * Getter for the maximum allowed duration of the evolution procedure. No new generation is made when this
     * duration is surpassed by actual duration.
     *
     * @return the maximum allowed duration of the evolution procedure.
     */
    public long getMaximumAllowedDuration() {
        return maximumAllowedDuration;
    }

    /**
     * Setter for the maximum allowed duration of the evolution procedure. No new generation is made when this
     * duration is surpassed by actual duration.
     *
     * @param maximumAllowedDuration The maximum allowed duration of the evolution procedure.
     */
    public void setMaximumAllowedDuration(long maximumAllowedDuration) {
        this.maximumAllowedDuration = maximumAllowedDuration;
    }

    /**
     * Method that sets a specified field by using reflection.
     *
     * @param object The parameters object to write the field value to.
     * @param fieldName The name of the field that should be set.
     * @param fieldValue The value of the field that should be set.
     * @return true if the field was set, false if not.
     */
    public static boolean set(Object object, String fieldName, Object fieldValue) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                if (field.getType().equals(Population.MutationMethod.class)) {
                    fieldValue = Population.MutationMethod.valueOf((String) fieldValue);
                } else if (field.getType().equals(Population.SelectionMethod.class)) {
                    fieldValue = Population.SelectionMethod.valueOf((String) fieldValue);
                }
                field.set(object, fieldValue);
                return true;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return false;
    }

    /**
     * Getter for the maximum allowed rmsd between the anchor and
     * its matching substructure in the candidates best conformer.
     *
     * @return the maximum allowed rmsd
     */
    public Double getMaxAnchorMinimizedRmsd() {
        return maxAnchorMinimizedRmsd;
    }

    /**
     * Setter for the maximum allowed rmsd between the anchor and
     * its matching substructure in the candidates best conformer.
     *
     * @param maxAnchorMinimizedRmsd, the maximum allowed rmsd
     */
    public void setMaxAnchorMinimizedRmsd(Double maxAnchorMinimizedRmsd) {
        this.maxAnchorMinimizedRmsd = maxAnchorMinimizedRmsd;
    }

    /**
     * Getter for the interspecies crossover method.
     *
     * @return the interspecies crossover method.
     */
    public Population.InterspeciesCrossoverMethod getInterspeciesCrossoverMethod() {
        return interspeciesCrossoverMethod;
    }

    /**
     * Setter for the interspecies crossover method.
     *
     * @param interspeciesCrossoverMethod the interspecies crossover method.
     */
    public void setInterspeciesCrossoverMethod(Population.InterspeciesCrossoverMethod interspeciesCrossoverMethod) {
        this.interspeciesCrossoverMethod = interspeciesCrossoverMethod;
    }

    /**
     * Getter for the species determination method.
     *
     * @return the species determination method.
     */
    public Population.SpeciesDeterminationMethod getSpeciesDeterminationMethod() {
        return speciesDeterminationMethod;
    }

    /**
     * Setter for the species determination method.
     *
     * @param speciesDeterminationMethod The species determination method.
     */
    public void setSpeciesDeterminationMethod(Population.SpeciesDeterminationMethod speciesDeterminationMethod) {
        this.speciesDeterminationMethod = speciesDeterminationMethod;
    }

    /**
     * Setter for the amount of candidates that is sampled during the entire evolution process.
     *
     * @param targetCandidateCount The amount of candidates that is sampled.
     */
    public void setTargetCandidateCount(int targetCandidateCount) {
        this.targetCandidateCount = targetCandidateCount;
    }

    /**
     * Getter for the amount of candidates that is sampled during the entire evolution process.
     *
     * @return the amount of candidates that is sampled.
     */
    public int getTargetCandidateCount() {
        return targetCandidateCount;
    }
}
