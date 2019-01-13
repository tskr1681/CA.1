/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.DPoint3;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * An implementation of a 3D matrix, or grid that is used to describe an exclusive shape in.
 * The implementation of the exclusive shape functionality is based on the
 * <a href="https://pubs.acs.org/doi/abs/10.1021/ci200097m">Pharmit</a> implementation of exclusive shape.
 * <a href="https://sourceforge.net/p/pharmit">pharmit source code</a>
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Grid {
    private final double resolution;
    private final boolean[][][] grid;
    private final DPoint3 referenceCoordinates;

    /**
     * Constructor for a grid with a specified size, reference coordinate and resolution.
     *
     * @param referenceCoordinates The position of the corner of the grid with the smallest x, y and z variables
     *                             in the 3d space.
     * @param xSize The size of the rectangular cuboid on the x axis.
     * @param ySize The size of the rectangular cuboid on the y axis.
     * @param zSize The size of the rectangular cuboid on the z axis.
     * @param resolution The size of a grid cell. if 0.5 then 1 angstrom will consist of 2 grid cells (cubes)
     */
    public Grid(DPoint3 referenceCoordinates, double xSize, double ySize, double zSize, double resolution) {
        this.referenceCoordinates = referenceCoordinates;
        this.resolution = resolution;
        grid = new boolean
                [getCeil(xSize, resolution)]
                [getCeil(ySize, resolution)]
                [getCeil(zSize, resolution)];
    }

    /**
     * the size of the axis in terms of grid cells (cubes).
     *
     * @param size The size of the axis in angstrom.
     * @param resolution The size of a grid cell. if 0.5 then 1 angstrom will consist of 2 grid cells (cubes).
     * @return the size of the axis in terms of grid cells (cubes).
     */
    private int getCeil(double size, double resolution) {
        return (int) Math.ceil(size / resolution);
    }

    /**
     * marks a sphere as occupied or not occupied in the grid with the given coordinates and radius.
     *
     * @param sphereCoordinates The coordinates of the center of the sphere to mark.
     * @param radius The radius of the sphere to mark.
     * @param value If the sphere should be marked as occupied or not.
     */
    void markSphere(DPoint3 sphereCoordinates, double radius, boolean value) {
        double x = sphereCoordinates.x;
        double y = sphereCoordinates.y;
        double z = sphereCoordinates.z;

        //mark all yz circles
        for (double d = 0; d <= radius; d += resolution) {
            double chordRadius = chordRadius(radius, d);

            markYZCircle(x + d, y, z, chordRadius, value);
            if (d != 0) {
                markYZCircle(x - d, y, z, chordRadius, value);
            }
        }
    }

    /**
     * Marks a circle as occupied or not occupied in the grid with the given coordinates and radius on the YZ plane.
     *
     * @param x The center of the circle on the x axis.
     * @param y The center of the circle on the y axis.
     * @param z The center of the circle on the z axis.
     * @param radius The radius of the circle to mark.
     * @param value If the circle should be marked as occupied or not.
     */
    private void markYZCircle(double x, double y, double z, double radius, boolean value) {
        for (double d = 0; d <= radius; d += resolution) {
            double chordRadius = chordRadius(radius, d);
            markZChord(x, y + d, z, chordRadius, value);
            if (d != 0) {
                markZChord(x, y - d, z, chordRadius, value);
            }
        }
    }

    /**
     * Marks a chord of a circle on the z axis as occupied or not occupied.
     *
     * @param x The center of the chord on the x axis.
     * @param y The center of the chord on the y axis.
     * @param z The center of the chord on the z axis.
     * @param radius the chord radius to mark.
     * @param value If the chord should be marked as occupied or not.
     */
    private void markZChord(double x, double y, double z, double radius, boolean value) {
        GridLocation start = pointToGrid(x, y, z - radius);
        GridLocation end = pointToGrid(x, y, z + radius);
        if (start == null || end == null)
            return;
        this.setRange(start, end, value);
    }

    /**
     * Set the range in the grid as occupied or not on the z axis.
     *
     * @param start The location of the grid to start marking.
     * @param end The location of the grid to end marking.
     * @param b If the range should be marked as occupied or not.
     */
    private void setRange(GridLocation start, GridLocation end, boolean b) {
        for (int i = start.getZ(); i <= end.getZ(); i++) {
            grid[start.getX()][start.getY()][i] = b;
        }
    }

    /**
     * Converts a point in 3d space to a point in the grid.
     *
     * @param x The x coordinate in 3d space in angstrom.
     * @param y The y coordinate in 3d space in angstrom.
     * @param z The z coordinate in 3d space in angstrom.
     * @return the grid cell location that contains the given point.
     */
    private GridLocation pointToGrid(double x, double y, double z) {
        int gridX = transformCoordinateToGrid(x, referenceCoordinates.x);
        int gridY = transformCoordinateToGrid(y, referenceCoordinates.y);
        int gridZ = transformCoordinateToGrid(z, referenceCoordinates.z);

        if (locationOutsideGrid(gridX, gridY, gridZ)) return null;

        return new GridLocation(gridX, gridY, gridZ);
    }

    /**
     * Checks if the given indices for a location int the grid are out of bounds of the grid.
     *
     * @param x The x index.
     * @param y The y index.
     * @param z The z index.
     * @return true if the location is outside of the grid.
     */
    private boolean locationOutsideGrid(int x, int y, int z) {
        if (x < 0 || x >= grid.length ||
                y < 0 || y >= grid[0].length ||
                z < 0 || z >= grid[0][0].length) {
            System.out.printf("%s, %s, %s%n", x, y, z);
            return true;
        }
        return false;
    }

    /**
     * Converts a location in the grid to a point in 3D space.
     *
     * @param x The x index.
     * @param y The y index.
     * @param z The z index.
     * @return the coordinates of the grid location in 3D space.
     */
    public DPoint3 gridToPoint(int x, int y, int z) {
        double pointX = transformCoordinateToRealValue(x, referenceCoordinates.x);
        double pointY = transformCoordinateToRealValue(y, referenceCoordinates.y);
        double pointZ = transformCoordinateToRealValue(z, referenceCoordinates.z);

        return new DPoint3(pointX, pointY, pointZ);
    }

    /**
     * Transforms a coordinate or index in the grid to the coordinate in 3D space.
     *
     * @param coordinate The coordinate to convert.
     * @param referenceCoordinate The reference coordinate that defines the starting point for the grid on the axis.
     * @return the coordinates of the point in 3d space.
     */
    private double transformCoordinateToRealValue(int coordinate, double referenceCoordinate) {
        return (coordinate * resolution) + referenceCoordinate;
    }

    /**
     * Transforms a coordinate in 3D space to the coordinate or index in the grid.
     *
     * @param coordinate The coordinate in 3D space that should be converted.
     * @param referenceCoordinate The reference coordinate of the grid of the specific axis.
     * @return the index of the location in the grid.
     */
    private int transformCoordinateToGrid(double coordinate, double referenceCoordinate) {
        return (int) Math.round((coordinate - referenceCoordinate) / resolution);
    }

    /**
     * Calculates the radius of a chord.
     *
     * @param radius the radius of the current object.
     * @param d the amount to move.
     * @return the 'radius' of the chord.
     */
    private double chordRadius(double radius, double d) {
        if (d > radius) {
            return 0;
        }
        return Math.sqrt(radius * radius - d * d);
    }

    /**
     * Method that checks if the given location is marked in the grid as occupied.
     *
     * @param location The location to check.
     * @return true if the location is marked as occupied, false if not.
     */
    boolean isMarked(DPoint3 location) {
        return isMarked(location.x, location.y, location.z);
    }

    /**
     * Method that checks if the given location is marked in the grid as occupied.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @return true if the location is marked as occupied, false if not.
     */
    private boolean isMarked(double x, double y, double z) {
        GridLocation locationInGrid = pointToGrid(x, y, z);
        if (locationInGrid == null) return false;
        return grid[locationInGrid.getX()][locationInGrid.getY()][locationInGrid.getZ()];
    }

    /**
     * Enclose the exclusive shape, or occupied space by the given amount.
     *
     * @param amount the amount to grow the exclusive shape by in angstrom.
     */
    void grow(double amount) {
        int num = (int) Math.ceil(amount / resolution);
        for (int i = 0; i < num; i++) {
            growByOne();
        }
    }

    /**
     * Encloses the exclusive shape, or occupied space by a single cube or grid cell.
     */
    private void growByOne() {

        List<GridLocation> toAdd = new ArrayList<>();
        for (GridLocation markedGridLocation : getMarkedGridLocations()) {
            int x = markedGridLocation.getX();
            int y = markedGridLocation.getY();
            int z = markedGridLocation.getZ();

            // Check if the surrounding cells are marked as occupied and store the location if it is not.
            if (!grid[x + 1][y][z]) {
                toAdd.add(new GridLocation(x + 1, y, z));
            }
            if (!grid[x - 1][y][z]) {
                toAdd.add(new GridLocation(x - 1, y, z));
            }

            if (!grid[x][y + 1][z]) {
                toAdd.add(new GridLocation(x, y + 1, z));
            }
            if (!grid[x][y - 1][z]) {
                toAdd.add(new GridLocation(x, y - 1, z));
            }

            if (!grid[x][y][z + 1]) {
                toAdd.add(new GridLocation(x, y, z + 1));
            }
            if (!grid[x][y][z - 1]) {
                toAdd.add(new GridLocation(x, y, z - 1));
            }
        }
        this.setCollection(toAdd, true);
    }

    /**
     * Method that checks if the location or cube in the grid is exposed on one of its sides.
     *
     * @param x The x index.
     * @param y The y index.
     * @param z The z index.
     * @return true if the cube is exposed on one of its sides.
     */
    private boolean isExposed(int x, int y, int z) {
        return !grid[x + 1][y][z] || !grid[x - 1][y][z] ||
                !grid[x][y + 1][z] || !grid[x][y - 1][z] ||
                !grid[x][y][z + 1] || !grid[x][y][z - 1];
    }

    /**
     * Marks the probe sphere of the given size as unmarked, not occupied, on every exposed point.
     *
     * @param probeSize The size of the probe sphere.
     */
    void scrapeProbe(double probeSize) {
        List<GridLocation> exposedPoints = new ArrayList<>();
        getExposedPoints(exposedPoints);

        for (GridLocation exposedPoint : exposedPoints) {
            // For every exposed point, mark te probe sphere as not occupied.
            markSphere(gridToPoint(exposedPoint.getX(), exposedPoint.getY(), exposedPoint.getZ()),
                    probeSize,
                    false);
        }
    }

    /**
     * Removes an enclosing layer of the exclusive shape, or occupied space by the given amount.
     *
     * @param amount the amount to shrink the exclusive shape by in angstrom.
     */
    void shrink(double amount)
    {
        int num = (int) Math.ceil(amount / resolution);
        for (int i = 0; i < num; i++)
        {
            shrinkByOne();
        }

    }

    /**
     * Removes a layer of enclosing cubes of the exclusive shape, or occupied space by a single cube or grid cell.
     */
    private void shrinkByOne() {
        List<GridLocation> exposedPoints = new ArrayList<>();
        getExposedPoints(exposedPoints);

        for (GridLocation exposedPoint : exposedPoints) {
            this.grid[exposedPoint.getX()][exposedPoint.getY()][exposedPoint.getZ()] = false;
        }
    }

    /**
     * Method collects every exposed point in the collection of marked grid locations.
     *
     * @param exposedPoints the collection to add exposed points to.
     */
    private void getExposedPoints(List<GridLocation> exposedPoints) {
        for (GridLocation markedGridLocation : getMarkedGridLocations()) {
            int x = markedGridLocation.getX();
            int y = markedGridLocation.getY();
            int z = markedGridLocation.getZ();

            if (isExposed(x, y, z)) {
                exposedPoints.add(markedGridLocation);
            }
        }
    }

    /**
     * Method sets the marking of each grid location in a list of grid locations.
     *
     * @param toAdd the collection of grid locations to set the marking for.
     * @param value if the grid locations should be marked as occupied.
     */
    private void setCollection(List<GridLocation> toAdd, boolean value) {
        for (GridLocation gridLocation : toAdd) {
            if (!locationOutsideGrid(gridLocation.getX(), gridLocation.getY(), gridLocation.getZ())) {
                grid[gridLocation.getX()][gridLocation.getY()][gridLocation.getZ()] = value;
            }
        }
    }

    /**
     * Method that collects the as occupied marked grid locations.
     *
     * @return every marked grid location in a list.
     */
    private List<GridLocation> getMarkedGridLocations() {
        List<GridLocation> markedGridLocations = new ArrayList<>();

        // Loop through yz planes in the grid.
        for (int i = 0; i < this.grid.length; i++) {
            boolean[][] yzGrid = this.grid[i];
            // Loop through every z axis in the yz plane.
            for (int j = 0; j < yzGrid.length; j++) {
                boolean[] zArray = yzGrid[j];
                // Loop through every cell or cube in the z axis.
                for (int k = 0; k < zArray.length; k++) {
                    if (zArray[k]) {
                        // Collect the location if marked as occupied.
                        markedGridLocations.add(new GridLocation(i, j, k));
                    }
                }
            }
        }
        return markedGridLocations;
    }


    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(System.lineSeparator());

        for (int i = 0; i < grid.length; i++) {
            String s = YZPlaneToString(i);
            sj.add(s);
        }

        return sj.toString();
    }

    /**
     * Method that converts an yz plane to a string.
     *
     * @param i The index of the yz plane to convert.
     * @return the yz plane as a string.
     */
    private String YZPlaneToString(int i) {
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        boolean[][] booleans = grid[i];
        sj.add(i + ":");
        for (boolean[] aBooleans : booleans) {
            StringBuilder sb = new StringBuilder();
            for (boolean ab : aBooleans) {
                sb.append(formatBool(ab));
            }
            sj.add(sb.toString());
        }
        return sj.toString();
    }

    /**
     * Method that converts a boolean to a colored bit of whitespace.
     *
     * @param b the boolean to convert.
     * @return a gray bit of whitespace in case the cube is not occupied and a white bit of whitespace if it is.
     */
    private String formatBool(boolean b) {
        return b ? " " : "\033[0;100m \033[0m";
    }

    public boolean[][][] getGrid() {
        return grid;
    }
}
