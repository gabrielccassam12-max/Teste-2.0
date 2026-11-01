package com.pyro.particle;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Server-side helpers that spawn the 3D cube particles. */
public final class Fx {
    private Fx() {}

    public static final Random R = new Random();

    /** One cube with an exact velocity. */
    public static void vel(ServerWorld w, String type, double x, double y, double z, double vx, double vy, double vz) {
        w.spawnParticles(ModParticles.get(type), x, y, z, 0, vx, vy, vz, 1.0);
    }

    public static void vel(ServerWorld w, String type, Vec3d p, Vec3d v) {
        vel(w, type, p.x, p.y, p.z, v.x, v.y, v.z);
    }

    /** Gaussian random vector with the given spread. */
    public static Vec3d jitter(double s) {
        return new Vec3d(R.nextGaussian() * s, R.nextGaussian() * s, R.nextGaussian() * s);
    }

    /**
     * Explosion made of cubes: a low radial burst of small cubes, big soft blobs, rising embers,
     * a white-hot core and a little smoke. speed ~ 0.1 * (how far it should reach in blocks).
     */
    public static void blastCubes(ServerWorld w, Vec3d c, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double a = R.nextDouble() * Math.PI * 2;
            double elev = R.nextDouble() * 0.6;
            double sp = speed * (0.3 + R.nextDouble() * 0.9);
            vel(w, "cube_fire", c.x, c.y + 0.1, c.z, Math.cos(a) * Math.cos(elev) * sp, Math.sin(elev) * sp + 0.03, Math.sin(a) * Math.cos(elev) * sp);
        }
        for (int i = 0; i < Math.max(3, count / 6); i++) {
            double a = R.nextDouble() * Math.PI * 2;
            double sp = speed * (0.15 + R.nextDouble() * 0.5);
            vel(w, "cube_big", c.x, c.y + 0.2, c.z, Math.cos(a) * sp, 0.04 + R.nextDouble() * 0.08, Math.sin(a) * sp);
        }
        for (int i = 0; i < Math.max(4, count / 4); i++) {
            double a = R.nextDouble() * Math.PI * 2;
            double sp = speed * R.nextDouble() * 0.5;
            vel(w, "cube_ember", c.x, c.y + 0.2, c.z, Math.cos(a) * sp, 0.15 + R.nextDouble() * 0.45, Math.sin(a) * sp);
        }
        for (int i = 0; i < 12; i++) {
            Vec3d j = jitter(0.25);
            vel(w, "cube_core", c.add(j), jitter(0.03));
        }
        for (int i = 0; i < Math.max(2, count / 14); i++) {
            vel(w, "cube_smoke", c.x, c.y + 0.4, c.z, R.nextGaussian() * speed * 0.15, 0.05 + R.nextDouble() * 0.06, R.nextGaussian() * speed * 0.15);
        }
    }
}
