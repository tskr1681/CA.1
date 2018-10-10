package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.descriptors.*;
import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static java.util.stream.DoubleStream.of;

public class Population implements Iterable<Candidate> {

    private final Reactor reaction;
    private final List<List<Molecule>> reactantLists;
    private double[][][] alleleDistances;
    private List<Candidate> candidateList;
    private int populationSize;

    public Population(List<List<Molecule>> reactantLists, Reactor reaction, int initialGenerationSize) {
        this.reaction = reaction;
        this.reactantLists = reactantLists;
        this.populationSize = initialGenerationSize;
        this.candidateList = new RandomCompoundReactor(this.reaction, initialGenerationSize).execute(this.reactantLists);
        alleleDistances = new double[reactantLists.size()][][];
        computeAlleleDistances();
    }

    private void computeAlleleDistances() {
        for (int i1 = 0; i1 < reactantLists.size(); i1++) {
            List<Molecule> reactants = reactantLists.get(i1);
            alleleDistances[i1] = new double[reactants.size()][reactants.size()];
            for (int i = 0; i < reactants.size(); i++) {
                for (int j = 0; j < i; j++) {
                    double tanimoto = (double) 1 - getTanimoto(reactants, i, j);
                    alleleDistances[i1][i][j] = tanimoto;
                    alleleDistances[i1][j][i] = tanimoto;
                }
                alleleDistances[i1][i][i] = 1;
            }
            for (int i = 0; i < alleleDistances[i1].length; i++) {
                for (int j = 0; j < alleleDistances[i1][i].length; j++) {
                    System.out.print(String.format("| %.2f ", alleleDistances[i1][i][j]));
                }
                System.out.println();
            }
            System.out.println("------------------------------------------------");
        }
    }

    private float getTanimoto(List<Molecule> reactants, int i, int j) {
        ChemicalFingerprint firstFingerprint = new ChemicalFingerprint( new CFParameters() );
        ChemicalFingerprint secondFingerprint = new ChemicalFingerprint( new CFParameters() );
        try {
            firstFingerprint.generate(reactants.get(i));
            secondFingerprint.generate(reactants.get(j));
            return firstFingerprint.getTanimoto(secondFingerprint);
        } catch (MDGeneratorException e) {
            return 0;
        }
    }

    public void produceOffspring() {
        produceOffspring(this.populationSize);
    }

    public void produceOffspring(int offspringSize) {
        // Create list of offspring
        List<Candidate> offspring = new ArrayList<>();
        // Perform crossing over
        // Shuffle parents
        Collections.shuffle(this.candidateList);
        // Select parents
//        candidateList = this.fitnessProportionateSelection();
        // Loop to fill offspring list to offspring size
        for (int i = 0; offspring.size() < offspringSize; i++) {
            System.out.println("i = " + i);
            // Get the recombined genome by crossing over
            List<Integer> recombinedGenome = getRecombinedGenome(i);
            // Mutate the recombined genome
            mutate(recombinedGenome);
            try {
                Molecule[] reactants = getReactantsFromIndices(recombinedGenome);
                reaction.setReactants(reactants);
                Molecule[] products;
                if ((products = reaction.react()) != null) {
                    offspring.add(new Candidate(recombinedGenome, products[0]));
                }
            } catch (ReactionException e) {
                e.printStackTrace();
            }
        }
        // Possibly introduce new individuals
    }

    private Molecule[] getReactantsFromIndices(List<Integer> recombinedGenome) {
        return IntStream.range(0, reactantLists.size())
                .mapToObj(i -> reactantLists.get(i).get(recombinedGenome.get(i)))
                .toArray(Molecule[]::new);
    }

    private List<Candidate> fitnessProportionateSelection() {
        // Assert that the candidates have a score
        assert (candidateList.size() > 0) && candidateList.get(0).getScore() != null;
        List<Candidate> selectedParents = new ArrayList<>();
        for (int i = 0; i < candidateList.size() * 0.5; i++) {
            selectedParents.add(candidateList.get(rouletteSelect()));
        }
        return selectedParents;
    }

    private int rouletteSelect() {
        return makeWeightedChoice(candidateList.stream()
                .map(Candidate::getScore)
                .mapToDouble(v -> v)
                .toArray());
    }

    private int makeWeightedChoice(double[] weights) {
        double weightsSum = 0;
        // Get sum of fitness scores
        weightsSum = DoubleStream.of(weights).sum();
        // get a random value
        double value = new Random().nextDouble() * weightsSum;
        // locate the random value based on the weights
        for (int i = 0; i < weights.length; i++) {
            value -= weights[i];
            if (value < 0) return i;
        }
        // when rounding errors occur, we return the last item's index
        return weights.length - 1;
    }

    private List<Integer> getRecombinedGenome(int i) {
        // Get the index of two parents to perform crossover between the two
        int firstParentIndex = i % this.candidateList.size();
        int otherParentIndex = (i + 1) % this.candidateList.size();
        // Get the two parents
        Candidate firstParent = this.candidateList.get(firstParentIndex);
        Candidate otherParent = this.candidateList.get(otherParentIndex);
        // Perform crossover between the two
        return firstParent.crossover(otherParent);
    }

    private void mutate(List<Integer> genome) {
        // Loop through each allele
        String format = "%10s -> %10s";
        System.out.println(String.format(format, "curr", "new"));
        for (int i = 0; i < genome.size(); i++) {
            int allele = genome.get(i);
//            List<String> printAble = new ArrayList<>();
//            for (double dist : alleleDistances[i][allele]) {
//                printAble.add(String.format("%.2f", dist));
//            }
//            System.out.println(String.join(" | ", printAble));
            int reactantIndex = makeWeightedChoice(alleleDistances[i][allele]);
            genome.set(i, reactantIndex);
            System.out.println(String.format("%3s (%1.2f) -> %3s (%1.2f)", allele, alleleDistances[i][allele][allele] / DoubleStream.of(alleleDistances[i][allele]).sum(), reactantIndex,  alleleDistances[i][allele][reactantIndex] / DoubleStream.of(alleleDistances[i][allele]).sum()));
        }
    }

    @Override
    public Iterator<Candidate> iterator() {
        return this.candidateList.iterator();
    }
}
