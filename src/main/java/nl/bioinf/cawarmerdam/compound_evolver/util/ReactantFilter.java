package nl.bioinf.cawarmerdam.compound_evolver.util;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.SearchException;

import java.util.List;
import java.util.stream.Collectors;

public class ReactantFilter {
    private ReactantFilter() {
    }

    /**
     * Filters a list of reactants by molecular weight.
     *
     * @param reactants the reactants to filter
     * @param weight    the maximum weight the reactants can have
     * @return a filtered list of reactants
     */
    public static List<String> filterByWeight(List<String> reactants, double weight) {
        return reactants.stream().filter(molecule -> {
            try {
                return MolImporter.importMol(molecule).getMass() < weight;
            } catch (MolFormatException e) {
                e.printStackTrace();
                return false;
            }
        }).collect(Collectors.toList());
    }

    private static boolean filterMoleculeBySmarts(String m, String[] smarts) {
        MolSearch s = new MolSearch();
        try {
            s.setTarget(MolImporter.importMol(m));

            for (String smart : smarts) {
                if (!smart.equals("")) {
                    s.setQuery(smart);
                    try {
                        if (s.getMatchCount() > 0) {
                            return false;
                        }
                    } catch (SearchException ignored) {
                    }
                }
            }
            return true;
        } catch (MolFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> filterBySmarts(List<String> reactants, String[] smarts) {
        return reactants.stream().filter(molecule -> filterMoleculeBySmarts(molecule, smarts)).collect(Collectors.toList());
    }
}
