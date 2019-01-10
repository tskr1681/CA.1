/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

/**
 * Exception for a reactant count unequal to the received amount of reactant sets.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class MisMatchedReactantCount extends Exception {
    private int reactantCount;
    private int listSize;

    /**
     * Constructor for the exception that creates a custom message.
     *
     * @param reactantCount The amount of reactants the reaction expects.
     * @param listSize The amount of reactant sets that was provided.
     */
    public MisMatchedReactantCount(int reactantCount, int listSize) {
        super(String.format("%s reactants expected by the reaction. received %s", reactantCount, listSize));
        this.reactantCount = reactantCount;
        this.listSize = listSize;
    }

    /**
     * Getter for the amount of reactants the reaction expects.
     *
     * @return the amount of reactants the reaction expects.
     */
    public int getReactantCount() {
        return reactantCount;
    }

    /**
     * Getter for the amount of reactant sets that was provided.
     *
     * @return the amount of reactant sets that was provided.
     */
    public int getListSize() {
        return listSize;
    }
}
