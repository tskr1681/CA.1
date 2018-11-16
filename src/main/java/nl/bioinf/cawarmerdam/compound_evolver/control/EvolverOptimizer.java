package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactionFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.model.CommandLineEvolutionProgressConnector;
import nl.bioinf.cawarmerdam.compound_evolver.model.MisMatchedReactantCount;
import nl.bioinf.cawarmerdam.compound_evolver.model.PipeLineException;
import nl.bioinf.cawarmerdam.compound_evolver.model.Population;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EvolverOptimizer {
    private EvolverOptimizer(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath) {
        ArrayList<String> permutations = new ArrayList<>();
        GeneratePermutations(new ArrayList<>(), permutations, 0, "");
        for (String permutation : permutations) {
            try {
                run(reactantLists, reactor, receptorPath, anchorPath, 50);
            } catch (MisMatchedReactantCount | ReactionException | PipeLineException misMatchedReactantCount) {
                misMatchedReactantCount.printStackTrace();
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

    private void run(List<List<Molecule>> reactantLists, Reactor reactor, Path receptorPath, Path anchorPath, int initialGenerationSize) throws MisMatchedReactantCount, ReactionException, PipeLineException {
        Population population = new Population(reactantLists, reactor, initialGenerationSize);
        population.initializeAlleleSimilaritiesMatrix();
        population.setMutationMethod(Population.MutationMethod.DISTANCE_DEPENDENT);
        population.setSelectionMethod(Population.SelectionMethod.TRUNCATED_SELECTION);
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(population, new CommandLineEvolutionProgressConnector());
        compoundEvolver.setupPipeline(Paths.get("C:\\Users\\P286514\\uploads"), receptorPath, anchorPath);
        compoundEvolver.setDummyFitness(false);
        // Evolve compounds
        compoundEvolver.evolve();
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
        Path receptorPath = Paths.get(args[args.length - 2]);
        // Load anchor molecule path
        Path anchorPath = Paths.get(args[args.length - 1]);
        // Construct the initial population
        new EvolverOptimizer(reactantLists, reactor, receptorPath, anchorPath);
    }
}
