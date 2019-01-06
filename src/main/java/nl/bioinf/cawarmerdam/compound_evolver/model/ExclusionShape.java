package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.DPoint3;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;

import java.util.HashMap;
import java.util.Map;

public class ExclusionShape {
    // Bosco uses as default 960, Shrake and Rupley seem to use in their paper 92 (not sure if this is actually the same parameter)
    private static final double DEFAULT_PROBE_SIZE = 1.4;
    private static final double RESOLUTION = 0.5;

    private static Map<Integer, Double> ELEMENT_VDW_RADII = new HashMap<>();
    static {
        ELEMENT_VDW_RADII.put(1, 1.10);
        ELEMENT_VDW_RADII.put(6, 1.55);
        ELEMENT_VDW_RADII.put(7, 1.40);
        ELEMENT_VDW_RADII.put(8, 1.35);
        ELEMENT_VDW_RADII.put(2, 2.20);
        ELEMENT_VDW_RADII.put(3, 1.22);
        ELEMENT_VDW_RADII.put(4, 0.63);
        ELEMENT_VDW_RADII.put(5, 1.55);
        ELEMENT_VDW_RADII.put(9, 1.30);
        ELEMENT_VDW_RADII.put(10, 2.02);
        ELEMENT_VDW_RADII.put(11, 2.20);
        ELEMENT_VDW_RADII.put(12, 1.50);
        ELEMENT_VDW_RADII.put(13, 1.50);
        ELEMENT_VDW_RADII.put(14, 2.20);
        ELEMENT_VDW_RADII.put(15, 1.88);
        ELEMENT_VDW_RADII.put(16, 1.81);
        ELEMENT_VDW_RADII.put(17, 1.75);
        ELEMENT_VDW_RADII.put(18, 2.77);
        ELEMENT_VDW_RADII.put(19, 2.39);
        ELEMENT_VDW_RADII.put(20, 1.95);
        ELEMENT_VDW_RADII.put(21, 1.32);
        ELEMENT_VDW_RADII.put(22, 1.95);
        ELEMENT_VDW_RADII.put(23, 1.06);
        ELEMENT_VDW_RADII.put(24, 1.13);
        ELEMENT_VDW_RADII.put(25, 1.19);
        ELEMENT_VDW_RADII.put(26, 1.95);
        ELEMENT_VDW_RADII.put(27, 1.13);
        ELEMENT_VDW_RADII.put(28, 1.24);
        ELEMENT_VDW_RADII.put(29, 1.15);
        ELEMENT_VDW_RADII.put(30, 1.15);
        ELEMENT_VDW_RADII.put(31, 1.55);
        ELEMENT_VDW_RADII.put(32, 2.72);
        ELEMENT_VDW_RADII.put(33, 0.83);
        ELEMENT_VDW_RADII.put(34, 0.90);
        ELEMENT_VDW_RADII.put(35, 1.95);
        ELEMENT_VDW_RADII.put(36, 1.90);
        ELEMENT_VDW_RADII.put(37, 2.65);
        ELEMENT_VDW_RADII.put(38, 2.02);
        ELEMENT_VDW_RADII.put(39, 1.61);
        ELEMENT_VDW_RADII.put(40, 1.42);
        ELEMENT_VDW_RADII.put(41, 1.33);
        ELEMENT_VDW_RADII.put(42, 1.75);
        ELEMENT_VDW_RADII.put(43, 1.80);
        ELEMENT_VDW_RADII.put(44, 1.20);
        ELEMENT_VDW_RADII.put(45, 1.22);
        ELEMENT_VDW_RADII.put(46, 1.44);
        ELEMENT_VDW_RADII.put(47, 1.55);
        ELEMENT_VDW_RADII.put(48, 1.75);
        ELEMENT_VDW_RADII.put(49, 1.46);
        ELEMENT_VDW_RADII.put(50, 1.67);
        ELEMENT_VDW_RADII.put(51, 1.12);
        ELEMENT_VDW_RADII.put(52, 1.26);
        ELEMENT_VDW_RADII.put(53, 2.15);
        ELEMENT_VDW_RADII.put(54, 2.10);
        ELEMENT_VDW_RADII.put(55, 3.01);
        ELEMENT_VDW_RADII.put(56, 2.41);
        ELEMENT_VDW_RADII.put(57, 1.83);
        ELEMENT_VDW_RADII.put(58, 1.86);
        ELEMENT_VDW_RADII.put(59, 1.62);
        ELEMENT_VDW_RADII.put(60, 1.79);
        ELEMENT_VDW_RADII.put(61, 1.76);
        ELEMENT_VDW_RADII.put(62, 1.74);
        ELEMENT_VDW_RADII.put(63, 1.96);
        ELEMENT_VDW_RADII.put(64, 1.69);
        ELEMENT_VDW_RADII.put(65, 1.66);
        ELEMENT_VDW_RADII.put(66, 1.63);
        ELEMENT_VDW_RADII.put(67, 1.61);
        ELEMENT_VDW_RADII.put(68, 1.59);
        ELEMENT_VDW_RADII.put(69, 1.57);
        ELEMENT_VDW_RADII.put(70, 1.54);
        ELEMENT_VDW_RADII.put(71, 1.53);
        ELEMENT_VDW_RADII.put(72, 1.40);
        ELEMENT_VDW_RADII.put(73, 1.22);
        ELEMENT_VDW_RADII.put(74, 1.26);
        ELEMENT_VDW_RADII.put(75, 1.30);
        ELEMENT_VDW_RADII.put(76, 1.58);
        ELEMENT_VDW_RADII.put(77, 1.22);
        ELEMENT_VDW_RADII.put(78, 1.55);
        ELEMENT_VDW_RADII.put(79, 1.45);
        ELEMENT_VDW_RADII.put(80, 1.55);
        ELEMENT_VDW_RADII.put(81, 1.96);
        ELEMENT_VDW_RADII.put(82, 2.16);
        ELEMENT_VDW_RADII.put(83, 1.73);
        ELEMENT_VDW_RADII.put(84, 1.21);
        ELEMENT_VDW_RADII.put(85, 1.12);
        ELEMENT_VDW_RADII.put(86, 2.30);
        ELEMENT_VDW_RADII.put(87, 3.24);
        ELEMENT_VDW_RADII.put(88, 2.57);
        ELEMENT_VDW_RADII.put(89, 2.12);
        ELEMENT_VDW_RADII.put(90, 1.84);
        ELEMENT_VDW_RADII.put(91, 1.60);
        ELEMENT_VDW_RADII.put(92, 1.75);
        ELEMENT_VDW_RADII.put(93, 1.71);
        ELEMENT_VDW_RADII.put(94, 1.67);
        ELEMENT_VDW_RADII.put(95, 1.66);
        ELEMENT_VDW_RADII.put(96, 1.65);
        ELEMENT_VDW_RADII.put(97, 1.64);
        ELEMENT_VDW_RADII.put(98, 1.63);
        ELEMENT_VDW_RADII.put(99, 1.62);
        ELEMENT_VDW_RADII.put(100, 1.61);
        ELEMENT_VDW_RADII.put(101, 1.60);
        ELEMENT_VDW_RADII.put(102, 1.59);
        ELEMENT_VDW_RADII.put(103, 1.58);
    }

    private final Grid grid;

    /**
     *
     * @param receptor A molecule that represents the excluded space
     * @throws IllegalArgumentException if any atom in the array is a Hydrogen atom
     */
    public ExclusionShape(Molecule receptor, double tolerance) {
        // Make a grid that spans the size of the molecule and has resolution
        DPoint3[] enclosingCube = receptor.getEnclosingCube();
        DPoint3 margin = new DPoint3(5, 5, 5);
        DPoint3 referenceCoordinate = DPoint3.subtract(enclosingCube[0], margin);
        DPoint3 endCoordinate = DPoint3.add(enclosingCube[1], margin);
        DPoint3 cubeSize = DPoint3.subtract(endCoordinate, referenceCoordinate);
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

    private void fillAccessibleSurface(Molecule receptor) {
        for (MolAtom atom : receptor.atoms()) {
            grid.markSphere(atom.getLocation(),
                    ELEMENT_VDW_RADII.get(atom.getAtno()) + DEFAULT_PROBE_SIZE, true);
        }
    }

//    public boolean fitsShape(Molecule molecule) {
//        DPoint3[] enclosingCube = molecule.getEnclosingCube();
//        enclosingCube
//    }

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
}
