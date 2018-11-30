package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.reaction.ReactionException;
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
import org.antlr.v4.misc.OrderedHashMap;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class EvolverOptimizer {
    private EvolverOptimizer(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath, Path uploadPath, List<Pair<String, List<Object>>> parameterLists) {
        // Get range
        List<GAParameters> gaParameterVectors = new ArrayList<>();

        GAParameters baseParameters = getBaseParameters();
        GeneratePermutations(parameterLists, gaParameterVectors, 0, baseParameters);
        List<GAParameters> filteredGAParameterVectors = gaParameterVectors.stream()
                .filter(vector -> vector.getCrossoverRate() + vector.getRandomImmigrantRate() <= 1)
                .collect(Collectors.toList());

        List<List<Object>> durations = new ArrayList<>();

        System.out.printf("Sampling %d parameter vectors%n%n", filteredGAParameterVectors.size());
        for (int i = 0; i < filteredGAParameterVectors.size(); i++) {
            GAParameters parameterVector = filteredGAParameterVectors.get(i);
            try {
                Path runPath = makeRunDirectory(uploadPath, i);
                writeGAParameters(i, parameterVector, runPath);
                CompoundEvolver run = run(reactantLists, reactor, receptorPath, anchorPath, runPath, parameterVector);
                List<List<Double>> scores = run.getFitness();
                double bestScore = scores.stream().flatMap(List::stream)
                        .mapToDouble(s -> s).max().orElseThrow(NoSuchElementException::new);
                durations.add(Arrays.asList(new Object[]{i, bestScore, run.getDuration()}));
                String csvData = GenerateCsv.generateCsvFile(scores, System.lineSeparator());
                String csvFileName = String.format("%d-scores.csv", i);
                Path csvFilePath = runPath.resolve(csvFileName);
                System.out.printf("Writing %s", csvFileName);
                try (PrintWriter out = new PrintWriter(csvFilePath.toFile())) {
                    out.println(csvData);
                } catch (FileNotFoundException e) {
                    System.out.printf("Failed writing %s%n", csvFileName);
                    e.printStackTrace();
                }
            } catch (MisMatchedReactantCount | ReactionException | PipelineException | OffspringFailureOverflow | UnSelectablePopulationException | IOException e) {
                System.out.printf("Run %d failed%n", i);
                e.printStackTrace();
            }
        }
        Path outputTable = uploadPath.resolve("out.csv");
        System.out.printf("Writing %s", outputTable);
        try (PrintWriter out = new PrintWriter(outputTable.toFile())) {
            out.println(GenerateCsv.generateCsvFile(durations, System.lineSeparator()));
        } catch (FileNotFoundException e) {
            System.out.printf("Failed writing %s%n", outputTable);
            e.printStackTrace();
        }
    }

    private Path makeRunDirectory(Path uploadPath, int i) throws IOException {
        Path runPath = uploadPath.resolve("run_" + i);
        Files.createDirectories(runPath.toAbsolutePath());
        return runPath;
    }

    private void writeGAParameters(int i, GAParameters parameterVector, Path runPath) throws IOException {
        Path paramsFilePath = runPath.resolve(String.format("%d-params.json", i));
        if (!Files.exists(paramsFilePath, LinkOption.NOFOLLOW_LINKS))
            Files.createFile(paramsFilePath);
        //Object to JSON in file
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writeValue(paramsFilePath.toFile(), parameterVector);
    }

    private GAParameters getBaseParameters() {
        GAParameters gaParameters = new GAParameters();
        gaParameters.setForceField(CompoundEvolver.ForceField.SMINA);
        gaParameters.setTerminationCondition(CompoundEvolver.TerminationCondition.DURATION);
        gaParameters.setFitnessMeasure(CompoundEvolver.FitnessMeasure.LIGAND_EFFICIENCY);
        gaParameters.setSelectionMethod(Population.SelectionMethod.TRUNCATED_SELECTION);
        gaParameters.setMutationMethod(Population.MutationMethod.DISTANCE_INDEPENDENT);
        gaParameters.setMaxGenerations(Integer.MAX_VALUE);
        gaParameters.setMaxAnchorMinimizedRmsd(2.0);
        gaParameters.setMaximumAllowedDuration(60000*5);
        return gaParameters;
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
        String[] reactantFiles = Arrays.copyOfRange(args, 1, args.length - 4);
        List<List<Molecule>> reactantLists = ReactantFileHandler.loadMolecules(reactantFiles);
        // Load receptor molecule path
        Path receptorPath = Paths.get(args[args.length - 4]);
        // Load anchor molecule path
        Path anchorPath = Paths.get(args[args.length - 3]);
        // Construct the initial population
        Path uploadPath = Paths.get(args[args.length - 2]);
        List<Pair<String, List<Object>>> parameterLists = parseParameterLists(args[args.length - 1]);
        // Construct the initial population
        new EvolverOptimizer(reactantLists, reactor, receptorPath, anchorPath, uploadPath, parameterLists);
    }

    private static List<Pair<String, List<Object>>> parseParameterLists(String arg) throws java.io.IOException {
        // Parse JSON to java
        HashMap parameterRanges =
                new ObjectMapper().readValue(new FileReader(arg), HashMap.class);

        List<Pair<String, List<Object>>> parameterLists = new ArrayList<>();

        Iterator it = parameterRanges.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            List<Object> value = (List<Object>) pair.getValue();
            parameterLists.add(new ImmutablePair<>(pair.getKey().toString(), value));
            it.remove(); // avoids a ConcurrentModificationException
        }
        return parameterLists;
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

    private CompoundEvolver run(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath, Path uploadPath, GAParameters parameters) throws MisMatchedReactantCount, ReactionException, PipelineException, OffspringFailureOverflow, UnSelectablePopulationException {
        Population population = new Population(reactantLists, reactor, parameters.getGenerationSize());
        population.initializeAlleleSimilaritiesMatrix();
        population.setMutationMethod(parameters.getMutationMethod());
        population.setSelectionMethod(parameters.getSelectionMethod());
        population.setSelectionFraction(parameters.getSelectionRate());
        population.setMutationRate(parameters.getMutationRate());
        population.setCrossoverRate(parameters.getCrossoverRate());
        population.setRandomImmigrantRate(parameters.getRandomImmigrantRate());
        population.setElitismRate(parameters.getElitistRate());
        population.setMaxAnchorMinimizedRmsd(parameters.getMaxAnchorMinimizedRmsd());
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, new CommandLineEvolutionProgressConnector());
        compoundEvolver.setDummyFitness(false);
        compoundEvolver.setForceField(parameters.getForceField());
        compoundEvolver.setFitnessMeasure(parameters.getFitnessMeasure());
        compoundEvolver.setTerminationCondition(parameters.getTerminationCondition());
        compoundEvolver.setMaxNumberOfGenerations(parameters.getMaxGenerations());
        compoundEvolver.setupPipeline(uploadPath, receptorPath, anchorPath);
        compoundEvolver.setMaximumAllowedDuration(parameters.getMaximumAllowedDuration());

        // Evolve compounds
        compoundEvolver.evolve();
        return compoundEvolver;
    }
}
