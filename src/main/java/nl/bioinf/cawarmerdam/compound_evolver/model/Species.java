/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class that holds a reaction and how the different reactants (different location in the reaction)
 * map to this reaction.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Species {
    private final List<Integer> reactantIndices;
    private final Reactor reaction;

    /**
     * Constructor of a species instance.
     *
     * @param reactantIndices A list of indices that each corresponds to a list of reactants out of the list of
     *                        lists of reactants. this list of reactants should contain reactants that fit in the
     *                        location of the reaction according to the position of the index in this list.
     * @param reaction        The reactor reaction.
     */
    public Species(List<Integer> reactantIndices, Reactor reaction) throws MisMatchedReactantCount {
        if (reactantIndices.size() != reaction.getReactantCount()) {
            // Throw exception
            throw new MisMatchedReactantCount(reaction.getReactantCount(), reactantIndices.size());
        }
        this.reactantIndices = reactantIndices;
        this.reaction = reaction;
    }

    /**
     * Constructs a list of species that map identically between the reactions and the given list of lists of reactants.
     *
     * @param reactions     The reactor reactions which resemble species.
     * @param reactantCount The amount of lists of reactants received.
     * @return a list of species.
     * @throws MisMatchedReactantCount if the number of reactants does not match the number of reactants required
     *                                 in the reactions.
     */
    public static List<Species> constructSpecies(List<Reactor> reactions, int reactantCount) throws MisMatchedReactantCount {
        ArrayList<Species> species = new ArrayList<>();

        for (Reactor reaction : reactions) {
            species.add(new Species(IntStream.range(0, reactantCount).boxed().collect(Collectors.toList()),
                    reaction));
        }
        return species;
    }

    /**
     * Getter for the reaction in the species.
     *
     * @return the reactor reaction in the species.
     */
    public Reactor getReaction() {
        return reaction;
    }

    /**
     * Getter for the reactant indices. A list of indices that each corresponds to a list
     * of reactants out of the list of lists of reactants. this list of reactants should contain reactants that
     * fit in the location of the reaction according to the position of the index in this list.
     *
     * @return the reactant indices.
     */
    public List<Integer> getReactantIndices() {
        return reactantIndices;
    }

    /**
     * Collects those reactant lists that are used in this species' reaction.
     *
     * @param reactantLists the list of lists of reactants.
     * @return those reactant lists that are used in this species' reaction.
     */
    List<List<Molecule>> getReactantListsSubset(List<List<Molecule>> reactantLists) {
        return this.getReactantIndices()
                .stream()
                .map(reactantLists::get)
                .collect(Collectors.toList());
    }

    /**
     * Collects those reactants that are used in this species' reaction.
     *
     * @param reactants the list of reactants that can form a genotype.
     * @return those reactants that are used in this species' reaction.
     */
    Molecule[] getReactantsSubset(List<String> reactants) {
        return this.getReactantIndices()
                .stream()
                .map(reactants::get)
                .map(s -> {
                    try {
                        return MolImporter.importMol(s);
                    } catch (MolFormatException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .toArray(Molecule[]::new);
    }

    /**
     * Constructs a list of species according to the list of reactor reaction and list of reactant file orders.
     *
     * @param reactions          The list of reactions that each resembles a species.
     * @param reactantsFileOrder The list of reactant file orders, each a list with each integer referring to a
     *                           list of reactants or a specific reactant in a genotype.
     * @return a list of species.
     * @throws MisMatchedReactantCount if the number of reactants does not match the number of reactants required
     *                                 in the reactions.
     */
    public static List<Species> constructSpecies(List<Reactor> reactions, List<List<Integer>> reactantsFileOrder) throws MisMatchedReactantCount {
        ArrayList<Species> species = new ArrayList<>();

        for (int i = 0; i < reactions.size(); i++) {
            Reactor reaction = reactions.get(i);
            species.add(new Species(reactantsFileOrder.get(i), reaction));
        }
        return species;
    }

    /**
     * A method that determines what parts of two species use the same reactants.
     *
     * @param other Another species.
     * @return the indices of reactants that are used in both this species and the other species.
     */
    List<Integer> reactantIndexIntersection(Species other) {
        return this.reactantIndices.stream()
                .distinct()
                .filter(thisIndex -> other.reactantIndices.stream().anyMatch(otherIndex -> otherIndex.equals(thisIndex)))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Species " +
                this.getReactantIndices().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(""));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Species species = (Species) o;
        return Objects.equals(getReactantIndices(), species.getReactantIndices()) &&
                Objects.equals(getReaction(), species.getReaction());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReactantIndices(), getReaction());
    }
}
