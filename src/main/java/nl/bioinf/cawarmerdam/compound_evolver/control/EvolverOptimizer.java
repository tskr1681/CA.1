package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactionFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;
import nl.bioinf.cawarmerdam.compound_evolver.util.GenerateCsv;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EvolverOptimizer {
    private EvolverOptimizer(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath, Path uploadPath) {
        // Get range
        List<Integer> permutations = IntStream.iterate(5, n -> n + 5)
                .limit(100)
                .boxed()
                .collect(Collectors.toList());

//        GeneratePermutations(new ArrayList<>(), permutations, 0, "");
        for (int i = 0; i < permutations.size(); i++) {
            Integer permutation = permutations.get(i);
            try {
                List<List<Double>> run = run(reactantLists, reactor, receptorPath, anchorPath, uploadPath, permutation);
                String csvData = GenerateCsv.generateCsvFile(run, System.lineSeparator());
                try (PrintWriter out = new PrintWriter(String.format("%d-scores.csv", i))) {
                    out.println(csvData);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (MisMatchedReactantCount | ReactionException | PipelineException | OffspringFailureOverflow e) {
                e.printStackTrace();
            }
        }
    }

    private void GeneratePermutations(List<List<Character>> Lists, List<String> result, int depth, String current)
    {
        if(depth == Lists.size())
        {
            result.add(current);
            return;
        }

        for(int i = 0; i < Lists.get(depth).size(); ++i)
        {
            GeneratePermutations(Lists, result, depth + 1, current + Lists.get(depth).get(i));
        }
    }

    private List<List<Double>> run(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath, Path uploadPath, int initialGenerationSize) throws MisMatchedReactantCount, ReactionException, PipelineException, OffspringFailureOverflow {
        Population population = new Population(reactantLists, reactor, initialGenerationSize);
        population.initializeAlleleSimilaritiesMatrix();
        population.setMutationMethod(Population.MutationMethod.DISTANCE_DEPENDENT);
        population.setSelectionMethod(Population.SelectionMethod.TRUNCATED_SELECTION);
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, new CommandLineEvolutionProgressConnector());
        compoundEvolver.setupPipeline(uploadPath, receptorPath, anchorPath);
        compoundEvolver.setDummyFitness(false);
        // Evolve compounds
        compoundEvolver.evolve();
        return compoundEvolver.getFitness();
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
        String[] reactantFiles = Arrays.copyOfRange(args, 1, args.length - 3);
        List<List<Molecule>> reactantLists = ReactantFileHandler.loadMolecules(reactantFiles);
        // Load receptor molecule path
        Path receptorPath = Paths.get(args[args.length - 3]);
        // Load anchor molecule path
        Path anchorPath = Paths.get(args[args.length - 2]);
        // Construct the initial population
        Path uploadPath = Paths.get(args[args.length - 1]);
        // Construct the initial population
        new EvolverOptimizer(reactantLists, reactor, receptorPath, anchorPath, uploadPath);
    }
}
