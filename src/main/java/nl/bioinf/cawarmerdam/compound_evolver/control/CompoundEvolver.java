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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

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
    private PipelineStep<Candidate, Double> pipe;
    private Population population;
    private EvolutionProgressConnector evolutionProgressConnector;
    private double nonImprovingGenerationAmountFactor;

    public boolean isDummyFitness() {
        return dummyFitness;
    }

    private boolean dummyFitness;

    public void setDummyFitness(boolean dummyFitness) {
        this.dummyFitness = dummyFitness;
    }

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

    public CompoundEvolver(Population population, EvolutionProgressConnector evolutionProgressConnector) {
        this.population = population;
        this.evolutionProgressConnector = evolutionProgressConnector;
        this.maxNumberOfGenerations = 5;
        this.forceField = ForceField.MAB;
        this.terminationCondition = TerminationCondition.FIXED_GENERATION_NUMBER;
    }

    public Path getPipelineOutputFileLocation() {
        return pipelineOutputFileLocation;
    }

    public void setPipelineOutputFileLocation(Path pipelineOutputFileLocation) {
        if (!pipelineOutputFileLocation.toFile().exists()){
            boolean mkdir = pipelineOutputFileLocation.toFile().mkdir();
            if (!mkdir) {
                throw new PipeLineException(String.format("Could not create directory '%s'",
                        pipelineOutputFileLocation.toString()));
            }
        }
        this.pipelineOutputFileLocation = pipelineOutputFileLocation;
    }

    public double getNonImprovingGenerationAmountFactor() {
        return nonImprovingGenerationAmountFactor;
    }

    public void setNonImprovingGenerationAmountFactor(double nonImprovingGenerationAmountFactor) {
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
        scoreCandidates();
        evolutionProgressConnector.handleNewGeneration(population.getCurrentGeneration());
        // Evolve
        while (!shouldTerminate()) {
            System.out.println(this.population.toString());
            // Produce offspring
            this.population.produceOffspring();
            // Score the candidates
            scoreCandidates();
            evolutionProgressConnector.handleNewGeneration(population.getCurrentGeneration());
        }
    }

    private void scoreCandidates() {
        if (!dummyFitness) {
            // Check if pipe is present
            if (pipe == null) throw new RuntimeException("pipeline setup not complete!");
            // Get executorService with thread pool size 4
            ExecutorService executor = Executors.newFixedThreadPool(4);
            // Create list to hold future object associated with Callable
            List<Future<Void>> futures = new ArrayList<>();
            // Loop through candidates to produce and submit new tasks
            for (Candidate candidate : this.population) {
                // Setup callable
                Callable<Void> pipelineExecutor = new PipelineTask(pipe, candidate);
                // Add future, which the executor will return to the list
                futures.add(executor.submit(pipelineExecutor));
            }
            // Loop through futures to handle thrown exceptions
            for(Future<Void> future : futures){
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    // Handle exception
                    e.printStackTrace();
                }
            }
            executor.shutdown();
            System.out.println("Finished all threads");
        } else {
            for (Candidate candidate : this.population) {
                // Set dummy fitness
                double score = candidate.getPhenotype().getExactMass();
                candidate.setScore(score);
            }
        }
    }

    private boolean shouldTerminate() {
        int generationNumber = this.population.getGenerationNumber();
        if (this.terminationCondition == TerminationCondition.CONVERGENCE && generationNumber > 5) {
            return this.hasConverged(generationNumber);
        }
        return generationNumber == this.maxNumberOfGenerations || evolutionProgressConnector.isTerminationRequired();
    }

    private boolean hasConverged(int generationNumber) {
        // Get all fitness scores so far
        List<List<Double>> populationFitness = this.getPopulationFitness();

        // Initialize highestScore and it's generation number
        double highestScore = 0;
        int highestScoringGenerationNumber = 0;

        // Initialize maximum of the generation.
        double max = 0;
        for (int i = 0; i < populationFitness.size(); i++) {
            // Get maximum of the generation
            max = Collections.max(populationFitness.get(i));

            // If maximum of the generation is larger than the highest score overall, set new highest score
            if (max > highestScore) {
                highestScore = max;
                highestScoringGenerationNumber = i;
            }
        }
        System.out.printf("highscore: %.2f at Ngen %d, curr: %.2f --- ", highestScore, highestScoringGenerationNumber, max);
        double nonImprovingGenerationNumber = highestScoringGenerationNumber * this.nonImprovingGenerationAmountFactor + highestScoringGenerationNumber;
        System.out.printf("%f < %d = %s%n", nonImprovingGenerationNumber, generationNumber, nonImprovingGenerationNumber < generationNumber);
        return nonImprovingGenerationNumber < generationNumber;
    }

    private void setupPipeline(Path outputFileLocation) {
        Path anchor = Paths.get("X:\\Internship\\reference_fragment\\anchor.sdf");
        Path receptor = Paths.get("X:\\Internship\\receptor\\rec.mab");
        setupPipeline(outputFileLocation, receptor, anchor);
    }

    /**
     * Setup the pipeline for scoring candidates
     */
    public void setupPipeline(Path outputFileLocation, Path receptorFile, Path anchor) {
        this.setPipelineOutputFileLocation(outputFileLocation);
        ThreeDimensionalConverterStep threeDimensionalConverterStep = new ThreeDimensionalConverterStep(
                this.pipelineOutputFileLocation);
        ConformerFixationStep conformerFixationStep = new ConformerFixationStep(anchor, "obfit.exe");
        EnergyMinimizationStep energyMinimizationStep = getEnergyMinimizationStep(receptorFile);
        PipelineStep<Candidate, Path> converterStep = threeDimensionalConverterStep.pipe(conformerFixationStep);
        this.pipe = converterStep.pipe(energyMinimizationStep);
    }

    private EnergyMinimizationStep getEnergyMinimizationStep(Path receptorFile) {
        if (this.forceField == ForceField.MAB) {
            return new MolocEnergyMinimizationStep(
                    "",
                    receptorFile,
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
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, new CommandLineEvolutionProgressConnector());
        compoundEvolver.setupPipeline(Paths.get("C:\\Users\\P286514\\uploads"));
        compoundEvolver.setDummyFitness(false);
        // Evolve compounds
        compoundEvolver.evolve();
    }
}
