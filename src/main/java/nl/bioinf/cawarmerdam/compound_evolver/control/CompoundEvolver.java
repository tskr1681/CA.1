/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.formats.MolImporter;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
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
    private PipelineStep<Molecule, Path> pipe;
    private Population population;
    private File anchor;

    private CompoundEvolver(List<List<Molecule>> reactantLists, Reactor reactor, File anchor, int maxProducts) {
        this.anchor = anchor;
        // Construct the initial population
        List<Candidate> candidateList = new RandomCompoundReactor(reactor, maxProducts).execute(reactantLists);
        this.population = new Population(candidateList);
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
        for (int i = 0; i < 1; i++) {
            // Perform crossing over in population
            this.population.crossover();
            // Mutate part of population
            this.population.mutate();
            // Possibly introduce new individuals
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
        Path filename = pipe.execute(candidate.getPhenotype());
        // Assign score to candidate
        candidate.setScore(0.5);
    }

    /**
     * Setup the pipeline for scoring candidates
     * @return Pipeline that can be executed
     */
    private PipelineStep<Molecule, Path> setupPipeline() {
        PipelineStep<Molecule, Path> converter = new ThreeDimensionalConverter(Paths.get("..\\uploads"));
        PipelineStep<Molecule, Path> pipe = converter.pipe(new ConformerFixationStep(
                anchor,
                "obfit.exe"));
        return pipe;
    }

    /**
     * main for cli
     *
     * @param args An array of strings being the command line input
     */
    public static void main(String[] args) throws Exception {
        // Load reactor from argument
        Reactor reactor = CompoundEvolver.loadReaction(args[0]);
        // Get maximum amount of product
        int maxSamples = Integer.parseInt(args[args.length - 1]);
        // Load molecules
        String[] reactantFiles = Arrays.copyOfRange(args, 1, args.length - 2);
        List<List<Molecule>> reactantLists = CompoundEvolver.loadMolecules(reactantFiles);
        // Load anchor molecule
        File anchor = new File(args[args.length - 2]);
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(reactantLists, reactor, anchor, maxSamples);
        // Evolve compounds
        compoundEvolver.evolve();
    }

    private static Reactor loadReaction(String filename) throws Exception {
        Reactor reactor = new Reactor();
        try {
            MolImporter importer = new MolImporter(filename);
            reactor.setReaction(importer.read());
            importer.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return reactor;
    }

    private static List<List<Molecule>> loadMolecules(String[] filenames) throws Exception {
        List<List<Molecule>> reactantLists = new ArrayList<>();
        for (String i : filenames) {
            List<Molecule> moleculeMap = new ArrayList<>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(i));
                for (String line; (line = br.readLine()) != null; ) {
                    try {
                        moleculeMap.add(MolImporter.importMol(line));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
                reactantLists.add(moleculeMap);
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
        return reactantLists;
    }
}
