package nl.bioinf.cawarmerdam.compound_evolver.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.model.Population;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GAParameters implements Serializable {
    private long maximumAllowedDuration;
    private int generationSize;
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

    public int getGenerationSize() {
        return generationSize;
    }

    public void setGenerationSize(int generationSize) {
        this.generationSize = generationSize;
    }

    public int getMaxGenerations() {
        return maxGenerations;
    }

    public void setMaxGenerations(int maxGenerations) {
        this.maxGenerations = maxGenerations;
    }

    public Double getSelectionRate() {
        return selectionRate;
    }

    public void setSelectionRate(double selectionRate) {
        this.selectionRate = selectionRate;
    }

    public Double getMutationRate() {
        return mutationRate;
    }

    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
    }

    public Double getCrossoverRate() {
        return crossoverRate;
    }

    public void setCrossoverRate(double crossoverRate) {
        this.crossoverRate = crossoverRate;
    }

    public Double getRandomImmigrantRate() {
        return randomImmigrantRate;
    }

    public void setRandomImmigrantRate(double randomImmigrantRate) {
        this.randomImmigrantRate = randomImmigrantRate;
    }

    public Double getElitistRate() {
        if (elitistRate != null) return elitistRate;
        // If this is not explicitly set get the remaining bit from 1.
        else return 1 - getCrossoverRate() - getRandomImmigrantRate();
    }

    public void setElitistRate(double elitistRate) {
        this.elitistRate = elitistRate;
    }

    public Double getNonImprovingGenerationAmountFactor() {
        return nonImprovingGenerationAmountFactor;
    }

    public void setNonImprovingGenerationAmountFactor(double nonImprovingGenerationAmountFactor) {
        this.nonImprovingGenerationAmountFactor = nonImprovingGenerationAmountFactor;
    }

    public CompoundEvolver.TerminationCondition getTerminationCondition() {
        return terminationCondition;
    }

    public void setTerminationCondition(CompoundEvolver.TerminationCondition terminationCondition) {
        this.terminationCondition = terminationCondition;
    }

    public CompoundEvolver.FitnessMeasure getFitnessMeasure() {
        return fitnessMeasure;
    }

    public void setFitnessMeasure(CompoundEvolver.FitnessMeasure fitnessMeasure) {
        this.fitnessMeasure = fitnessMeasure;
    }

    public CompoundEvolver.ForceField getForceField() {
        return forceField;
    }

    public void setForceField(CompoundEvolver.ForceField forceField) {
        this.forceField = forceField;
    }

    public Population.MutationMethod getMutationMethod() {
        return mutationMethod;
    }

    public void setMutationMethod(Population.MutationMethod mutationMethod) {
        this.mutationMethod = mutationMethod;
    }

    public Population.SelectionMethod getSelectionMethod() {
        return selectionMethod;
    }

    public void setSelectionMethod(Population.SelectionMethod selectionMethod) {
        this.selectionMethod = selectionMethod;
    }

    public long getMaximumAllowedDuration() {
        return maximumAllowedDuration;
    }

    public void setMaximumAllowedDuration(long maximumAllowedDuration) {
        this.maximumAllowedDuration = maximumAllowedDuration;
    }

    public static boolean set(Object object, String fieldName, Object fieldValue) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
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

    @Override
    public String toString() {
        return "GAParameters{" +
                "generationSize=" + generationSize +
                ", maxGenerations=" + maxGenerations +
                ", selectionRate=" + selectionRate +
                ", mutationRate=" + mutationRate +
                ", crossoverRate=" + crossoverRate +
                ", randomImmigrantRate=" + randomImmigrantRate +
                ", elitistRate=" + getElitistRate() +
                ", nonImprovingGenerationAmountFactor=" + nonImprovingGenerationAmountFactor +
                ", terminationCondition=" + terminationCondition +
                ", fitnessMeasure=" + fitnessMeasure +
                ", forceField=" + forceField +
                ", mutationMethod=" + mutationMethod +
                ", selectionMethod=" + selectionMethod +
                '}';
    }

    public Double getMaxAnchorMinimizedRmsd() {
        return maxAnchorMinimizedRmsd;
    }

    public void setMaxAnchorMinimizedRmsd(Double maxAnchorMinimizedRmsd) {
        this.maxAnchorMinimizedRmsd = maxAnchorMinimizedRmsd;
    }
}
