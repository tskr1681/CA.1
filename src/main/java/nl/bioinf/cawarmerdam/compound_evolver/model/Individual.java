package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.Molecule;

public class Individual {

    private Molecule[] genotype;
    private Molecule phenotype;
    private float score;

    public Individual(Molecule[] genotype, Molecule phenotype) {
        this.genotype = genotype;
        this.phenotype = phenotype;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}
