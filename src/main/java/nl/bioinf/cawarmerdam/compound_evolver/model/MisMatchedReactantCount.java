package nl.bioinf.cawarmerdam.compound_evolver.model;

public class MisMatchedReactantCount extends Exception {
    private int reactantCount;
    private int listSize;

    public MisMatchedReactantCount(int reactantCount, int listSize) {
        super(String.format("%s reactants expected by the reaction. received %s", reactantCount, listSize));
        this.reactantCount = reactantCount;
        this.listSize = listSize;
    }

    public int getReactantCount() {
        return reactantCount;
    }

    public int getListSize() {
        return listSize;
    }
}
