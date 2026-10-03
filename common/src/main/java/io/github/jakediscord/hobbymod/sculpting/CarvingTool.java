package io.github.jakediscord.hobbymod.sculpting;

public enum CarvingTool {
    DETAIL(0, false, "Detail chisel"),
    POINT(1, false, "Point chisel"),
    ROUGH(2, false, "Roughing mallet"),
    POLISH(1, true, "Polishing rasp");

    public final int radius;
    public final boolean polishes;
    public final String title;

    CarvingTool(int radius, boolean polishes, String title) {
        this.radius = radius;
        this.polishes = polishes;
        this.title = title;
    }
}
