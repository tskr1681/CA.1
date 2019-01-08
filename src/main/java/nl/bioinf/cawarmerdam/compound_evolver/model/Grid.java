/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model;

import chemaxon.struc.DPoint3;
import org.apache.poi.hssf.record.PrintGridlinesRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class Grid {
    private final double resolution;
    private boolean[][][] grid;
    private DPoint3 referenceCoordinates;

    public Grid(DPoint3 referenceCoordinates, double xSize, double ySize, double zSize, double resolution) {
        this.referenceCoordinates = referenceCoordinates;
        this.resolution = resolution;
        grid = new boolean
                [getCeil(xSize, resolution)]
                [getCeil(ySize, resolution)]
                [getCeil(zSize, resolution)];
    }

    private int getCeil(double size, double resolution) {
        return (int) Math.ceil(size / resolution);
    }

    public void markSphere(DPoint3 sphereCoordinates, double radius, boolean value) {
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

    private void markYZCircle(double x, double y, double z, double radius, boolean value) {
        for (double d = 0; d <= radius; d += resolution) {
            double chordRadius = chordRadius(radius, d);
            markZChord(x, y + d, z, chordRadius, value);
            if (d != 0) {
                markZChord(x, y - d, z, chordRadius, value);
            }
        }
    }

    private void markZChord(double x, double y, double z, double radius, boolean value) {
        GridLocation start = pointToGrid(x, y, z - radius);
        GridLocation end = pointToGrid(x, y, z + radius);
        if (start == null || end == null)
            return;
        this.setRange(start, end, value);
    }

    private void setRange(GridLocation start, GridLocation end, boolean b) {
        for (int i = start.getZ(); i <= end.getZ(); i++) {
            grid[start.getX()][start.getY()][i] = b;
        }
    }

    public GridLocation pointToGrid(double x, double y, double z) {
        int gridX = transformCoordinateToGrid(x, referenceCoordinates.x);
        int gridY = transformCoordinateToGrid(y, referenceCoordinates.y);
        int gridZ = transformCoordinateToGrid(z, referenceCoordinates.z);

        if (locationOutsideGrid(gridX, gridY, gridZ)) return null;

        return new GridLocation(gridX, gridY, gridZ);
    }

    private boolean locationOutsideGrid(int x, int y, int z) {
        if (x < 0 || x >= grid.length ||
                y < 0 || y >= grid[0].length ||
                z < 0 || z >= grid[0][0].length) {
            System.out.printf("%s, %s, %s%n", x, y, z);
            return true;
        }
        return false;
    }

    public DPoint3 gridToPoint(int x, int y, int z) {
        double pointX = transformCoordinateToRealValue(x, referenceCoordinates.x);
        double pointY = transformCoordinateToRealValue(y, referenceCoordinates.y);
        double pointZ = transformCoordinateToRealValue(z, referenceCoordinates.z);

        return new DPoint3(pointX, pointY, pointZ);
    }

    private double transformCoordinateToRealValue(int coord, double refCoord) {
        return (coord * resolution) + refCoord;
    }

    private int transformCoordinateToGrid(double coord, double refCoord) {
        return (int) Math.round((coord - refCoord) / resolution);
    }

    private double chordRadius(double radius, double d) {
        if (d > radius) {
            return 0;
        }
        return Math.sqrt(radius * radius - d * d);
    }

    boolean isMarked(DPoint3 location) {
        return isMarked(location.x, location.y, location.z);
    }

    private boolean isMarked(double x, double y, double z) {
        GridLocation locationInGrid = pointToGrid(x, y, z);
        if (locationInGrid == null) return false;
        return grid[locationInGrid.getX()][locationInGrid.getY()][locationInGrid.getZ()];
    }

    void grow(double amount) {
        int num = (int) Math.ceil(amount / resolution);
        for (int i = 0; i < num; i++) {
            growByOne();
        }
    }

    private void growByOne() {

        List<GridLocation> toAdd = new ArrayList<>();
        for (GridLocation markedGridLocation : getMarkedGridLocations()) {
            int x = markedGridLocation.getX();
            int y = markedGridLocation.getY();
            int z = markedGridLocation.getZ();

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

    private boolean isExposed(int x, int y, int z) {
        return !grid[x + 1][y][z] || !grid[x - 1][y][z] ||
                !grid[x][y + 1][z] || !grid[x][y - 1][z] ||
                !grid[x][y][z + 1] || !grid[x][y][z - 1];
    }

    void scrapeProbe(double defaultProbeSize) {
        List<GridLocation> exposedPoints = new ArrayList<>();
        getExposedPoints(exposedPoints);

        for (GridLocation exposedPoint : exposedPoints) {
            markSphere(gridToPoint(exposedPoint.getX(), exposedPoint.getY(), exposedPoint.getZ()),
                    defaultProbeSize,
                    false);
        }
    }

    void shrink(double amount)
    {
        int num = (int) Math.ceil(amount / resolution);
        for (int i = 0; i < num; i++)
        {
            shrinkByOne();
        }

    }

    private void shrinkByOne() {
        List<GridLocation> exposedPoints = new ArrayList<>();
        getExposedPoints(exposedPoints);

        for (GridLocation exposedPoint : exposedPoints) {
            this.grid[exposedPoint.getX()][exposedPoint.getY()][exposedPoint.getZ()] = false;
        }
    }

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

    private void setCollection(List<GridLocation> toAdd, boolean value) {
        for (GridLocation gridLocation : toAdd) {
            if (!locationOutsideGrid(gridLocation.getX(), gridLocation.getY(), gridLocation.getZ())) {
                grid[gridLocation.getX()][gridLocation.getY()][gridLocation.getZ()] = value;
            }
        }
    }

    private List<GridLocation> getMarkedGridLocations() {
        List<GridLocation> markedGridLocations = new ArrayList<>();

        for (int i = 0; i < this.grid.length; i++) {
            boolean[][] yzGrid = this.grid[i];
            for (int j = 0; j < yzGrid.length; j++) {
                boolean[] zArray = yzGrid[j];
                for (int k = 0; k < zArray.length; k++) {
                    if (zArray[k]) {
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

    private String YZPlaneToString(int i) {
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        boolean[][] booleans = grid[i];
        sj.add(String.valueOf(i) + ":");
        for (boolean[] aBooleans : booleans) {
            StringBuilder sb = new StringBuilder();
            for (boolean ab : aBooleans) {
                sb.append(formatBool(ab));
            }
            sj.add(sb.toString());
        }
        return sj.toString();
    }

    private String formatBool(boolean b) {
        return b ? " " : "\033[0;100m \033[0m";
    }

    public boolean[][][] getGrid() {
        return grid;
    }
}
