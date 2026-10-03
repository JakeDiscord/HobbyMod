package io.github.jakediscord.hobbymod.sculpting;

public enum CarvingTool {
    DETAIL(0.038, false, "Detail chisel"),
    POINT(0.072, false, "Point chisel"),
    ROUGH(0.14, false, "Roughing mallet"),
    POLISH(0.10, true, "Polishing rasp");

    public final double cutRadius;
    public final boolean polishes;
    public final String title;
    CarvingTool(double radius,boolean polishes,String title) { cutRadius=radius;this.polishes=polishes;this.title=title; }
}
