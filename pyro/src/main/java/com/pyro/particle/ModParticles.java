package com.pyro.particle;

import com.pyro.Pyro;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;

/** The mod's 3D cube particles. Names match assets/pyro/particles/<name>.json. */
public final class ModParticles {
    private ModParticles() {}

    public static final Map<String, CubeProfile> PROFILES = new LinkedHashMap<>();
    public static final Map<String, SimpleParticleType> TYPES = new LinkedHashMap<>();

    static {
        // name        young     middle    old       size  var   life var  grav    drag   shrink spin  flat  alpha inner
        PROFILES.put("cube_fire",  new CubeProfile(0xFFF3B0, 0xFFA928, 0xE2480F, 0.20f, 0.10f, 14, 6, 0.0f,    0.91f, 0.955f, 14f, 1f,    0.80f, true));
        PROFILES.put("cube_big",   new CubeProfile(0xFFF0A0, 0xFF9A22, 0xD83C0C, 0.55f, 0.25f, 11, 4, 0.0f,    0.88f, 0.930f, 8f,  1f,    0.55f, true));
        PROFILES.put("cube_core",  new CubeProfile(0xFFFFFF, 0xFFF0A0, 0xFFB030, 0.35f, 0.10f, 5,  2, 0.0f,    0.80f, 0.880f, 20f, 1f,    0.95f, true));
        PROFILES.put("cube_ember", new CubeProfile(0xFFEFA0, 0xFF9A20, 0xB8300A, 0.075f, 0.03f, 28, 12, -0.004f, 0.97f, 0.985f, 25f, 1f,   0.95f, false));
        PROFILES.put("cube_ring",  new CubeProfile(0xFFE9B0, 0xFFB444, 0xE07A22, 0.15f, 0.03f, 5,  1, 0.0f,    1.00f, 0.900f, 0f,  0.25f, 0.90f, false));
        PROFILES.put("cube_smoke", new CubeProfile(0x5A5550, 0x3A3735, 0x2A2828, 0.35f, 0.15f, 30, 10, -0.002f, 0.94f, 1.010f, 6f,  1f,    0.35f, false));
    }

    public static SimpleParticleType get(String name) {
        return TYPES.get(name);
    }

    public static void init() {
        for (String name : PROFILES.keySet()) {
            SimpleParticleType t = FabricParticleTypes.simple(true);
            Registry.register(Registries.PARTICLE_TYPE, Pyro.id(name), t);
            TYPES.put(name, t);
        }
    }
}
