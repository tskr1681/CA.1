/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.formats.MolImporter;
import chemaxon.reaction.Reaction;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactionFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class CompoundEvolver {
    private Random random = new Random();
    private PipelineStep<Molecule, Path> pipe;
    private Population population;
    private File anchor;

    public CompoundEvolver(Population population, File anchor) {
        this.anchor = anchor;
        this.population = population;
        this.population.setSelectionMethod(Population.SelectionMethod.CLEAR);
        // Setup the pipeline
        this.pipe = setupPipeline();
    }

    /**
     * Evolve compounds
     */
    private void evolve() {
        for (Candidate candidate : this.population) {
            try {
                scoreCandidates(candidate);
            } catch (PipeLineError e) {
                e.printStackTrace();
            }
        }
        // Evolve
        for (int i = 0; i < 6; i++) {
            // Produce offspring
            this.population.produceOffspring();
            // Score the candidates
            for (Candidate candidate : this.population) {
                try {
                    scoreCandidates(candidate);
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
    private void scoreCandidates(Candidate candidate) throws PipeLineError {
        // Execute pipeline
//        Path filename = pipe.execute(candidate.getPhenotype());
        // Assign score to candidate
        candidate.setScore(random.nextDouble());
    }

    /**
     * Setup the pipeline for scoring candidates
     * @return Pipeline that can be executed
     */
    private PipelineStep<Molecule, Path> setupPipeline() {
        ThreeDimensionalConverterStep threeDimensionalConverterStep = new ThreeDimensionalConverterStep(
                Paths.get("..\\uploads"));
        ConformerFixationStep conformerFixationStep = new ConformerFixationStep(anchor, "obfit.exe");
        MolocEnergyMinimizationStep molocEnergyMinimizationStep = new MolocEnergyMinimizationStep("", "", "Mol3d.exe");
        PipelineStep<Molecule, Path> converterStep = threeDimensionalConverterStep.pipe(conformerFixationStep);
        PipelineStep<Molecule, Path> pipe = converterStep.pipe(molocEnergyMinimizationStep);
        return pipe;
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
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, anchor);
        // Evolve compounds
        compoundEvolver.evolve();
    }
}
