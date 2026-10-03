package io.github.jakediscord.hobbymod.sculpting;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** Loader-independent marble volume. Each editable cell is 1/32 of a block. */
public final class MarbleVolume {
    public static final int SIZE = 32;
    public static final int CELLS = SIZE * SIZE * SIZE;
    public static final int WORDS = CELLS / Long.SIZE;
    private final BitSet marble;
    private final BitSet polished;
    private List<Face> mesh;

    public MarbleVolume() {
        marble = new BitSet(CELLS);
        marble.set(0, CELLS);
        polished = new BitSet(CELLS);
    }

    private MarbleVolume(BitSet marble, BitSet polished) {
        this.marble = marble;
        this.polished = polished;
        this.polished.and(marble);
    }

    public MarbleVolume copy() { return new MarbleVolume((BitSet) marble.clone(), (BitSet) polished.clone()); }
    public int count() { return marble.cardinality(); }
    public int polishedCount() { return polished.cardinality(); }
    public long[] marbleBits() { return marble.toLongArray(); }
    public long[] polishBits() { return polished.toLongArray(); }

    public static MarbleVolume read(long[] cells, long[] polish) {
        if (cells.length > WORDS || polish.length > WORDS) return new MarbleVolume();
        BitSet solid = BitSet.valueOf(cells);
        // An empty or malformed saved volume must not create invisible unbreakable blocks.
        if (solid.isEmpty()) return new MarbleVolume();
        return new MarbleVolume(solid, BitSet.valueOf(polish));
    }

    public static int index(int x, int y, int z) { return x + SIZE * (y + SIZE * z); }
    public static int x(int index) { return index % SIZE; }
    public static int y(int index) { return index / SIZE % SIZE; }
    public static int z(int index) { return index / (SIZE * SIZE); }
    public boolean has(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < SIZE && y < SIZE && z < SIZE && marble.get(index(x, y, z));
    }
    public boolean surface(int x, int y, int z) {
        return has(x, y, z) && (!has(x - 1, y, z) || !has(x + 1, y, z)
                || !has(x, y - 1, z) || !has(x, y + 1, z) || !has(x, y, z - 1) || !has(x, y, z + 1));
    }

