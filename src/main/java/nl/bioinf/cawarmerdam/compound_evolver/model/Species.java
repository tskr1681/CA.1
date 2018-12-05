package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;

import java.util.List;
import java.util.stream.Collectors;

public class Species {
    private List<Integer> reactantIndices;
    private Reactor reaction;

    Species(List<Integer> reactantIndices, Reactor reaction) {
        this.reactantIndices = reactantIndices;
        this.reaction = reaction;
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
}
