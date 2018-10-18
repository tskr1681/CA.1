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

    private int setNumberOfGenerations;
    private Random random = new Random();
    private PipelineStep<Molecule, Double> pipe;
    private Population population;
    private File anchor;

    public enum ForceField {MAB, MMFF94}

    public enum TerminationCondition {FIXED_GENERATION_NUMBER, CONVERGENCE, DURATION}

    public CompoundEvolver(Population population, File anchor) {
        this.anchor = anchor;
        this.population = population;
        this.setNumberOfGenerations = 20;
        // Setup the pipeline
        this.pipe = setupPipeline();
    }

    public int getSetNumberOfGenerations() {
        return setNumberOfGenerations;
    }

    public void setNumberOfGenerations(int generations) {
        this.setNumberOfGenerations = generations;
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
        for (int i = 0; i < this.setNumberOfGenerations; i++) {
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
        }
    }

    /**
     * Score set the fitness score of each candidate
     * @param candidate Candidate instance
     * @throws PipeLineError if an error occurred in the pipeline
     */
    private void scoreCandidate(Candidate candidate) throws PipeLineError {
        // Execute pipeline
//        double score = -pipe.execute(candidate.getPhenotype());
        double score = candidate.getPhenotype().getExactMass();
//        System.out.println("score = " + score);
        // Assign score to candidate
//        candidate.setScore(random.nextDouble());
        candidate.setScore(score);
    }

    /**
     * Setup the pipeline for scoring candidates
     * @return Pipeline that can be executed
     */
    private PipelineStep<Molecule, Double> setupPipeline() {
        ThreeDimensionalConverterStep threeDimensionalConverterStep = new ThreeDimensionalConverterStep(
                Paths.get("X:\\uploads"));
        ConformerFixationStep conformerFixationStep = new ConformerFixationStep(anchor, "obfit.exe");
        MolocEnergyMinimizationStep energyMinimizationStep = getEnergyMinimizationStep();
        PipelineStep<Molecule, Path> converterStep = threeDimensionalConverterStep.pipe(conformerFixationStep);
        PipelineStep<Molecule, Double> pipe = converterStep.pipe(energyMinimizationStep);
        return pipe;
    }

    private MolocEnergyMinimizationStep getEnergyMinimizationStep() {
        if (true) {
            return new MolocEnergyMinimizationStep(
                    "",
                    "X:\\Internship\\receptor\\rec.mab",
                    "C:\\Program Files (x86)\\moloc\\bin\\Mol3d.exe");
        }
        return null;
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
        File anchor = new File(args[args.length - 2]);
        // Construct the initial population
        Population population = new Population(reactantLists, reactor, maxSamples);
        population.computeAlleleSimilarities();
        population.setMutationMethod(Population.MutationMethod.DISTANCE_DEPENDENT);
        population.setSelectionMethod(Population.SelectionMethod.TRUNCATED_SELECTION);
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, anchor);
        // Evolve compounds
        compoundEvolver.evolve();
    }
}
