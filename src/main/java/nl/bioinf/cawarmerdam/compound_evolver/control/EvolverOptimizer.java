package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactionFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;
import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.PipelineException;
import nl.bioinf.cawarmerdam.compound_evolver.util.GAParameters;
import nl.bioinf.cawarmerdam.compound_evolver.util.GenerateCsv;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class EvolverOptimizer {
    private EvolverOptimizer(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath, Path uploadPath, List<Pair<String, List<Object>>> parameterLists, int candidateCount, int repetitions) {
        List<GAParameters> filteredGAParameterVectors = getParameterVectors(parameterLists, candidateCount);

        List<List<Object>> resultsTable = getResultsTable();

        System.out.printf("Sampling %d parameter vectors %d times, %d total%n%n",
                filteredGAParameterVectors.size(),
                repetitions,
                filteredGAParameterVectors.size() * repetitions);
        for (int runIndex = 0; runIndex < filteredGAParameterVectors.size(); runIndex++) {
            GAParameters parameterVector = filteredGAParameterVectors.get(runIndex);
            performRepetitions(
                    reactantLists,
                    reactor,
                    receptorPath,
                    anchorPath,
                    uploadPath,
                    repetitions,
                    resultsTable,
                    runIndex,
                    parameterVector);
        }
        writeOutputCsv(uploadPath, resultsTable);
    }

    private List<GAParameters> getParameterVectors(List<Pair<String, List<Object>>> parameterLists, int candidateCount) {
        List<GAParameters> gaParameterVectors = new ArrayList<>();

        GAParameters baseParameters = getBaseParameters(candidateCount);
        GeneratePermutations(parameterLists, gaParameterVectors, 0, baseParameters);
        return gaParameterVectors.stream()
                .filter(vector -> vector.getCrossoverRate() + vector.getRandomImmigrantRate() <= 1)
                .collect(Collectors.toList());
    }

    private void writeOutputCsv(Path uploadPath, List<List<Object>> resultsTable) {
        Path outputTable = uploadPath.resolve("out.csv");
        System.out.printf("Writing %s", outputTable);
        try (PrintWriter out = new PrintWriter(outputTable.toFile())) {
            out.println(GenerateCsv.generateCsvFile(resultsTable, System.lineSeparator()));
        } catch (FileNotFoundException e) {
            System.out.printf("Failed writing %s%n", outputTable);
            e.printStackTrace();
        }
    }

    private void performRepetitions(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath, Path uploadPath, int repetitions, List<List<Object>> resultsTable, int i, GAParameters parameterVector) {
        for (int repetition = 0; repetition < repetitions; repetition++) {
            String identifier = i + "r" + repetition;
            Path runPath;
            try {
                runPath = makeRunDirectory(uploadPath, identifier);
                writeGAParameters(identifier, parameterVector, runPath);
                CompoundEvolver run = run(reactantLists, reactor, receptorPath, anchorPath, runPath, parameterVector);
                List<List<Double>> scores = run.getFitness();
                writeOutput(resultsTable, parameterVector, identifier, runPath, run, scores);
            } catch (MisMatchedReactantCount | PipelineException | OffspringFailureOverflow | UnSelectablePopulationException | IOException e) {
                System.out.printf("Run %d, repetition %d, failed%n", i, repetition);
                e.printStackTrace();
            }
        }
    }

    private void writeOutput(List<List<Object>> resultsTable, GAParameters parameterVector, String identifier, Path runPath, CompoundEvolver run, List<List<Double>> scores) {
        double bestScore = scores.stream().flatMap(List::stream)
                .mapToDouble(s -> s).max().orElseThrow(NoSuchElementException::new);
        addRun(resultsTable, parameterVector, identifier, run, bestScore);
        String csvData = GenerateCsv.generateCsvFile(scores, System.lineSeparator());
        String csvFileName = String.format("%s-scores.csv", identifier);
        Path csvFilePath = runPath.resolve(csvFileName);
        System.out.printf("Writing %s", csvFileName);
        try (PrintWriter out = new PrintWriter(csvFilePath.toFile())) {
            out.println(csvData);
        } catch (FileNotFoundException e) {
            System.out.printf("Failed writing %s%n", csvFileName);
            e.printStackTrace();
        }
    }

    private List<List<Object>> getResultsTable() {
        List<List<Object>> table = new ArrayList<>();
        table.add(Arrays.asList(new Object[]{
                "id",
                "score",
                "duration",
                "population-size",
                "selection-size",
                "mutation-rate",
                "selection-method",
                "mutation-method",
                "crossover-rate",
                "elitist-rate",
                "random-immigrant-rate"
        }));
        return table;
    }

    private void addRun(List<List<Object>> runs, GAParameters parameterVector, String identifier, CompoundEvolver run, double bestScore) {
        runs.add(Arrays.asList(new Object[]{
                identifier,
                bestScore,
                run.getDuration(),
                parameterVector.getPopulationSize(),
                parameterVector.getSelectionRate(),
                parameterVector.getMutationRate(),
                parameterVector.getSelectionMethod(),
                parameterVector.getMutationMethod(),
                parameterVector.getCrossoverRate(),
                parameterVector.getElitistRate(),
                parameterVector.getRandomImmigrantRate(),
        }));
    }

    /**
     * Main for optimization using the command line interface
     *
     * @param args An array of strings being the command line input
     */
    public static void main(String[] args) throws Exception {
        // Load reactor from argument
        Reactor reactor = ReactionFileHandler.loadReaction(args[0]);
        // Load molecules
        String[] reactantFiles = Arrays.copyOfRange(args, 1, args.length - 6);
        List<List<Molecule>> reactantLists = ReactantFileHandler.loadMolecules(reactantFiles);
        // Load receptor molecule path
        Path receptorPath = Paths.get(args[args.length - 6]);
        // Load anchor molecule path
        Path anchorPath = Paths.get(args[args.length - 5]);
        // Construct the initial population
        Path uploadPath = Paths.get(args[args.length - 4]);
        List<Pair<String, List<Object>>> parameterLists = parseParameterLists(args[args.length - 3]);
        int candidateCount = Integer.parseInt(args[args.length - 2]);
        int repetitions = Integer.parseInt(args[args.length - 1]);
        // Construct the initial population
        new EvolverOptimizer(reactantLists, reactor, receptorPath, anchorPath, uploadPath, parameterLists, candidateCount, repetitions);
    }

    private static List<Pair<String, List<Object>>> parseParameterLists(String arg) throws java.io.IOException {
        // Parse JSON to java
        HashMap parameterRanges =
                new ObjectMapper().readValue(new FileReader(arg), HashMap.class);

        List<Pair<String, List<Object>>> parameterLists = new ArrayList<>();

        Iterator it = parameterRanges.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            List<Object> value = (List<Object>)  pair.getValue();
            parameterLists.add(new ImmutablePair<>(pair.getKey().toString(), value));
            it.remove(); // avoids a ConcurrentModificationException
        }
        return parameterLists;
    }

    private Path makeRunDirectory(Path uploadPath, String i) throws IOException {
        Path runPath = uploadPath.resolve("run_" + i);
        Files.createDirectories(runPath.toAbsolutePath());
        return runPath;
    }

    private void writeGAParameters(String i, GAParameters parameterVector, Path runPath) throws IOException {
        Path paramsFilePath = runPath.resolve(String.format("%s-params.json", i));
        if (!Files.exists(paramsFilePath, LinkOption.NOFOLLOW_LINKS))
            Files.createFile(paramsFilePath);
        //Object to JSON in file
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writeValue(paramsFilePath.toFile(), parameterVector);
    }

    private GAParameters getBaseParameters(int candidateCount) {
        GAParameters gaParameters = new GAParameters();
        gaParameters.setForceField(CompoundEvolver.ForceField.SMINA);
        gaParameters.setTerminationCondition(CompoundEvolver.TerminationCondition.MAXIMUM_CANDIDATE_COUNT);
        gaParameters.setFitnessMeasure(CompoundEvolver.FitnessMeasure.LIGAND_EFFICIENCY);
        gaParameters.setSelectionMethod(Population.SelectionMethod.TOURNAMENT_SELECTION);
        gaParameters.setMutationMethod(Population.MutationMethod.DISTANCE_INDEPENDENT);
        gaParameters.setMaxGenerations(Integer.MAX_VALUE);
        gaParameters.setSpeciesDeterminationMethod(Population.SpeciesDeterminationMethod.FIXED);
        gaParameters.setInterspeciesCrossoverMethod(Population.InterspeciesCrossoverMethod.NONE);
        gaParameters.setMaxAnchorMinimizedRmsd(2.0);
        gaParameters.setTargetCandidateCount(candidateCount);
        return gaParameters;
    }

    private void GeneratePermutations(List<Pair<String, List<Object>>> ParameterLists, List<GAParameters> parameterVectors, int depth, GAParameters current) {
        if (depth == ParameterLists.size()) {
            parameterVectors.add(current);
            return;
        }

        Pair<String, List<Object>> parameterRange = ParameterLists.get(depth);
        for (int i = 0; i < parameterRange.getValue().size(); ++i) {
            GAParameters.set(current, parameterRange.getKey(), parameterRange.getValue().get(i));
            GeneratePermutations(ParameterLists, parameterVectors, depth + 1, SerializationUtils.clone(current));
        }
    }

    private CompoundEvolver run(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath, Path uploadPath, GAParameters parameters) throws MisMatchedReactantCount, PipelineException, OffspringFailureOverflow, UnSelectablePopulationException {
        List<Species> species = Species.constructSpecies(Collections.singletonList(reactor), reactantLists.size());
        Population population = new Population(
                reactantLists,
                species,
                parameters.getSpeciesDeterminationMethod(),
                parameters.getPopulationSize());

        population.initializeAlleleSimilaritiesMatrix();
        population.setMutationMethod(parameters.getMutationMethod());
        population.setSelectionMethod(parameters.getSelectionMethod());
        population.setSelectionFraction(parameters.getSelectionRate());
        population.setMutationRate(parameters.getMutationRate());
        population.setCrossoverRate(parameters.getCrossoverRate());
        population.setRandomImmigrantRate(parameters.getRandomImmigrantRate());
        population.setElitismRate(parameters.getElitistRate());
        population.setMaxAnchorMinimizedRmsd(parameters.getMaxAnchorMinimizedRmsd());
        population.setInterspeciesCrossoverMethod(parameters.getInterspeciesCrossoverMethod());
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, new CommandLineEvolutionProgressConnector());
        compoundEvolver.setDummyFitness(false);
        compoundEvolver.setCleanupFiles(true);
        compoundEvolver.setForceField(parameters.getForceField());
        compoundEvolver.setFitnessMeasure(parameters.getFitnessMeasure());
        compoundEvolver.setTerminationCondition(parameters.getTerminationCondition());
        compoundEvolver.setMaxNumberOfGenerations(parameters.getMaxGenerations());
        compoundEvolver.setupPipeline(uploadPath, receptorPath, anchorPath);
        compoundEvolver.setTargetCandidateCount(parameters.getTargetCandidateCount());

        // Evolve compounds
        compoundEvolver.evolve();
        return compoundEvolver;
    }
}
