package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.SearchException;
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

    private static boolean filterMoleculeBySmarts(Molecule m, String[] smarts) {
        MolSearch s = new MolSearch();
        s.setTarget(m);
        for (String smart : smarts) {
            s.setQuery(smart);
            try {
                if (s.getMatchCount() > 0) {
                    return false;
                }
            } catch (SearchException ignored) {
            }
        }
        return true;
    }

    public static List<Molecule> filterBySmarts(List<Molecule> reactants, String[] smarts) {
        return reactants.stream().filter(molecule -> filterMoleculeBySmarts(molecule, smarts)).collect(Collectors.toList());
    }
}
