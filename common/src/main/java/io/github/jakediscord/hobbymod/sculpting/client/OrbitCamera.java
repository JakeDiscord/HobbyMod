package io.github.jakediscord.hobbymod.sculpting.client;

/** Pure camera math shared by preview projection and picking; angles are radians. */
public final class OrbitCamera {
    public double yaw = Math.toRadians(-35), pitch = Math.toRadians(24), zoom = 1;
    public double panX, panY;
    public double[] view(double x, double y, double z) {
        x -= 0.5; y -= 0.5; z -= 0.5;
        double a = Math.cos(yaw) * x + Math.sin(yaw) * z;
        double b = -Math.sin(yaw) * x + Math.cos(yaw) * z;
        return new double[]{a, Math.cos(pitch) * y - Math.sin(pitch) * b, Math.sin(pitch) * y + Math.cos(pitch) * b};
    }
    public double[] inverse(double x, double y, double z) {
        double a = Math.cos(pitch) * y + Math.sin(pitch) * z;
        double b = -Math.sin(pitch) * y + Math.cos(pitch) * z;
        return new double[]{Math.cos(yaw) * x - Math.sin(yaw) * b, a, Math.sin(yaw) * x + Math.cos(yaw) * b};
    }
    public double[] rayOrigin(double screenX, double screenY) {
        double[] p = inverse(screenX, screenY, 3);
        for (int i = 0; i < 3; i++) p[i] += 0.5;
        return p;
    }
    public double[] rayDirection() { return inverse(0, 0, -1); }
    public void orbit(double dx, double dy) {
        yaw += dx * 0.012;
        pitch = Math.clamp(pitch + dy * 0.012, -Math.PI / 2 + 0.01, Math.PI / 2 - 0.01);
    }
    public void frame() { yaw = Math.toRadians(-35); pitch = Math.toRadians(24); zoom = 1; panX = 0; panY = 0; }
}
