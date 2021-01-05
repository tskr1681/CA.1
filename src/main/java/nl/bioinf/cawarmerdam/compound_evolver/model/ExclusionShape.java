/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.DPoint3;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that defines how an exclusion shape instance is defined.
 * The implementation of the exclusive shape functionality is based on the
 * <a href="https://pubs.acs.org/doi/abs/10.1021/ci200097m">Pharmit</a> implementation of exclusive shape.
 * <a href="https://sourceforge.net/p/pharmit">pharmit source code</a>
 * <p>
 * Source for the Vanderwaals radii is <a href="https://github.com/openbabel/openbabel/blob/master/src/elementtable.h">OpenBabel</a>
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ExclusionShape {
    private static final double DEFAULT_PROBE_SIZE = 1.4;
    private static final double RESOLUTION = 0.5;

    private static final Map<Integer, Double> ELEMENT_VDW_RADII = new HashMap<>();

    static {
        ELEMENT_VDW_RADII.put(0, 0d);
        ELEMENT_VDW_RADII.put(1, 1.1d);
        ELEMENT_VDW_RADII.put(2, 1.4d);
        ELEMENT_VDW_RADII.put(3, 1.81d);
        ELEMENT_VDW_RADII.put(4, 1.53d);
        ELEMENT_VDW_RADII.put(5, 1.92d);
        ELEMENT_VDW_RADII.put(6, 1.7d);
        ELEMENT_VDW_RADII.put(7, 1.55d);
        ELEMENT_VDW_RADII.put(8, 1.52d);
        ELEMENT_VDW_RADII.put(9, 1.47d);
        ELEMENT_VDW_RADII.put(10, 1.54d);
        ELEMENT_VDW_RADII.put(11, 2.27d);
        ELEMENT_VDW_RADII.put(12, 1.73d);
        ELEMENT_VDW_RADII.put(13, 1.84d);
        ELEMENT_VDW_RADII.put(14, 2.1d);
        ELEMENT_VDW_RADII.put(15, 1.8d);
        ELEMENT_VDW_RADII.put(16, 1.8d);
        ELEMENT_VDW_RADII.put(17, 1.75d);
        ELEMENT_VDW_RADII.put(18, 1.88d);
        ELEMENT_VDW_RADII.put(19, 2.75d);
        ELEMENT_VDW_RADII.put(20, 2.31d);
        ELEMENT_VDW_RADII.put(21, 2.3d);
        ELEMENT_VDW_RADII.put(22, 2.15d);
        ELEMENT_VDW_RADII.put(23, 2.05d);
        ELEMENT_VDW_RADII.put(24, 2.05d);
        ELEMENT_VDW_RADII.put(25, 2.05d);
        ELEMENT_VDW_RADII.put(26, 2.05d);
        ELEMENT_VDW_RADII.put(27, 2d);
        ELEMENT_VDW_RADII.put(28, 2d);
        ELEMENT_VDW_RADII.put(29, 2d);
        ELEMENT_VDW_RADII.put(30, 2.1d);
        ELEMENT_VDW_RADII.put(31, 1.87d);
        ELEMENT_VDW_RADII.put(32, 2.11d);
        ELEMENT_VDW_RADII.put(33, 1.85d);
        ELEMENT_VDW_RADII.put(34, 1.9d);
        ELEMENT_VDW_RADII.put(35, 1.83d);
        ELEMENT_VDW_RADII.put(36, 2.02d);
        ELEMENT_VDW_RADII.put(37, 3.03d);
        ELEMENT_VDW_RADII.put(38, 2.49d);
        ELEMENT_VDW_RADII.put(39, 2.4d);
        ELEMENT_VDW_RADII.put(40, 2.3d);
        ELEMENT_VDW_RADII.put(41, 2.15d);
        ELEMENT_VDW_RADII.put(42, 2.1d);
        ELEMENT_VDW_RADII.put(43, 2.05d);
        ELEMENT_VDW_RADII.put(44, 2.05d);
        ELEMENT_VDW_RADII.put(45, 2d);
        ELEMENT_VDW_RADII.put(46, 2.05d);
        ELEMENT_VDW_RADII.put(47, 2.1d);
        ELEMENT_VDW_RADII.put(48, 2.2d);
        ELEMENT_VDW_RADII.put(49, 2.2d);
        ELEMENT_VDW_RADII.put(50, 1.93d);
        ELEMENT_VDW_RADII.put(51, 2.17d);
        ELEMENT_VDW_RADII.put(52, 2.06d);
        ELEMENT_VDW_RADII.put(53, 1.98d);
        ELEMENT_VDW_RADII.put(54, 2.16d);
        ELEMENT_VDW_RADII.put(55, 3.43d);
        ELEMENT_VDW_RADII.put(56, 2.68d);
        ELEMENT_VDW_RADII.put(57, 2.5d);
        ELEMENT_VDW_RADII.put(58, 2.48d);
        ELEMENT_VDW_RADII.put(59, 2.47d);
        ELEMENT_VDW_RADII.put(60, 2.45d);
        ELEMENT_VDW_RADII.put(61, 2.43d);
        ELEMENT_VDW_RADII.put(62, 2.42d);
        ELEMENT_VDW_RADII.put(63, 2.4d);
        ELEMENT_VDW_RADII.put(64, 2.38d);
        ELEMENT_VDW_RADII.put(65, 2.37d);
        ELEMENT_VDW_RADII.put(66, 2.35d);
        ELEMENT_VDW_RADII.put(67, 2.33d);
        ELEMENT_VDW_RADII.put(68, 2.32d);
        ELEMENT_VDW_RADII.put(69, 2.3d);
        ELEMENT_VDW_RADII.put(70, 2.28d);
        ELEMENT_VDW_RADII.put(71, 2.27d);
        ELEMENT_VDW_RADII.put(72, 2.25d);
        ELEMENT_VDW_RADII.put(73, 2.2d);
        ELEMENT_VDW_RADII.put(74, 2.1d);
        ELEMENT_VDW_RADII.put(75, 2.05d);
        ELEMENT_VDW_RADII.put(76, 2d);
        ELEMENT_VDW_RADII.put(77, 2d);
        ELEMENT_VDW_RADII.put(78, 2.05d);
        ELEMENT_VDW_RADII.put(79, 2.1d);
        ELEMENT_VDW_RADII.put(80, 2.05d);
        ELEMENT_VDW_RADII.put(81, 1.96d);
        ELEMENT_VDW_RADII.put(82, 2.02d);
        ELEMENT_VDW_RADII.put(83, 2.07d);
        ELEMENT_VDW_RADII.put(84, 1.97d);
        ELEMENT_VDW_RADII.put(85, 2.02d);
        ELEMENT_VDW_RADII.put(86, 2.2d);
        ELEMENT_VDW_RADII.put(87, 3.48d);
        ELEMENT_VDW_RADII.put(88, 2.83d);
        ELEMENT_VDW_RADII.put(89, 2d);
        ELEMENT_VDW_RADII.put(90, 2.4d);
        ELEMENT_VDW_RADII.put(91, 2d);
        ELEMENT_VDW_RADII.put(92, 2.3d);
        ELEMENT_VDW_RADII.put(93, 2d);
        ELEMENT_VDW_RADII.put(94, 2d);
        ELEMENT_VDW_RADII.put(95, 2d);
        ELEMENT_VDW_RADII.put(96, 2d);
        ELEMENT_VDW_RADII.put(97, 2d);
        ELEMENT_VDW_RADII.put(98, 2d);
        ELEMENT_VDW_RADII.put(99, 2d);
        ELEMENT_VDW_RADII.put(100, 2d);
        ELEMENT_VDW_RADII.put(101, 2d);
        ELEMENT_VDW_RADII.put(102, 2d);
        ELEMENT_VDW_RADII.put(103, 2d);
        ELEMENT_VDW_RADII.put(104, 2d);
        ELEMENT_VDW_RADII.put(105, 2d);
        ELEMENT_VDW_RADII.put(106, 2d);
        ELEMENT_VDW_RADII.put(107, 2d);
        ELEMENT_VDW_RADII.put(108, 2d);
        ELEMENT_VDW_RADII.put(109, 2d);
        ELEMENT_VDW_RADII.put(110, 2d);
        ELEMENT_VDW_RADII.put(111, 2d);
        ELEMENT_VDW_RADII.put(112, 2d);
        ELEMENT_VDW_RADII.put(113, 2d);
        ELEMENT_VDW_RADII.put(114, 2d);
        ELEMENT_VDW_RADII.put(115, 2d);
        ELEMENT_VDW_RADII.put(116, 2d);
        ELEMENT_VDW_RADII.put(117, 2d);
        ELEMENT_VDW_RADII.put(118, 2d);
    }

    private final Grid grid;

    /**
     * Constructor of an exclusion shape. This defines what part of a 3D space is exclusive to a molecule.
     *
     * @param receptor A molecule that represents the excluded space
     * @throws IllegalArgumentException if any atom in the array is a Hydrogen atom
     */
    public ExclusionShape(Molecule receptor, double tolerance) {
        // Make a grid that spans the size of the molecule and has resolution
        DPoint3[] enclosingCube = receptor.getEnclosingCube();

        // The grid should expand a bit further than the enclosing cube.
        // 5 angstrom is used to maintain a buffer with even the largest elements. (biggest radius is 3.24 angstrom)
        DPoint3 margin = new DPoint3(5, 5, 5);

        // The reference point is a corner of the enclosing grid
        DPoint3 referenceCoordinate = DPoint3.subtract(enclosingCube[0], margin);
        DPoint3 endCoordinate = DPoint3.add(enclosingCube[1], margin);
        DPoint3 cubeSize = DPoint3.subtract(endCoordinate, referenceCoordinate);

        // Construct a grid with the calculated dimensions, the reference coordinate, and the resolution of 0.5.
        grid = new Grid(referenceCoordinate, cubeSize.x, cubeSize.y, cubeSize.z, RESOLUTION);

        fillAccessibleSurface(receptor);
        // Scrape using the default probe size
        grid.scrapeProbe(DEFAULT_PROBE_SIZE);
        if (tolerance > 0) {
            grid.shrink(tolerance);
        } else {
            grid.grow(-tolerance);
        }
    }

    /**
     * Marks grid spheres based on the receptor
     *
     * @param receptor the receptor to mark the grid from
     */
    private void fillAccessibleSurface(Molecule receptor) {
        for (MolAtom atom : receptor.atoms()) {
            grid.markSphere(atom.getLocation(),
                    ELEMENT_VDW_RADII.get(atom.getAtno()) + DEFAULT_PROBE_SIZE, true);
        }
    }

    /**
     * Checks for collisions with the grid
     *
     * @param other the molecule to check for collisions
     * @return are there collisions?
     */
    public boolean inShape(Molecule other) {
        for (MolAtom atom : other.atoms()) {
            boolean marked = grid.isMarked(atom.getLocation());
            if (marked) return true;
        }
        return false;
    }

    public Grid getGrid() {
        return grid;
    }

    /**
     * Getter for the map of VDW radii
     *
     * @return the map of VDW radii
     */
    public static Map<Integer, Double> getElementVdwRadii() {
        return ELEMENT_VDW_RADII;
    }
}
