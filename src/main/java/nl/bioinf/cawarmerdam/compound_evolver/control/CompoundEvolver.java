/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.marvin.plugin.PluginException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactionFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;
import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class CompoundEvolver {

    private List<List<Double>> scores = new ArrayList<>();
    private Path pipelineOutputFilePath;
    private ForceField forceField;
    private TerminationCondition terminationCondition;
    private PipelineStep<Candidate, Void> pipe;
    private Population population;
    private EvolutionProgressConnector evolutionProgressConnector;
    private FitnessMeasure fitnessMeasure;
    private long startTime;
    private long duration;
    private long maximumAllowedDuration;
    private int maxNumberOfGenerations;
    private double nonImprovingGenerationAmountFactor;
    private boolean dummyFitness;
    private boolean cleanupFiles;

    public CompoundEvolver(Population population, EvolutionProgressConnector evolutionProgressConnector) {
        this.population = population;
        this.evolutionProgressConnector = evolutionProgressConnector;
        this.maxNumberOfGenerations = 25;
        this.maximumAllowedDuration = 600000;
        this.forceField = ForceField.MAB;
        this.setCleanupFiles(false);
        this.terminationCondition = TerminationCondition.FIXED_GENERATION_NUMBER;
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
        ArrayList<Reactor> reactions = new ArrayList<>();
        reactions.add(reactor);
        List<Species> species = Species.constructSpecies(reactions, reactantLists.size());
        Population population = new Population(reactantLists, species, maxSamples);
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

    /**
     * Getter for the duration of the evolution procedure in ms.
     *
     * @return the duration of the evolution procudure in ms.
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Getter for the fitness of candidates in every generationNumber so far.
     *
     * @return a list of lists of fitness scores
     */
    public List<List<Double>> getFitness() {
        return scores;
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
     * Getter for the maximum allowed duration of the evolution procedure. No new generation is made when this
     * duration is surpassed by actual duration.
     *
     * @param maximumAllowedDuration, the maximum allowed duration of the evolution procedure.
     */
    public void setMaximumAllowedDuration(long maximumAllowedDuration) {
        this.maximumAllowedDuration = maximumAllowedDuration;
    }

    /**
     * Getter for the dummy fitness setting.
     *
     * @return true if the dummy fitness will be applied. (the molecular mass is the raw score)
     */
    public boolean isDummyFitness() {
        return dummyFitness;
    }

    /**
     * Setter for the dummy fitness setting.
     *
     * @param dummyFitness, true if the dummy fitness should be applied. (the molecular mass is the raw score)
     */
    public void setDummyFitness(boolean dummyFitness) {
        this.dummyFitness = dummyFitness;
    }

    /**
     * Getter for the output file path for the pipeline.
     *
     * @return the output file path for the pipeline.
     */
    public Path getPipelineOutputFilePath() {
        return pipelineOutputFilePath;
    }

    /**
     * Setter for the output file path for the pipeline.
     *
     * @param pipelineOutputFilePath the output file path for the pipeline.
     * @throws PipelineException if the output file path does not exist and cannot be created.
     */
    private void setPipelineOutputFilePath(Path pipelineOutputFilePath) throws PipelineException {
        if (!pipelineOutputFilePath.toFile().exists()) {
            // Try to create the file output location
            try {
                Files.createDirectory(pipelineOutputFilePath);
            } catch (IOException e) {

                // Format exception method
                String exceptionMessage = String.format("Could not create directory '%s'",
                        pipelineOutputFilePath.toString());
                // Throw pipeline exception
                throw new PipelineException(
                        exceptionMessage, e);
            }
        }
        this.pipelineOutputFilePath = pipelineOutputFilePath;
    }

    /**
     * Getter for the factor that the generation amount is multiplied with to get the generation number that
     * up to which no improvement may be present to force termination.
     *
     * @return the generation multiplication factor that is used to determine the amount of generations that
     * may show no improvement before termination is forced.
     */
    public double getNonImprovingGenerationAmountFactor() {
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
     * Getter for the force field that is used in scoring and minimization.
     *
     * @return the force field.
     */
    public ForceField getForceField() {
        return forceField;
    }

    /**
     * Setter for the force field that is used in scoring and minimization.
     *
     * @param forceField, the force field.
     */
    public void setForceField(ForceField forceField) {
        this.forceField = forceField;
    }

    /**
     * Getter for the fitness measure that is used.
     *
     * @return the fitness measure.
     */
    public FitnessMeasure getFitnessMeasure() {
        return fitnessMeasure;
    }

    /**
     * Setter for the fitness measure that is used.
     *
     * @param fitnessMeasure, the fitness measure.
     */
    public void setFitnessMeasure(FitnessMeasure fitnessMeasure) {
        this.fitnessMeasure = fitnessMeasure;
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

    /**
     * Evolve compounds
     */
    public void evolve() throws OffspringFailureOverflow, UnSelectablePopulationException {
        // Set startTime and signal that the evolution procedure has started
        startTime = System.currentTimeMillis();
        evolutionProgressConnector.setStatus(EvolutionProgressConnector.Status.RUNNING);
        scoreCandidates();
        evolutionProgressConnector.handleNewGeneration(population.getCurrentGeneration());
        updateDuration();
        try {
            // Evolve
            while (!shouldTerminate()) {

                System.out.println(this.population.toString());
                // Try to produce offspring

                this.population.produceOffspring();
                // Score the candidates
                scoreCandidates();
                evolutionProgressConnector.handleNewGeneration(population.getCurrentGeneration());
                updateDuration();
            }
            evolutionProgressConnector.setStatus(EvolutionProgressConnector.Status.SUCCESS);
        } catch (OffspringFailureOverflow | UnSelectablePopulationException e) {
            evolutionProgressConnector.setStatus(EvolutionProgressConnector.Status.FAILED);
            throw e;
        }
    }

    private void updateDuration() {
        long endTime = System.currentTimeMillis();
        this.duration = endTime - startTime;
    }

    private void scoreCandidates() {
        if (!dummyFitness) {
            // Check if pipe is present
            if (pipe == null) throw new RuntimeException("pipeline setup not complete!");
            // Get executorService with thread pool size 4
            ExecutorService executor = Executors.newFixedThreadPool(25);
            // Create list to hold future object associated with Callable
            List<Future<Void>> futures = new ArrayList<>();
            // Loop through candidates to produce and submit new tasks
            for (Candidate candidate : this.population) {
                // Setup callable
                Callable<Void> PipelineContainer = new CallablePipelineContainer(pipe, pipelineOutputFilePath, candidate, cleanupFiles);
                // Add future, which the executor will return to the list
                futures.add(executor.submit(PipelineContainer));
            }
            // Loop through futures to handle thrown exceptions
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    // Handle exception
                    evolutionProgressConnector.putException(e);
                    // Log exception
                    e.printStackTrace();
                }
            }
            executor.shutdown();
            // Log completed scoring round
            System.out.println("Finished all threads");
        } else {
            for (Candidate candidate : this.population) {
                // Set dummy fitness
                double score = candidate.getPhenotype().getExactMass();
                candidate.setRawScore(score);
                try {
                    candidate.calculateLigandEfficiency();
                } catch (PluginException e) {
                    e.printStackTrace();
                }
                candidate.calculateLigandLipophilicityEfficiency();
            }
        }
        processRawScores();
    }

    private void processRawScores() {
        for (Candidate candidate : this.population) {
            // Ligand efficiency
            candidate.setFitnessMeasure(this.fitnessMeasure);
        }
        // Collect scores
        List<Double> fitnesses = this.population.stream()
                .map(Candidate::getFitness)
                .collect(Collectors.toList());
        // Add scores for the archive
        scores.add(fitnesses);

        // Get min and max
        Double maxFitness = Collections.max(fitnesses);
        Double minFitness = Collections.min(fitnesses);
        // We whould like to calculate the fitness with the heavy atom
        for (Candidate candidate : this.population) {
            // Ligand efficiency
            candidate.calcNormFitness(minFitness, maxFitness);
        }
    }

    private boolean shouldTerminate() {
        int generationNumber = this.population.getGenerationNumber();
        if (this.terminationCondition == TerminationCondition.CONVERGENCE && generationNumber > 5) {
            return this.hasConverged(generationNumber);
        } else if (this.terminationCondition == TerminationCondition.DURATION) {
            return this.maximumAllowedDuration <= duration;
        }
        return generationNumber == this.maxNumberOfGenerations || evolutionProgressConnector.isTerminationRequired();
    }

    private boolean hasConverged(int generationNumber) {
        // Get all fitness scores so far
        List<List<Double>> populationFitness = this.getFitness();

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

    private void setupPipeline(Path outputFileLocation) throws PipelineException {
        Path anchor = Paths.get("X:\\Internship\\reference_fragment\\anchor.sdf");
        Path receptor = Paths.get("X:\\Internship\\receptor\\rec.mab");
        setupPipeline(outputFileLocation, receptor, anchor);
    }

    public void setupPipeline(Path outputFileLocation, Path receptorLocation, Path anchorLocation) throws PipelineException {
        int conformerCount = 15;
        double exclusionShapeTolerance = 0;
        setupPipeline(outputFileLocation, receptorLocation, anchorLocation, conformerCount, exclusionShapeTolerance);
    }

    /**
     * Setup the pipeline for scoring candidates
     */
    public void setupPipeline(Path outputFileLocation, Path receptorFilePath, Path anchor, int conformerCount, double exclusionShapeTolerance) throws PipelineException {
        // Set the pipeline output location
        this.setPipelineOutputFilePath(outputFileLocation);

        // Get the step for converting 'flat' molecules into multiple 3d conformers
        ThreeDimensionalConverterStep threeDimensionalConverterStep = new ThreeDimensionalConverterStep(
                this.pipelineOutputFilePath, conformerCount);
        // Get the step for fixing conformers to an anchor point
//        ConformerFixationStep conformerFixationStep = new ConformerFixationStep(anchor, System.getenv("OBFIT_EXE"));
        ConformerAlignmentStep conformerAlignmentStep = new ConformerAlignmentStep(anchor);
        // Get step that handles scored candidates
        ScoredCandidateHandlingStep scoredCandidateHandlingStep = new ScoredCandidateHandlingStep(
                anchor,
                receptorFilePath,
                exclusionShapeTolerance);
        // Get the step for energy minimization
        EnergyMinimizationStep energyMinimizationStep = getEnergyMinimizationStep(receptorFilePath, anchor);
        // Combine the steps and set the pipe.
        PipelineStep<Candidate, Candidate> converterStep = threeDimensionalConverterStep.pipe(conformerAlignmentStep);
        this.pipe = converterStep.pipe(energyMinimizationStep).pipe(scoredCandidateHandlingStep);
    }

    /**
     * Gets the minimization step that should be included in the pipeline based on the set force field.
     *
     * @param receptorFile The receptor file path in pdb format.
     * @param anchorFilePath the anchor file path in sdf format.
     * @return The energy minimization step that complies with the set force field.
     * @throws PipelineException if the minimization step could not be initialized.
     */
    private EnergyMinimizationStep getEnergyMinimizationStep(Path receptorFile, Path anchorFilePath) throws PipelineException {
        if (this.forceField == ForceField.MAB) {
            String mol3dExecutable = getExecutable("MOL3D_EXE");
            String esprntoExecutable = getExecutable("ESPRNTO_EXE");

            // Return Moloc implementation of the energy minimization step
            return new MolocEnergyMinimizationStep(
                    "",
                    receptorFile,
                    anchorFilePath,
                    mol3dExecutable,
                    esprntoExecutable);
        } else if (this.forceField == ForceField.SMINA) {
            String sminaExecutable = getExecutable("SMINA_EXE");
            String pythonExecutable = getExecutable("MGL_PYTHON");
            String prepareReceptorExecutable = getExecutable("PRPR_REC_EXE");

            // Return Smina implementation of the energy minimization step
            return new SminaEnergyMinimizationStep(
                    "",
                    receptorFile,
                    anchorFilePath,
                    sminaExecutable,
                    pythonExecutable,
                    prepareReceptorExecutable);
        } else {
            throw new RuntimeException(String.format("Force field '%s' is not implemented", this.forceField.toString()));
        }
    }

    /**
     * Method responsible for obtaining an executable path from the environment variables
     *
     * @param variableName The name of the environment variable containing the executable path
     * @return path to the executable as a String
     */
    private String getExecutable(String variableName) {
        // Try to get the variable
        String sminaExecutable = System.getenv(variableName);

        // Check if the smina executable was entered in the environment variables
        if (sminaExecutable == null) {
            // Throw an exception because the executable was not given in the environment variables
            throw new RuntimeException(String.format("Environment variable '%s' was null", variableName));
        }
        return sminaExecutable;
    }

    public void setCleanupFiles(boolean cleanupFiles) {
        this.cleanupFiles = cleanupFiles;
    }

    public enum ForceField {
        MAB("mab"),
        SMINA("smina");

        private final String text;

        ForceField(String text) {
            this.text = text;
        }

        public static ForceField fromString(String text) {
            for (ForceField condition : ForceField.values()) {
                if (condition.text.equalsIgnoreCase(text)) {
                    return condition;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }

    /**
     * Fitness measures that can be chosen.
     */
    public enum FitnessMeasure {
        LIGAND_LIPOPHILICITY_EFFICIENCY("ligandLipophilicityEfficiency"),
        LIGAND_EFFICIENCY("ligandEfficiency"),
        AFFINITY("affinity");

        private final String text;

        FitnessMeasure(String text) {
            this.text = text;
        }

        public static FitnessMeasure fromString(String text) {
            for (FitnessMeasure condition : FitnessMeasure.values()) {
                if (condition.text.equalsIgnoreCase(text)) {
                    return condition;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }

    /**
     * Termination conditions that can be chosen.
     */
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
        }
    }
}
