/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

/**
 * A class that holds the location of a point in a 3D grid.
 * It is just intended for storage.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
class GridLocation {
    private final int x;
    private final int y;
    private final int z;

    /**
     * Constructor for a grid location.
     *
     * @param x The x index.
     * @param y The y index.
     * @param z The z index.
     */
    GridLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Getter for the x index.
     *
     * @return the x index.
     */
    int getX() {
        return x;
    }

    /**
     * Getter for the y index.
     *
     * @return the y index.
     */
    int getY() {
        return y;
    }

    /**
     * Getter for the z index.
     *
     * @return the z index.
     */
    int getZ() {
        return z;
    }
}
