/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactionFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class CompoundEvolver {

    private Path pipelineOutputFileLocation;
    private ForceField forceField;
    private TerminationCondition terminationCondition;
    private int maxNumberOfGenerations;
    private Random random = new Random();
    private PipelineStep<Molecule, Double> pipe;
    private Population population;
    private Path anchor;
    private int nonImprovingGenerationAmountFactor;
    private boolean dummyFitness = true;

    public enum ForceField {MAB, MMFF94}

    public enum TerminationCondition {
        FIXED_GENERATION_NUMBER("fixed"),
        CONVERGENCE("convergence"),
        DURATION("duration");

        private final String text;

        TerminationCondition(String text) {
            this.text = text;
        }

        public static TerminationCondition fromString(String text) {
            for (TerminationCondition condition : TerminationCondition.values()) {
                if (condition.text.equalsIgnoreCase(text)) {
                    return condition;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }}

    public CompoundEvolver(Population population, Path anchor) {
        this.anchor = anchor;
        this.population = population;
        this.maxNumberOfGenerations = 5;
        this.forceField = ForceField.MAB;
        this.terminationCondition = TerminationCondition.FIXED_GENERATION_NUMBER;
    }

    public Path getPipelineOutputFileLocation() {
        return pipelineOutputFileLocation;
    }

    public void setPipelineOutputFileLocation(Path pipelineOutputFileLocation) {
        this.pipelineOutputFileLocation = pipelineOutputFileLocation;
    }

    public int getNonImprovingGenerationAmountFactor() {
        return nonImprovingGenerationAmountFactor;
    }

    public void setNonImprovingGenerationAmountFactor(int nonImprovingGenerationAmountFactor) {
        this.nonImprovingGenerationAmountFactor = nonImprovingGenerationAmountFactor;
    }

    public ForceField getForceField() {
        return forceField;
    }

    public void setForceField(ForceField forceField) {
        this.forceField = forceField;
    }

    public TerminationCondition getTerminationCondition() {
        return terminationCondition;
    }

    public void setTerminationCondition(TerminationCondition terminationCondition) {
        this.terminationCondition = terminationCondition;
    }

    public int getMaxNumberOfGenerations() {
        return maxNumberOfGenerations;
    }

    public void setMaxNumberOfGenerations(int generations) {
        this.maxNumberOfGenerations = generations;
    }

    public List<List<Double>> getPopulationFitness() {
        return this.population.getFitness();
    }

    /**
     * Evolve compounds
     */
    public void evolve() {
        for (Candidate candidate : this.population) {
            try {
                scoreCandidate(candidate);
            } catch (PipeLineError e) {
                e.printStackTrace();
            }
        }
        // Evolve
        int generationNumber = 0;
        while (!shouldTerminate(generationNumber)) {
            System.out.println(this.population.toString());
            // Produce offspring
            this.population.produceOffspring();
            // Score the candidates
            for (Candidate candidate : this.population) {
                try {
                    scoreCandidate(candidate);
                } catch (PipeLineError e) {
                    e.printStackTrace();
                }
            }
            generationNumber++;
        }
    }

    private boolean shouldTerminate(int generationNumber) {
        if (this.terminationCondition == TerminationCondition.FIXED_GENERATION_NUMBER) {
            return generationNumber == this.maxNumberOfGenerations;
        } else if (this.terminationCondition == TerminationCondition.CONVERGENCE) {
            return this.hasConverged(generationNumber);
        }
        throw new RuntimeException(
                String.format("Termination condition '%s' is not implemented", this.terminationCondition.toString()));
    }

    private boolean hasConverged(int generationNumber) {
        List<List<Double>> populationFitness = this.getPopulationFitness();
        double highestScore = 0;
        int highestScoringGenerationNumber = 0;
        for (int i = 0; i < populationFitness.size(); i++) {
            Double max = Collections.max(populationFitness.get(i));
            if (max > highestScore) {
                highestScore = max;
                highestScoringGenerationNumber = i;
            }
        }
        return highestScoringGenerationNumber * this.nonImprovingGenerationAmountFactor + highestScoringGenerationNumber > generationNumber;
    }

    /**
     * Score set the fitness score of each candidate
     *
     * @param candidate Candidate instance
     * @throws PipeLineError if an error occurred in the pipeline
     */
    private void scoreCandidate(Candidate candidate) throws PipeLineError {
        // Execute pipeline
        double score = 0;
        if (!dummyFitness) {
            if (pipe == null) throw new RuntimeException("pipeline setup not complete!");
            score = -pipe.execute(candidate.getPhenotype());
        } else {
            score = candidate.getPhenotype().getExactMass();
        }
//        System.out.println("score = " + score);
        // Assign score to candidate
//        candidate.setScore(random.nextDouble());
        candidate.setScore(score);
    }

    /**
     * Setup the pipeline for scoring candidates
     *
     * @return Pipeline that can be executed
     */
    public void setupPipeline(Path outputFileLocation) {
        this.setPipelineOutputFileLocation(outputFileLocation);
        ThreeDimensionalConverterStep threeDimensionalConverterStep = new ThreeDimensionalConverterStep(
                this.pipelineOutputFileLocation);
        ConformerFixationStep conformerFixationStep = new ConformerFixationStep(anchor, "obfit.exe");
        EnergyMinimizationStep energyMinimizationStep = getEnergyMinimizationStep();
        PipelineStep<Molecule, Path> converterStep = threeDimensionalConverterStep.pipe(conformerFixationStep);
        this.pipe = converterStep.pipe(energyMinimizationStep);
    }

    private EnergyMinimizationStep getEnergyMinimizationStep() {
        if (this.forceField == ForceField.MAB) {
            return new MolocEnergyMinimizationStep(
                    "",
                    "X:\\Internship\\receptor\\rec.mab",
                    "C:\\Program Files (x86)\\moloc\\bin\\Mol3d.exe");
        } else if (this.forceField == ForceField.MMFF94) {
            return new VinaEnergyMinimizationStep(
                    "",
                    Paths.get("X:\\Internship\\receptor\\rec.pdbqt"),
                    "C:\\Program Files (x86)\\The Scripps Research Institute\\Vina\\vina.exe");
        } else {
            throw new RuntimeException(String.format("Force field '%s' is not implemented", this.forceField.toString()));
        }
    }

    /**
     * main for cli
     *
     * @param args An array of strings being the command line input
     */
    public static void main(String[] args) throws Exception {
        // Load reactor from argument
        Reactor reactor = ReactionFileHandler.loadReaction(args[0]);
        // Get maximum amount of product
        int maxSamples = Integer.parseInt(args[args.length - 1]);
        // Load molecules
        String[] reactantFiles = Arrays.copyOfRange(args, 1, args.length - 2);
        List<List<Molecule>> reactantLists = ReactantFileHandler.loadMolecules(reactantFiles);
        // Load anchor molecule
        Path anchor = Paths.get(args[args.length - 2]);
        // Construct the initial population
        Population population = new Population(reactantLists, reactor, maxSamples);
        population.initializeAlleleSimilaritiesMatrix();
        population.setMutationMethod(Population.MutationMethod.DISTANCE_DEPENDENT);
        population.setSelectionMethod(Population.SelectionMethod.TRUNCATED_SELECTION);
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, anchor);
        compoundEvolver.setupPipeline(Paths.get("X:\\uploads"));
        // Evolve compounds
        compoundEvolver.evolve();
    }
}
