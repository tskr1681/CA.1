package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.struc.Molecule;

import java.util.List;
import java.util.stream.Collectors;

public class ReactantFilter {
    private ReactantFilter() {}

    /**
     * Filters a list of reactants by molecular weight.
     * @param reactants the reactants to filter
     * @param weight the maximum weight the reactants can have
     * @return a filtered list of reactants
     */
    public static List<Molecule> filterByWeight(List<Molecule> reactants, double weight) {
        return reactants.stream().filter(molecule -> molecule.getMass() < weight).collect(Collectors.toList());
    }
}
