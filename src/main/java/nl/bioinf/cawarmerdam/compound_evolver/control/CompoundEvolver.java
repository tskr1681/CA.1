/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.marvin.plugin.PluginException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import com.jacob.com.NotImplementedException;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactionFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;
import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.*;
import nl.bioinf.cawarmerdam.compound_evolver.util.*;

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
    private ExecutorService executor;
    private Map<Long, Integer> clashingConformerCounter = new HashMap<>();
    private Map<Long, Integer> tooDistantConformerCounter = new HashMap<>();
    private List<List<Double>> scores = new ArrayList<>();
    private Path pipelineOutputFilePath;
    private ConformerOption conformerOption;
    private ForceField forceField;
    private ScoringOption scoringOption;
    private TerminationCondition terminationCondition;
    private List<PipelineStep<Candidate, Void>> pipe;
    private List<PipelineStep<Candidate, Candidate>> pipe2;
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
    private int targetCandidateCount;
    private int candidatesScored;
    private GenerationDataFileManager manager;
    private boolean selective;
    private boolean prepareReceptor;
    private boolean neural_network;
    private boolean deleteInvalid;

    /**
     * The constructor for a compound evolver.
     *
     * @param population                 The initial population.
     * @param evolutionProgressConnector An object that can be used to write progress to.
     */
    public CompoundEvolver(Population population, EvolutionProgressConnector evolutionProgressConnector) {
        this.population = population;
        this.evolutionProgressConnector = evolutionProgressConnector;
        this.maxNumberOfGenerations = 25;
        this.maximumAllowedDuration = 600000;
        this.forceField = ForceField.MAB;
        this.setCleanupFiles(false);
        this.terminationCondition = TerminationCondition.FIXED_GENERATION_NUMBER;
        this.conformerOption = ConformerOption.CHEMAXON;
        this.neural_network = false;
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
        String[] reactantFiles = Arrays.copyOfRange(args, 1, args.length - 4);
        List<List<Molecule>> reactantLists = ReactantFileHandler.loadMolecules(reactantFiles, 0);
        // Load anchor and receptor molecules
        Path pipelineLocation = Paths.get(args[args.length - 2]);
        Path receptor = Paths.get(args[args.length - 4]);
        Path anchor = Paths.get(args[args.length - 3]);
        // Construct the initial population
        ArrayList<Reactor> reactions = new ArrayList<>();
        reactions.add(reactor);
        List<Species> species = Species.constructSpecies(reactions, reactantLists.size());
        Population population = new Population(reactantLists, species, maxSamples, 1);
//        population.initializeAlleleSimilaritiesMatrix();
        population.setMutationMethod(Population.MutationMethod.DISTANCE_DEPENDENT);
        population.setSelectionMethod(Population.SelectionMethod.TRUNCATED_SELECTION);
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, new CommandLineEvolutionProgressConnector());
        compoundEvolver.setupPipeline(pipelineLocation, receptor, anchor);
        compoundEvolver.setDummyFitness(false);
        // Evolve compounds
        compoundEvolver.evolve();
    }

    /**
     * Getter for the duration of the evolution procedure in ms.
     *
     * @return the duration of the evolution procedure in ms.
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
     * Setter for the maximum allowed duration of the evolution procedure. No new generation is made when this
     * duration is surpassed by actual duration.
     *
     * @param maximumAllowedDuration The maximum allowed duration of the evolution procedure.
     */
    public void setMaximumAllowedDuration(long maximumAllowedDuration) {
        this.maximumAllowedDuration = maximumAllowedDuration;
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
     * @param dummyFitness True if the dummy fitness should be applied. (the molecular mass is the raw score)
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
     * @param pipelineOutputFilePath The output file path for the pipeline.
     * @throws PipelineException If the output file path does not exist and cannot be created.
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
     * @param forceField The force field.
     */
    public void setForceField(ForceField forceField) {
        this.forceField = forceField;
    }

    /**
     * Getter for the scoring method that is used in scoring and minimization.
     *
     * @return the scoring method.
     */
    public ScoringOption getScoringOption() {
        return scoringOption;
    }

    /**
     * Setter for the scoring method that is used in scoring and minimization.
     *
     * @param scoringOption The scoring option.
     */
    public void setScoringOption(ScoringOption scoringOption) {
        this.scoringOption = scoringOption;
    }

    /**
     * Setter for the conformer generation method.
     *
     * @param conformerOption The conformer generation option.
     */
    public void setConformerOption(ConformerOption conformerOption) {
        this.conformerOption = conformerOption;
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
     * @param fitnessMeasure The fitness measure.
     */
    public void setFitnessMeasure(FitnessMeasure fitnessMeasure) {
        this.fitnessMeasure = fitnessMeasure;
    }

    /**
     * Getter for the termination condition.
     *
     * @return the termination condition.
     */
    public TerminationCondition getTerminationCondition() {
        return terminationCondition;
    }

    /**
     * Setter for the termination condition.
     *
     * @param terminationCondition The fitness measure.
     */
    public void setTerminationCondition(TerminationCondition terminationCondition) {
        this.terminationCondition = terminationCondition;
    }

    /**
     * Setter for selectivity checks
     *
     * @param selective do selectivity checks?
     */
    public void setSelective(boolean selective) {
        this.selective = selective;
    }


    /**
     * Checks if receptor preparation is enabled
     *
     * @return if receptor preparation is enabled
     */
    public boolean isPrepareReceptor() {
        return prepareReceptor;
    }

    /**
     * Sets if receptor preparation is enabled
     *
     * @param prepareReceptor set if receptor preparation is enabled
     */
    public void setPrepareReceptor(boolean prepareReceptor) {
        this.prepareReceptor = prepareReceptor;
    }

    public boolean isDeleteInvalid() {
        return deleteInvalid;
    }

    public void setDeleteInvalid(boolean deleteInvalid) {
        this.deleteInvalid = deleteInvalid;
    }

    /**
     * Getter for the maximum number of generations in the evolution process.
     *
     * @return the maximum number of generations in the evolution process.
     */
    public int getMaxNumberOfGenerations() {
        return maxNumberOfGenerations;
    }

    /**
     * Setter for the maximum number of generations in the evolution process.
     *
     * @param generations The maximum number of generations in the evolution process.
     */
    public void setMaxNumberOfGenerations(int generations) {
        this.maxNumberOfGenerations = generations;
    }

    private List<Candidate> filterCandidates(List<List<Candidate>> c) {
        System.out.println("Candidates to filter: " + c);
        List<Candidate> out = new ArrayList<>();
        if (c.size() > 0) {
            for (int i = 0; i < c.size(); i++) {
                boolean invalid = false;
                for (int j = 0; j < c.get(0).size(); j++) {
                    if (c.get(i).get(j) == null) {
                        invalid = true;
                        break;
                    }
                }
                if (!invalid) {
                    out.add(c.get(i).get(0));
                }
            }
        }
        System.out.println("Filtered candidates: " + out);
        return out;
    }

    /**
     * Evolve compounds
     */
    public void evolve() throws OffspringFailureOverflow, TooFewScoredCandidates, ForcedTerminationException {
//        dummyFitness = true;
        // Set startTime and signal that the evolution procedure has started
        startTime = System.currentTimeMillis();
        this.executor = Executors.newFixedThreadPool(getIntegerEnvironmentVariable("POOL_SIZE"));
        evolutionProgressConnector.setStatus(EvolutionProgressConnector.Status.RUNNING);

        this.population.setTotalGenerations(maxNumberOfGenerations);
        // Score the initial population
        List<List<Candidate>> candidates = getInitialCandidates();
        List<Candidate> validCandidates = new ArrayList<>(filterCandidates(candidates));
        System.out.println("candidates = " + candidates);
        System.out.println("validCandidates.size() = " + validCandidates.size());
        while (validCandidates.size() < population.getPopulationSize() && !evolutionProgressConnector.isTerminationRequired()) {
            if (this.pipelineOutputFilePath.resolve("terminate").toFile().exists())
                throw new ForcedTerminationException("The program was terminated forcefully.");
            population = new Population(population.reactantLists, population.species, population.getSpeciesDeterminationMethod(), population.getPopulationSize(), population.getReceptorAmount());
            population.setSelective(this.selective);
            candidates = getInitialCandidates();
            validCandidates.addAll(filterCandidates(candidates));
            System.out.println("candidates = " + candidates);
            System.out.println("validCandidates.size() = " + validCandidates.size());
        }
        if (evolutionProgressConnector.isTerminationRequired()) {
            evolutionProgressConnector.setStatus(EvolutionProgressConnector.Status.FAILED);
            return;
        }
        population.setCandidateList(validCandidates);
        population.setOutputLocation(pipelineOutputFilePath);
        scoreCandidates();
        population.setValidifypipe(pipe2);
        try {
            manager.writeGeneration(population);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                try {
                    manager.writeGeneration(population);
                } catch (Exception ignored) {
                }
                evolutionProgressConnector.handleNewGeneration(population.getCurrentGeneration());
                updateDuration();
            }
            evolutionProgressConnector.setStatus(EvolutionProgressConnector.Status.SUCCESS);
        } catch (OffspringFailureOverflow | TooFewScoredCandidates e) {
            evolutionProgressConnector.setStatus(EvolutionProgressConnector.Status.FAILED);
            throw e;
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        System.out.println("population.tooDistantConformerCounter = " + tooDistantConformerCounter.values().stream().mapToInt(i -> i).sum());
        System.out.println("clashingConformerCounter = " + clashingConformerCounter.values().stream().mapToInt(i -> i).sum());
        this.manager.close();
    }

    /**
     * Gets an environment variable as an integer.
     *
     * @param variableName the name of the environment variable that should be parsed to an integer.
     * @return An integer.
     */
    private int getIntegerEnvironmentVariable(String variableName) {
        String environmentVariable = getEnvironmentVariable(variableName);
        if (NumberCheckUtilities.isInteger(environmentVariable, 10)) {
            return Integer.parseInt(environmentVariable);
        }
        // Throw an exception because the environment variables was not an integer.
        throw new RuntimeException(String.format("Environment variable '%s' was not an integer value", variableName));
    }

    /**
     * Recalculates the current duration of evolution.
     */
    private void updateDuration() {
        long endTime = System.currentTimeMillis();
        this.duration = endTime - startTime;
    }

    /**
     * Scores the candidates in the population.
     */
    private void scoreCandidates() throws TooFewScoredCandidates, ForcedTerminationException {
        if (!neural_network) {
            if (!dummyFitness) {
                // Check if pipe is present
                if (pipe == null) throw new RuntimeException("pipeline setup not complete!");
                // Create list to hold future object associated with Callable
                List<Future<Void>> futures = new ArrayList<>();
                // Loop through candidates to produce and submit new tasks
                List<List<Candidate>> matchingCandidateList = this.population.matchingCandidateList();
                for (List<Candidate> candidates : matchingCandidateList) {
                    // Setup callable
                    Callable<Void> PipelineContainer = new CallableFullPipelineContainer(pipe, pipelineOutputFilePath, candidates, cleanupFiles);
                    // Add future, which the executor will return to the list
                    futures.add(executor.submit(PipelineContainer));
                }
                // Loop through futures to handle thrown exceptions
                for (Future<Void> future : futures) {
                    try {
                        if (this.pipelineOutputFilePath.resolve("terminate").toFile().exists())
                            throw new ForcedTerminationException("The program was terminated forcefully.");
                        future.get();
                        candidatesScored += 1;
                    } catch (InterruptedException | ExecutionException e) {
                        // Handle exception
//                    evolutionProgressConnector.putException(e);
                        // Log exception
                        System.err.println("Encountered an exception while scoring candidates: " + e.getMessage());
                    }
                }
                // Log completed scoring round
                System.out.println("Finished all threads");
            } else {
                for (List<Candidate> candidates : this.population.matchingCandidateList()) {
                    for (Candidate candidate : candidates) {
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
            }
            population.filterUnscoredCandidates();
            if (population.size() == 0) {
                throw new TooFewScoredCandidates(
                        "The population is empty. Increase the amount of candidates or conformers, or apply less restrictive filters");
            }
        } else {
            NNHelper helper = new NNHelper(Paths.get("C:\\Users\\F100961\\Documents\\chemprop-master\\predict.py"),
                    Paths.get("C:\\Users\\F100961\\Documents\\chemprop-master\\checkpoint\\fold_0\\model_0\\model.pt"),
                    Paths.get("C:\\Users\\F100961\\Desktop\\wrappers\\chemprop\\run-in.bat"), this.pipelineOutputFilePath);
            List<List<Candidate>> matchingCandidateList = this.population.matchingCandidateList();
            if (matchingCandidateList.get(0).size() > 1) {
                throw new NotImplementedException("Neural Network scoring is only implemented for single receptors at this point in time.");
            }
            try {
                helper.getMoleculeScores(matchingCandidateList.stream().map(candidates -> candidates.get(0)).collect(Collectors.toList()));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        processRawScores();
    }

    /**
     * Gets the candidates in the population.
     */
    private List<List<Candidate>> getInitialCandidates() throws ForcedTerminationException {
        List<List<Candidate>> candidates = new ArrayList<>();
        if (!dummyFitness && !neural_network) {
            // Check if pipe is present
            if (pipe == null) throw new RuntimeException("pipeline setup not complete!");
            // Create list to hold future object associated with Callable
            List<Future<List<Candidate>>> futures = new ArrayList<>();
            // Loop through candidates to produce and submit new tasks
            List<List<Candidate>> matchingCandidateList = this.population.matchingCandidateList();
            for (List<Candidate> candidateList : matchingCandidateList) {
                // Setup callable
                Callable<List<Candidate>> PipelineContainer = new CallableValidificationPipelineContainer(pipe2, pipelineOutputFilePath, candidateList);
                // Add future, which the executor will return to the list
                futures.add(executor.submit(PipelineContainer));
            }
            // Loop through futures to handle thrown exceptions
            for (Future<List<Candidate>> future : futures) {
                try {
                    if (this.pipelineOutputFilePath.resolve("terminate").toFile().exists())
                        throw new ForcedTerminationException("The program was terminated forcefully.");
                    List<Candidate> c = future.get(180, TimeUnit.SECONDS);
                    if (c != null) {
                        candidates.add(c);
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
                    // Handle exception
//                    evolutionProgressConnector.putException(e);
                    // Log exception
                    // System.err.println("Encountered exception while generating initial candidates: " + e.getMessage());
                }
            }
        } else {
            candidates = this.population.getCandidateList();
        }
        return candidates;
    }

    /**
     * Method responsible for processing the raw scores. Normalized scores are calculated by using the set
     * fitness measure.
     */
    private void processRawScores() {
        for (List<Candidate> candidates : this.population.getCandidateList()) {
            for (Candidate candidate : candidates) {
                candidate.setFitnessMeasure(this.fitnessMeasure);
            }
        }
        // Collect scores
        List<List<Double>> fitnesses = new ArrayList<>();
        for (List<Candidate> c : population.getCandidateList()) {
            fitnesses.add(c.stream().map(Candidate::getFitness).collect(Collectors.toList()));
        }

        // Get min and max
        Double maxFitness = Collections.max(fitnesses.stream().mapToDouble(Collections::max).boxed().collect(Collectors.toList()));
        Double minFitness = Collections.min(fitnesses.stream().mapToDouble(Collections::min).boxed().collect(Collectors.toList()));
        // We would like to calculate the fitness with the heavy atom
        for (List<Candidate> candidates : this.population.getCandidateList()) {
            for (Candidate candidate : candidates) {
                // Ligand efficiency
                candidate.calcNormFitness(minFitness, maxFitness);
            }
        }
        population.setFitnessCandidateList();
        // Add scores for the archive
        if (this.selective) {
            scores.add(Arrays.stream(MultiReceptorHelper.getFitnessList(population.getCandidateList())).boxed().collect(Collectors.toList()));
        } else {
            scores.add(Arrays.stream(MultiReceptorHelper.getFitnessListSelectivity(population.getCandidateList())).boxed().collect(Collectors.toList()));
        }
    }

    /**
     * Method that indicates if the evolution should be terminated.
     *
     * @return true if the evolution should be terminated.
     */
    private boolean shouldTerminate() {
        int generationNumber = this.population.getGenerationNumber();
        if (this.terminationCondition == TerminationCondition.CONVERGENCE && generationNumber > 5) {
            return this.hasConverged(generationNumber);
        } else if (this.terminationCondition == TerminationCondition.DURATION) {
            return this.maximumAllowedDuration <= duration;
        } else if (this.terminationCondition == TerminationCondition.MAXIMUM_CANDIDATE_COUNT) {
            System.out.println("candidatesScored = " + this.candidatesScored + ", max = " + this.targetCandidateCount);
            return this.targetCandidateCount <= this.candidatesScored;
        }
        return generationNumber == this.maxNumberOfGenerations || evolutionProgressConnector.isTerminationRequired();
    }

    /**
     * Method that indicates if the evolution has converged.
     *
     * @param generationNumber The number of the current generation.
     * @return true if the evolution has converged.
     */
    private boolean hasConverged(int generationNumber) {
        // Get all fitness scores so far
        List<List<Double>> populationFitness = this.getFitness();

        // Initialize highestScore and it's generation number
        double highestScore = 0;
        int highestScoringGenerationNumber = 0;

        // Initialize maximum of the generation.
        double max;
        for (int i = 0; i < populationFitness.size(); i++) {
            // Get maximum of the generation
            max = Collections.max(populationFitness.get(i));

            // If maximum of the generation is larger than the highest score overall, set new highest score
            if (max > highestScore) {
                highestScore = max;
                highestScoringGenerationNumber = i;
            }
        }
        double nonImprovingGenerationNumber = highestScoringGenerationNumber * this.nonImprovingGenerationAmountFactor + highestScoringGenerationNumber;
        return nonImprovingGenerationNumber < generationNumber;
    }

    void setupPipeline(Path outputFileLocation, Path receptorLocation, Path anchorLocation) throws PipelineException {
        int conformerCount = 15;
        double exclusionShapeTolerance = 0;
        double maximumAnchorDistance = 2;
        setupPipeline(outputFileLocation, receptorLocation, anchorLocation, conformerCount, exclusionShapeTolerance,
                maximumAnchorDistance, true);
    }

    /**
     * Setup the pipeline for scoring candidates
     */
    public void setupPipeline(Path outputFileLocation,
                              Path receptorFilePath,
                              Path anchor,
                              int conformerCount,
                              double exclusionShapeTolerance,
                              double maximumAnchorDistance,
                              boolean fast_align) throws PipelineException {
        // Set the pipeline output location
        this.setPipelineOutputFilePath(outputFileLocation);
        if (this.pipe == null)
            this.pipe = new ArrayList<>();
        if (this.pipe2 == null)
            this.pipe2 = new ArrayList<>();
        if (this.prepareReceptor) {
            PdbFixer.runFixer(System.getenv("LEPRO_EXE"), receptorFilePath);
            receptorFilePath = receptorFilePath.resolveSibling("pro.pdb");
        }
        // Get the step for converting 'flat' molecules into multiple 3d conformers
        PipelineStep<Candidate, Candidate> threeDimensionalConverterStep = getConformerStep(conformerCount, anchor);
//        PipelineStep<Candidate, Candidate> threeDimensionalConverterStep = new ThreeDimensionalConverterStep(this.pipelineOutputFilePath, conformerCount);
        // Get the step for fixing conformers to an anchor point
//        ConformerFixationStep conformerFixationStep = new ConformerFixationStep(anchor, System.getenv("OBFIT_EXE"));
        PipelineStep<Candidate, Candidate> converterStep;
        if (this.conformerOption == ConformerOption.CUSTOM) {
            converterStep = threeDimensionalConverterStep;
        } else {
            converterStep = threeDimensionalConverterStep.pipe(new ConformerAlignmentStep(anchor, fast_align));
        }
        // Get step that handles scored candidates
        PipelineStep<Candidate, Void> scoredCandidateHandlingStep = new ScoredCandidateHandlingStep(
        );
        // Get the step for energy minimization
        PipelineStep<Candidate, Candidate> energyMinimizationStep = getEnergyMinimizationStep(receptorFilePath, anchor, exclusionShapeTolerance, maximumAnchorDistance);
        // Combine the steps and set the pipe.
        PipelineStep<Candidate, Candidate> validifyStep = new ValidateConformersStep(anchor, receptorFilePath, exclusionShapeTolerance, maximumAnchorDistance, clashingConformerCounter, tooDistantConformerCounter, deleteInvalid);
        this.pipe2.add(converterStep.pipe(validifyStep).pipe(energyMinimizationStep).pipe(validifyStep));
        this.pipe.add(converterStep.pipe(validifyStep).pipe(energyMinimizationStep).pipe(scoredCandidateHandlingStep));
        System.out.println("Initializing generation manager");
        try {
            this.manager = new GenerationDataFileManager(pipelineOutputFilePath.resolve("gen-info.txt").toFile());
        } catch (IOException e) {
            System.err.println("Initializing generation data file manager failed.");
        }
    }

    /**
     * Gets the minimization step that should be included in the pipeline based on the set force field.
     *
     * @param receptorFile   The receptor file path in pdb format.
     * @param anchorFilePath the anchor file path in sdf format.
     * @return The energy minimization step that complies with the set force field.
     * @throws PipelineException if the minimization step could not be initialized.
     */
    private PipelineStep<Candidate, Candidate> getEnergyMinimizationStep(Path receptorFile, Path anchorFilePath, double exclusionShapeTolerance, double maximumAnchorDistance) throws PipelineException {
        PipelineStep<Candidate, Candidate> step;
        switch (this.forceField) {
            case MAB:
                String mol3dExecutable = getEnvironmentVariable("MOL3D_EXE");
                String esprntoExecutable = getEnvironmentVariable("ESPRNTO_EXE");
                step = new MolocEnergyMinimizationStep(
                        receptorFile,
                        mol3dExecutable,
                        esprntoExecutable).pipe(new ValidateConformersStep(anchorFilePath, receptorFile, exclusionShapeTolerance, maximumAnchorDistance, clashingConformerCounter, tooDistantConformerCounter, deleteInvalid));
                break;
            case SMINA:
                String sminaExecutable = getEnvironmentVariable("SMINA_EXE");
                String pythonExecutable = getEnvironmentVariable("MGL_PYTHON");
                String prepareReceptorExecutable = getEnvironmentVariable("PRPR_REC_EXE");

                // Return Smina implementation of the energy minimization step
                step = new SminaEnergyMinimizationStep(
                        receptorFile,
                        sminaExecutable,
                        pythonExecutable,
                        prepareReceptorExecutable).pipe(new ValidateConformersStep(anchorFilePath, receptorFile, exclusionShapeTolerance, maximumAnchorDistance, clashingConformerCounter, tooDistantConformerCounter, deleteInvalid));
                break;
            default:
                throw new RuntimeException(String.format("Force field '%s' is not implemented", this.forceField.toString()));
        }
        switch (this.scoringOption) {
            case MAB:
                if (this.forceField == ForceField.MAB) {
                    return step;
                } else {
                    String mol3dExecutable = getEnvironmentVariable("MOL3D_EXE");
                    String esprntoExecutable = getEnvironmentVariable("ESPRNTO_EXE");
                    return step.pipe(new MolocEnergyMinimizationStep(
                            receptorFile,
                            mol3dExecutable,
                            esprntoExecutable).pipe(new ValidateConformersStep(anchorFilePath, receptorFile, exclusionShapeTolerance, maximumAnchorDistance, clashingConformerCounter, tooDistantConformerCounter, deleteInvalid)));
                }
            case SMINA:
                if (this.forceField == ForceField.SMINA) {
                    return step;
                } else {
                    String sminaExecutable = getEnvironmentVariable("SMINA_EXE");
                    String pythonExecutable = getEnvironmentVariable("MGL_PYTHON");
                    String prepareReceptorExecutable = getEnvironmentVariable("PRPR_REC_EXE");

                    // Return Smina implementation of the energy minimization step
                    return step.pipe(new SminaEnergyMinimizationStep(
                            receptorFile,
                            sminaExecutable,
                            pythonExecutable,
                            prepareReceptorExecutable));
                }
            case SCORPION:
                String scorpionExecutable = getEnvironmentVariable("FINDPATHS3_EXE");
                String pythonExecutable = getEnvironmentVariable("PYTHON_EXE");
                String fixerExecutable = getEnvironmentVariable("FIXER_EXE");
                return step.pipe(new ScorpionScoringStep(receptorFile, scorpionExecutable, fixerExecutable, pythonExecutable));
            default:
                throw new RuntimeException(String.format("Scoring step '%s' is not implemented", this.scoringOption.toString()));

        }
    }

    private PipelineStep<Candidate, Candidate> getConformerStep(int conformerCount, Path anchor) {
        switch (this.conformerOption) {
            case CHEMAXON:
                return new ThreeDimensionalConverterStep(this.pipelineOutputFilePath, conformerCount);
            case MOLOC:
                return new MolocConformerStep(
                        this.pipelineOutputFilePath, conformerCount, System.getenv("MCNF_EXE"), System.getenv("MSMAB_EXE"), false);
            case MACROCYCLE:
                return new MolocConformerStep(
                        this.pipelineOutputFilePath, conformerCount, System.getenv("MCNF_EXE"), System.getenv("MSMAB_EXE"), true);
            case CUSTOM:
                return new CustomConformerStep(
                        this.pipelineOutputFilePath, Paths.get(System.getenv("RDKIT_WRAPPER")), Paths.get(System.getenv("CONFORMER_SCRIPT")), anchor, conformerCount);
            default:
                return new ThreeDimensionalConverterStep(this.pipelineOutputFilePath, conformerCount);
        }
    }

    /**
     * Method responsible for obtaining an executable path from the environment variables
     *
     * @param variableName The name of the environment variable containing the executable path
     * @return path to the executable as a String
     */
    private String getEnvironmentVariable(String variableName) {
        // Try to get the variable
        String sminaExecutable = System.getenv(variableName);

        // Check if the smina executable was entered in the environment variables
        if (sminaExecutable == null) {
            // Throw an exception because the executable was not given in the environment variables
            throw new RuntimeException(String.format("Environment variable '%s' was null", variableName));
        }
        return sminaExecutable;
    }

    /**
     * Setter for the clearing and cleaning of pipeline files.
     *
     * @param cleanupFiles True if pipeline files have to be cleared.
     */
    void setCleanupFiles(boolean cleanupFiles) {
        this.cleanupFiles = cleanupFiles;
    }

    /**
     * The way to generate conformers
     */
    public enum ConformerOption {
        CHEMAXON("ChemAxon"),
        MOLOC("Moloc"),
        MACROCYCLE("Macrocycle"),
        CUSTOM("Custom");

        private final String text;

        ConformerOption(String text) {
            this.text = text;
        }

        public static ConformerOption fromString(String text) {
            for (ConformerOption condition : ConformerOption.values()) {
                if (condition.text.equalsIgnoreCase(text)) {
                    return condition;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }

    /**
     * The force field or program enum.
     */
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
     * The force field or program enum.
     */
    public enum ScoringOption {
        MAB("mab"),
        SMINA("smina"),
        SCORPION("scorpion");

        private final String text;

        ScoringOption(String text) {
            this.text = text;
        }

        public static ScoringOption fromString(String text) {
            for (ScoringOption condition : ScoringOption.values()) {
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
        DURATION("duration"),
        MAXIMUM_CANDIDATE_COUNT("maximum candidate count");

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
