package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Species {
    private List<Integer> reactantIndices;
    private Reactor reaction;

    Species(List<Integer> reactantIndices, Reactor reaction) {
        this.reactantIndices = reactantIndices;
        this.reaction = reaction;
    }

    public static List<Species> constructSpecies(List<Reactor> reactions, int reactantCount) {
        ArrayList<Species> species = new ArrayList<>();

        for (Reactor reaction : reactions) {
            species.add(new Species(IntStream.range(0, reactantCount).boxed().collect(Collectors.toList()),
                    reaction));
        }
        return species;
    }

    public Reactor getReaction() {
        return reaction;
    }

    List<Integer> getReactantIndices() {
        return reactantIndices;
    }

    List<List<Molecule>> getReactantListsSubset(List<List<Molecule>> reactantLists) {
        return this.getReactantIndices()
                .stream()
                .map(reactantLists::get)
                .collect(Collectors.toList());
    }

    Molecule[] getReactantsSubset(List<Molecule> reactants) {
        return this.getReactantIndices()
                .stream()
                .map(reactants::get)
                .toArray(Molecule[]::new);
    }

    public static List<Species> constructSpecies(List<Reactor> reactions, List<List<Integer>> reactantsFileOrder) {
        ArrayList<Species> species = new ArrayList<>();

        for (int i = 0; i < reactions.size(); i++) {
            Reactor reaction = reactions.get(i);
            species.add(new Species(reactantsFileOrder.get(i), reaction));
        }
        return species;
    }

    @Override
    public String toString() {
        return "Species " +
                this.getReactantIndices().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(""));
    }
}
