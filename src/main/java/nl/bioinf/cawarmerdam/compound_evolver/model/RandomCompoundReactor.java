package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomCompoundReactor
        implements PipelineStep<List<List<Molecule>>, List<Candidate>> {

    private Reactor reactor;
    private int maxSamples;

    public RandomCompoundReactor(Reactor reactor, int maxSamples)
    {
        this.reactor = reactor;
        this.maxSamples = maxSamples;
    }

    @Override
    public List<Candidate> execute(List<List<Molecule>> reactantLists) {
        try {
            return randReact(reactantLists);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<Candidate> randReact(List<List<Molecule>> reactantLists) throws Exception {
        // Amount of products generated
        int nSampled = 0;
        Random random = new Random();
        List<Candidate> candidates = new ArrayList<>();
        // Try to generate products while the number of products generated is
        // lower than the maximum number of products wanted
        while (nSampled < maxSamples) {
            Iterator<List<Molecule>> iterator = reactantLists.iterator();
            List<Molecule> reactants = new ArrayList<>();
            List<Integer> indexGenome = new ArrayList<>();
            Molecule[] products;
            while (iterator.hasNext()) {
                // Get random reactants for a single reaction
                List<Molecule> map = iterator.next();
                int index = random.nextInt(map.size());
                indexGenome.add(index);
                Molecule reactant = map.get(index);
                reactants.add(reactant);
            }
            Molecule[] molecules = reactants.toArray(new Molecule[reactants.size()]);
            // Set the reactants
            reactor.setReactants(molecules);
            // Add the product and count the product if it can be made
            if ((products = reactor.react()) != null) {
                candidates.add(new Candidate(indexGenome, products[0]));
                nSampled++;
            }
        }
        return candidates;

    }
}
