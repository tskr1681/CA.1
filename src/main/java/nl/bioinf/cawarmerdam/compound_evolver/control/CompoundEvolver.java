[7:43 AM, 6/27/2023] Aniket: package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.reaction.AtomIdentifier;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.util.iterator.MoleculeIterator;
import com.chemaxon.search.mcs.MaxCommonSubstructure;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReactantScoreHelper {

    public static logPPlugin logPPlugin = new logPPlugin();

    public static List<Double> getReactantScores(Candidate candidate, CompoundEvolver.FitnessMeasure measure) throws IOException {
        if (candidate.getScoredConformersFile() == null) {
            return null;
        }
        List<Double> reactantscores = new ArrayList<>();
        Map<MolAtom, AtomIdentifier> atommap = candidate.getAtommap();
        Molecule[] reactants = candidate.getReactants();
        Molecule phenotype = candidate.getPhenotype();
        MolImporter importer = new MolImporter(candidate.getScoredConformersFile().toFile());
        Molecule scored = getScorpScores(importer);
        if (scored == null) {
            return null;
        }
        // Find the max common substructure, aka the mapping from unscored to scored molecule
        MaxCommonSubstructure substructure = MaxCommonSubstructure.newInstance();
        substructure.setQuery(phenotype);
        substructure.setTarget(scored);
        int[] mapping = substructure.find().getAtomMapping();
        // Go through all the atoms in the phenotype
        for (int i = 0; i < phenotype.atoms().size(); i++) {
            MolAtom atom = phenotype.atoms().get(i);
            AtomIdentifier identifier = atommap.get(atom);

            // It should have an identifier, it should be mapped, and it should be part of the reactants
            if (identifier != null && mapping[i] != -1 && identifier.getMoleculeIndex() != -1) {
                //get atom score
                MolAtom scoredatom = scored.atoms().get(mapping[i]);
                double score = scoredatom.getProperty("score") == null ? 0.0D : (double) scoredatom.getProperty("score");

                //get matching reactant atom and set score
                MolAtom reactantatom = reactants[identifier.getMoleculeIndex()].getAtom(identifier.getAtomIndex());
                if (reactantatom != null) {
                    reactantatom.putProperty("score", score);
                }
            }
        }

        for (Molecule reactant : reactants) {
            double normalizationfactor = measure == CompoundEvolver.FitnessMeasure.LIGAND_EFFICIENCY ? (reactant.getAtomCount() - reactant.getExplicitHcount()) : 1.0D;

            double score = reactant.atoms().stream().mapToDouble(molAtom ->
                    molAtom.getProperty("score") == null ? 0.0D : ((double) molAtom.getProperty("score") / normalizationfactor)
            ).sum();

            // Calculate QED
            double qed = calculateQED(reactant);

            // Calculate BBB score
            double bbbScore = calculateBBBScore(reactant);

            // Combine the scores based on your requirements
            double combinedScore = calculateCombinedScore(score, qed, bbbScore);

            // Sum all the scores for each reactant
            reactantscores.add(combinedScore);
        }
        return reactantscores;
    }

    public static double calculateQED(Molecule reactant) {
        // exp(w1 * logP - w2 * ro5Violation - w3 * logS - w4 * hba - w5 * hbd) / Z
        try {
            qedPlugin.setMolecule(reactant);
            qedPlugin.run();
            return qedPlugin.getQEDValue();
        } catch (PluginException e) {
            e.printStackTrace();
        }
        return 0.0D;
    }

    public static double calculateBBBScore(Molecule reactant) {
        // A * arom + B * HA + C * MWHBN + D * TPSA + E * pKa
        // Implement the BBB score calculation based on your specific criteria
        // Use properties, descriptors, or rules to evaluate BBB permeability of the molecule
        return 0.0D;
    }

    public static double calculateCombinedScore(double score, double qed, double bbbScore) {
        // Define your own formula to combine the individual scores (score, qed, bbbScore) into a single score
        // You can assign weights or apply mathematical operations based on your requirements
        return score + (0.5 * qed) - (0.2 * bbbScore);
    }

    public static double calculateLigandLipophilicityEfficiency(Molecule reactant, double score) {
        final double kcalToJouleConstant = 4.186798188;
        try {
            logPPlugin.setMolecule(reactant);
            logPPlugin.run();
            return Math.log(-score * kcalToJouleConstant) - logPPlugin.getlogPTrue();
        } catch (PluginException e) {
            e.printStackTrace();
        }
        return 0.0D;
    }

    public static Molecule getScorpScores(MolImporter importer) {
        double best = Double.NEGATIVE_INFINITY;
        Molecule best_mol = null;
        for (MoleculeIterator it = importer.getMoleculeIterator(); it.hasNext(); ) {
            Molecule m = it.next();
            if (m.properties().get("TOTAL") != null) {
                double score = Double.parseDouble(m.properties().get("TOTAL").getPropValue().toString());
                if (score > best) {
                    best = score;
                    best_mol = m;
                    String[] contacts = m.properties().get("CONTACTS").getPropValue().toString().split("\\n");
                    for (String contact : contacts) {
                        String[] contents = contact.replace("'", "").split(",");
                        double atom_score = Double.parseDouble(contents[contents.length - 1]);
                        int temp = 100;
                        for (int i = 0; i < 10; i++) {
                            temp = contents[0].indexOf(String.valueOf(i)) < temp && contents[0].contains(String.valueOf(i)) ? contents[0].indexOf(String.valueOf(i)) : temp;
                        }
                        int atom_index = Integer.parseInt(contents[0].substring(temp)) - 1;
                        m.atoms().get(atom_index).putProperty("score", atom_score);
                    }
                }
            }
        }
        return best_mol;
    }
}
[7:44 AM, 6/27/2023] Aniket: /*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.formats.MolImporter;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;
import com.google.common.collect.Lists;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;
import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.*;
import nl.bioinf.cawarmerdam.compound_evolver.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
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
    private final Map<Long, Integer> clashingConformerCounter = new HashMap<>();
    private final Map<Long, Integer> tooDistantConformerCounter = new HashMap<>();
    private final List<List<Double>> scores = new ArrayList<>();
    private Path pipelineOutputFilePath;
    private ConformerOption conformerOption;
    private ForceField forceField;
    private ScoringOption scoringOption;
    private TerminationCondition terminationCondition;
    private List<PipelineStep<Candidate, Void>> pipe;
    private List<PipelineStep<Candidate, Candidate>> pipe2;
    private Population population;
    private final EvolutionProgressConnector evolutionProgressConnector;
    private FitnessMeasure fitnessMeasure;
    private long startTime;
    private long duration;
    private long maximumAllowedDuration;
    private int maxNumberOfGenerations;
    private double nonImprovingGenerationAmountFactor;
    private boolean dummyFitness;
    private int targetCandidateCount;
    private int candidatesScored;
    private GenerationDataFileManager manager;
    private boolean selective;
    private boolean prepareReceptor;
    private boolean deleteInvalid;
    private boolean debugPrint;
    private boolean isBoosting = false;
    private BoosterApplication boosterApplication = BoosterApplication.NONE;

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
        this.terminationCondition = TerminationCondition.FIXED_GENERATION_NUMBER;
        this.conformerOption = ConformerOption.CHEMAXON;
    }


    public void setBoosterApplication(BoosterApplication boosterApplication) {
        this.boosterApplication = boosterApplication;
    }

    public boolean isDebugPrint() {
        return debugPrint;
    }

    public void setDebugPrint(boolean debugPrint) {
        this.debugPrint = debugPrint;
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
        getInitialPopulation();
        mainEvolution();
    }

    private void getInitialPopulation() throws ForcedTerminationException, TooFewScoredCandidates {
        //        dummyFitness = true;
        // Set startTime and signal that the evolution procedure has started
        startTime = System.currentTimeMillis();
        this.executor = Executors.newFixedThreadPool(getIntegerEnvironmentVariable("POOL_SIZE"));
        evolutionProgressConnector.setStatus(EvolutionProgressConnector.Status.RUNNING);

        this.population.setTotalGenerations(maxNumberOfGenerations);
        // Score the initial population
        System.out.println("Generating candidates!");
        List<List<Candidate>> candidates = getInitialCandidates();
        List<Candidate> validCandidates = new ArrayList<>(filterCandidates(candidates));
        System.out.println("Current candidates: " + candidates);
        System.out.println("Amount of valid candidates: " + validCandidates.size());
        while (validCandidates.size() < population.getPopulationSize() && !evolutionProgressConnector.isTerminationRequired()) {
            if (this.pipelineOutputFilePath.resolve("terminate").toFile().exists())
                throw new ForcedTerminationException("The program was terminated forcefully.");
            // Initial population needs to use the pre-filtered reactant lists
            population = population.newPopulation(true);
            candidates = getInitialCandidates();
            validCandidates.addAll(filterCandidates(candidates));
            System.out.println("Current candidates: " + candidates);
            System.out.println("Amount of valid candidates: " + validCandidates.size());
            deleteEmpty();
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
    }

    private void mainEvolution() throws ForcedTerminationException, OffspringFailureOverflow, TooFewScoredCandidates {
        try {
            // Evolve
            while (!shouldTerminate()) {
                System.out.println(this.population.toString());

                // Try to produce offspring
                System.out.println("Producing offspring!");
                this.population.produceOffspring();

                // Score the candidates
                System.out.println("Scoring candidates!");
                scoreCandidates();
                try {
                    manager.writeGeneration(population);
                } catch (Exception ignored) {
                }
                evolutionProgressConnector.handleNewGeneration(population.getCurrentGeneration());
                updateDuration();
                System.out.println("Deleting empty folders!");
                deleteEmpty();
            }
            if (this.scoringOption == ScoringOption.SCORPION && this.population.species.size() == 1
                    && !this.isBoosting && this.boosterApplication == BoosterApplication.SCORPION_BOOSTER) {
                runBooster();
            } else if (this.boosterApplication == BoosterApplication.COMBINATORIAL) {
                getBestCombinations();
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

    private void getBestCombinations() throws ForcedTerminationException, TooFewScoredCandidates {
        List<Candidate> bestCandidates = this.population.stream().sorted().collect(Collectors.toList()).subList((int) (0.9*population.size()), population.size());
        List<List<Integer>> bestReactants = new ArrayList<>();
        for (int i = 0; i < bestCandidates.get(0).getGenotype().size(); i++) {
            bestReactants.add(new ArrayList<>());
        }
        for (Candidate bestCandidate : bestCandidates) {
            for (int i = 0; i < bestCandidate.getGenotype().size(); i++) {
                bestReactants.get(i).add(bestCandidate.getGenotype().get(i));
            }
        }
        List<List<Integer>> allCombinations = Lists.cartesianProduct(bestReactants);
        List<Candidate> out = new ArrayList<>();
        for (List<Integer> combination : allCombinations) {
            Candidate c = new Candidate(combination, this.population.getCurrentValue().incrementAndGet(), population.getBaseSeed());
            c.finish(this.population.reactantLists, this.population.species);
            out.add(c);
        }
        this.population.setCandidateList(out);
        scoreCandidates();
        try {
            manager.writeGeneration(population);
        } catch (Exception ignored) {
        }
        evolutionProgressConnector.handleNewGeneration(population.getCurrentGeneration());
    }

    private void runBooster() throws ForcedTerminationException, TooFewScoredCandidates, OffspringFailureOverflow {
        List<List<ImmutablePair<Double, Integer>>> best_reactants = new ArrayList<>();
        for (Generation generation : this.evolutionProgressConnector.getGenerations()) {

            int candidate_count = generation.getCandidateList().size();
            int startIndex = best_reactants.size();
            try {
                for (int i = startIndex; i < startIndex + candidate_count; i++) {
                    Candidate c = generation.getCandidateList().get(i-startIndex);
                    List<Double> scores = ReactantScoreHelper.getReactantScores(c, this.fitnessMeasure);
                    if (scores != null) {
                        for (int j = 0; j < scores.size(); j++) {
                            while (best_reactants.size() <= i) {
                                best_reactants.add(new ArrayList<>());
                            }
                            best_reactants.get(i).add(new ImmutablePair<>(scores.get(j), c.getGenotype().get(j)));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        best_reactants = best_reactants.stream().filter(s -> s.size() > 0).collect(Collectors.toList());
        List<List<String>> reactants = getBestReactants(best_reactants, Math.min(best_reactants.size(), 10), population.reactantLists);
        List<List<Integer>> reactantSelection = new ArrayList<>();
        for (int i = 0; i < reactants.size(); i++) {
            reactantSelection.add(new ArrayList<>());
            for (int j = 0; j < reactants.get(i).size(); j++) {
                reactantSelection.get(i).add(j);
            }
        }
        this.population = population.newPopulation(reactants, reactantSelection);

        this.setTerminationCondition(TerminationCondition.FIXED_GENERATION_NUMBER);
        this.setMaxNumberOfGenerations(this.population.getGenerationNumber() + 3);
        this.isBoosting = true;
        scoreCandidates();
        mainEvolution();
    }

    private List<List<String>> getBestReactants(List<List<ImmutablePair<Double, Integer>>> scored, int amount, List<List<String>> reactants) {
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < scored.get(0).size(); i++) {
            List<ImmutablePair<Double, Integer>> scored_reactant_subset = new ArrayList<>();
            for (int j = 0; j < scored.size(); j++) {
                scored_reactant_subset.add(scored.get(j).get(i));
            }
            scored_reactant_subset.sort(Comparator.comparingDouble(ImmutablePair::getLeft));
            Collections.reverse(scored_reactant_subset);
            scored_reactant_subset = scored_reactant_subset.stream().distinct().collect(Collectors.toList());
            List<String> out_subset = new ArrayList<>();
            for (int j = 0; j < amount && j < scored_reactant_subset.size(); j++) {
                out_subset.add(reactants.get(i).get(scored_reactant_subset.get(j).getRight()));
            }
            out.add(out_subset);
        }
        return out;
    }

    private void deleteEmpty() {
        File[] directories = this.pipelineOutputFilePath.toFile().listFiles(File::isDirectory);
        for (File directory : directories) {
            if (directory.listFiles().length == 0) {
                try {
                    FileUtils.deleteDirectory(directory);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
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
        if (!dummyFitness) {
            // Check if pipe is present
            if (pipe == null) throw new RuntimeException("pipeline setup not complete!");
            // Create list to hold future object associated with Callable
            List<Future<Void>> futures = new ArrayList<>();
            // Loop through candidates to produce and submit new tasks
            List<List<Candidate>> matchingCandidateList = this.population.matchingCandidateList();
            for (List<Candidate> candidates : matchingCandidateList) {
                for (Candidate candidate : candidates) {
                    // Get candidate output directory
                    File candidate_dir = Paths.get(pipelineOutputFilePath.toString(),
                            String.valueOf(candidate.getIdentifier())).toFile();
                    try {
                        if (candidate_dir.exists() && candidate.canBeDeleted())
                            FileUtils.cleanDirectory(candidate_dir);
                    } catch (IOException e) {
                        if (debugPrint) {
                            System.err.println(e.getMessage());
                        }
                    }
                }
                // Setup callable
                Callable<Void> PipelineContainer = new CallableFullPipelineContainer(pipe, pipelineOutputFilePath, candidates);
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
                    System.err.println("Encountered an exception while scoring candidates: " + e.getCause().getMessage());
                    // Pipeline exceptions are expected, they are used to signal null candidates from validation as well
                    if (!(e.getCause() instanceof PipelineException)) {
                        e.getCause().printStackTrace();
                    }

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
        processRawScores();
    }

    /**
     * Gets the candidates in the population.
     */
    private List<List<Candidate>> getInitialCandidates() throws ForcedTerminationException {
        List<List<Candidate>> candidates = new ArrayList<>();
        if (!dummyFitness) {
            // Check if pipe is present
            if (pipe == null) throw new RuntimeException("pipeline setup not complete!");
            // Create list to hold future object associated with Callable
            List<Future<List<Candidate>>> futures = new ArrayList<>();
            // Loop through candidates to produce and submit new tasks
            List<List<Candidate>> matchingCandidateList = this.population.matchingCandidateList();
            for (List<Candidate> candidateList : matchingCandidateList) {
                // Setup callable
                CallableValidationPipelineContainer PipelineContainer = new CallableValidationPipelineContainer(pipe2, pipelineOutputFilePath, candidateList);
                PipelineContainer.setDebug(this.debugPrint);
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
                candidate.setCanBeDeleted(false);
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
        Molecule receptor;
        try {
            receptor = new MolImporter(receptorFilePath.toFile(), "pdb").read();
        } catch (IOException exception) {
            exception.printStackTrace();
            throw new PipelineException("Could not import receptor for exclusion shape");
        }
        ExclusionShape shape = new ExclusionShape(receptor, exclusionShapeTolerance);
        // Get the step for converting 'flat' molecules into multiple 3d conformers
        PipelineStep<Candidate, Candidate> threeDimensionalConverterStep = getConformerStep(conformerCount, anchor, maximumAnchorDistance);
//        PipelineStep<Candidate, Candidate> threeDimensionalConverterStep = new ThreeDimensionalConverterStep(this.pipelineOutputFilePath, conformerCount);
        // Get the step for fixing conformers to an anchor point
//        ConformerFixationStep conformerFixationStep = new ConformerFixationStep(anchor, System.getenv("OBFIT_EXE"));
        PipelineStep<Candidate, Candidate> converterStep;
        if (threeDimensionalConverterStep instanceof CustomConformerStep) {
            converterStep = threeDimensionalConverterStep;
        } else {
            converterStep = threeDimensionalConverterStep.pipe(new ConformerAlignmentStep(anchor, fast_align));
        }
        // Get step that handles scored candidates
        PipelineStep<Candidate, Void> scoredCandidateHandlingStep = new ScoredCandidateHandlingStep(
        );
        // Get the step for energy minimization
        PipelineStep<Candidate, Candidate> energyMinimizationStep = getEnergyMinimizationStep(receptorFilePath, anchor, exclusionShapeTolerance, maximumAnchorDistance, false);
        PipelineStep<Candidate, Candidate> energyAndScoringStep = getEnergyMinimizationStep(receptorFilePath, anchor, exclusionShapeTolerance, maximumAnchorDistance, true);
        // Combine the steps and set the pipe.
        ValidateConformersStep validifyStep = new ValidateConformersStep(anchor, maximumAnchorDistance, clashingConformerCounter, tooDistantConformerCounter, deleteInvalid, shape);
        validifyStep.setDebug(this.debugPrint);
        this.pipe2.add(converterStep.pipe(validifyStep).pipe(energyMinimizationStep));
        this.pipe.add(converterStep.pipe(validifyStep).pipe(energyAndScoringStep).pipe(scoredCandidateHandlingStep));
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
    private PipelineStep<Candidate, Candidate> getEnergyMinimizationStep(Path receptorFile, Path anchorFilePath, double exclusionShapeTolerance, double maximumAnchorDistance, boolean appendScoring) throws PipelineException {
        PipelineStep<Candidate, Candidate> step;
        Molecule receptor;
        try {
            receptor = new MolImporter(receptorFile.toFile(), "pdb").read();
        } catch (IOException exception) {
            exception.printStackTrace();
            throw new PipelineException("Could not import receptor for exclusion shape");
        }
        ExclusionShape shape = new ExclusionShape(receptor, exclusionShapeTolerance);
        ValidateConformersStep validateConformersStep = new ValidateConformersStep(anchorFilePath, maximumAnchorDistance, clashingConformerCounter, tooDistantConformerCounter, deleteInvalid, shape);
        validateConformersStep.setDebug(this.debugPrint);
        switch (this.forceField) {
            case MAB:
                String mol3dExecutable = getEnvironmentVariable("MOL3D_EXE");
                String esprntoExecutable = getEnvironmentVariable("ESPRNTO_EXE");
                step = new MolocEnergyMinimizationStep(
                        receptorFile,
                        mol3dExecutable,
                        // Optimize and score at the same time if the scoring to use is the same as the force field we are using
                        esprntoExecutable, this.scoringOption==ScoringOption.MAB).pipe(validateConformersStep);
                break;
            case SMINA:
                String sminaExecutable = getEnvironmentVariable("SMINA_EXE");
                // Return Smina implementation of the energy minimization step
                SminaEnergyMinimizationStep temp = new SminaEnergyMinimizationStep(
                        receptorFile,
                        sminaExecutable);
                temp.setDebug(this.debugPrint);
                step = temp.pipe(validateConformersStep);
                break;
            default:
                throw new RuntimeException(String.format("Force field '%s' is not implemented", this.forceField.toString()));
        }
        if (appendScoring) {
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
                                esprntoExecutable, true).pipe(new ValidateConformersStep(anchorFilePath, maximumAnchorDistance, clashingConformerCounter, tooDistantConformerCounter, deleteInvalid, shape)));
                    }
                case SMINA:
                    if (this.forceField == ForceField.SMINA) {
                        return step;
                    } else {
                        String sminaExecutable = getEnvironmentVariable("SMINA_EXE");

                        SminaEnergyMinimizationStep sminaStep = new SminaEnergyMinimizationStep(
                                receptorFile,
                                sminaExecutable);
                        sminaStep.setDebug(this.debugPrint);

                        // Return Smina implementation of the energy minimization step
                        return step.pipe(sminaStep).pipe(new ValidateConformersStep(anchorFilePath, maximumAnchorDistance, clashingConformerCounter, tooDistantConformerCounter, deleteInvalid, shape));
                    }
                case SCORPION:
                    String scorpionExecutable = getEnvironmentVariable("FINDPATHS3_EXE");
                    String pythonExecutable = getEnvironmentVariable("PYTHON_EXE");
                    String fixerExecutable = getEnvironmentVariable("FIXER_EXE");
                    String scorpionWrapper = System.getenv("SCORPION_WRAPPER");
                    return step.pipe(new ScorpionScoringStep(receptorFile, scorpionExecutable, fixerExecutable, pythonExecutable, scorpionWrapper));
                default:
                    throw new RuntimeException(String.format("Scoring step '%s' is not implemented", this.scoringOption.toString()));

            }
        } else {
            return step;
        }
    }

    private PipelineStep<Candidate, Candidate> getConformerStep(int conformerCount, Path anchor, double rmsd) {
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
                        this.pipelineOutputFilePath, Paths.get(System.getenv("RDKIT_WRAPPER")), Paths.get(System.getenv("CONFORMER_SCRIPT")), anchor, conformerCount, rmsd);
            case CUSTOM_MACROCYCLE:
                return new CustomConformerStep(
                        this.pipelineOutputFilePath, Paths.get(System.getenv("RDKIT_WRAPPER")), Paths.get(System.getenv("MACROCYCLE_SCRIPT")), anchor, conformerCount, rmsd);
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
     * The way to generate conformers
     */
    public enum ConformerOption {
        CHEMAXON("ChemAxon"),
        MOLOC("Moloc"),
        MACROCYCLE("Macrocycle"),
        CUSTOM("Custom"),
        CUSTOM_MACROCYCLE("Custom Macrocycle");

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
        AFFINITY("affinity"),
        QED("qed"),
        BLOOD_BRAIN_BARRIER("bbb");

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

    public enum BoosterApplication {
        SCORPION_BOOSTER("scorpion"),
        COMBINATORIAL("combinatorial"),
        NONE("none");
        private final String text;

        BoosterApplication(String text) {
            this.text = text;
        }
        public static BoosterApplication fromString(String text) {
            for (BoosterApplication condition : BoosterApplication.values()) {
                if (condition.text.equalsIgnoreCase(text)) {
                    return condition;
                }
            }
            return NONE;
        }
    }
}