    /** Brushes operate on an exposed cell; never add material or erase the final cell. */
    public int stroke(int target, CarvingTool tool) {
        if (target < 0 || target >= CELLS || !surface(x(target), y(target), z(target))) return 0;
        int cx = x(target), cy = y(target), cz = z(target), r = tool.radius;
        BitSet affected = new BitSet(CELLS);
        for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dy * dy + dz * dz > r * r) continue;
            int x = cx + dx, y = cy + dy, z = cz + dz;
            if (has(x, y, z) && (!tool.polishes || surface(x, y, z))) affected.set(index(x, y, z));
        }
        if (tool.polishes) {
            affected.andNot(polished);
            polished.or(affected);
        } else {
            if (affected.cardinality() >= count()) return 0;
            marble.andNot(affected);
            polished.and(marble);
        }
        int changed = affected.cardinality();
        if (changed > 0) mesh = null;
        return changed;
    }

    public record Hit(int cell, int side, double distance) {}

    /** Orthographic/perspective ray picking through empty carved space using voxel DDA. */
    public Hit pick(double[] origin, double[] direction) {
        double enter = 0, exit = Double.POSITIVE_INFINITY;
        int side = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) < 1e-10) {
                if (origin[axis] < 0 || origin[axis] > 1) return null;
                continue;
            }
            double a = -origin[axis] / direction[axis], b = (1 - origin[axis]) / direction[axis];
            double near = Math.min(a, b), far = Math.max(a, b);
            if (near > enter) { enter = near; side = axis * 2 + (direction[axis] < 0 ? 1 : 0); }
            exit = Math.min(exit, far);
        }
        if (exit < enter || exit < 0) return null;
        int[] cell = new int[3], step = new int[3];
        double[] next = new double[3], delta = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            cell[axis] = Math.clamp((int) Math.floor((origin[axis] + direction[axis] * (enter + 1e-8)) * SIZE), 0, SIZE - 1);
            step[axis] = direction[axis] >= 0 ? 1 : -1;
            delta[axis] = Math.abs(direction[axis]) < 1e-10 ? Double.POSITIVE_INFINITY : 1.0 / SIZE / Math.abs(direction[axis]);
            next[axis] = Math.abs(direction[axis]) < 1e-10 ? Double.POSITIVE_INFINITY
                    : ((cell[axis] + (step[axis] > 0 ? 1 : 0)) / (double) SIZE - origin[axis]) / direction[axis];
        }
        double distance = enter;
        for (int i = 0; i < 3 * SIZE + 3; i++) {
            if (has(cell[0], cell[1], cell[2])) return new Hit(index(cell[0], cell[1], cell[2]), side, distance);
            int axis = next[0] < next[1] ? 0 : 1;
            if (next[2] < next[axis]) axis = 2;
            distance = next[axis];
            if (distance > exit + 1e-8) return null;
            cell[axis] += step[axis];
            if (cell[axis] < 0 || cell[axis] >= SIZE) return null;
            side = axis * 2 + (step[axis] < 0 ? 1 : 0);
            next[axis] += delta[axis];
        }
        return null;
    }

    /** Greedy surface meshing: untouched marble renders as six quads, not 32,768 cubes. */
    public List<Face> faces() {
        if (mesh != null) return mesh;
        List<Face> result = new ArrayList<>();
        for (int side = 0; side < 6; side++) {
            int axis = side / 2, uAxis = (axis + 1) % 3, vAxis = (axis + 2) % 3;
            boolean positive = side % 2 == 1;
            for (int layer = 0; layer < SIZE; layer++) {
                int[] mask = new int[SIZE * SIZE];
                for (int v = 0; v < SIZE; v++) for (int u = 0; u < SIZE; u++) {
                    int[] p = new int[3]; p[axis] = layer; p[uAxis] = u; p[vAxis] = v;
                    if (!has(p[0], p[1], p[2])) continue;
                    int id = index(p[0], p[1], p[2]);
                    p[axis] += positive ? 1 : -1;
                    if (!has(p[0], p[1], p[2])) mask[u + v * SIZE] = polished.get(id) ? 2 : 1;
                }
                for (int v = 0; v < SIZE; v++) for (int u = 0; u < SIZE; u++) {
                    int material = mask[u + v * SIZE];
                    if (material == 0) continue;
                    int width = 1, height = 1;
                    while (u + width < SIZE && mask[u + width + v * SIZE] == material) width++;
                    outer: while (v + height < SIZE) {
                        for (int du = 0; du < width; du++) if (mask[u + du + (v + height) * SIZE] != material) break outer;
                        height++;
                    }
                    for (int dv = 0; dv < height; dv++) for (int du = 0; du < width; du++) mask[u + du + (v + dv) * SIZE] = 0;
                    result.add(new Face(side, layer + (positive ? 1 : 0), u, v, width, height, material == 2));
                }
            }
        }
        mesh = List.copyOf(result);
        return mesh;
    }

    public record Face(int side, int plane, int u, int v, int width, int height, boolean polished) {
        public double[][] vertices() {
            int axis = side / 2, ua = (axis + 1) % 3, va = (axis + 2) % 3;
            int[][] corners = side % 2 == 1
                    ? new int[][]{{u,v},{u+width,v},{u+width,v+height},{u,v+height}}
                    : new int[][]{{u,v},{u,v+height},{u+width,v+height},{u+width,v}};
            double[][] points = new double[4][3];
            for (int i = 0; i < 4; i++) {
                points[i][axis] = plane / (double) SIZE;
                points[i][ua] = corners[i][0] / (double) SIZE;
                points[i][va] = corners[i][1] / (double) SIZE;
            }
            return points;
        }
    }
}
