package com.pyro.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Saved with the player and kept after death. */
public record PlayerData(boolean awakened, int energy, int xp) {
    public static final PlayerData DEFAULT = new PlayerData(false, 0, 0);
    /** Mastery needed for Spark, Flame, Inferno, Sun. */
    public static final int[] LEVEL_XP = {0, 150, 500, 1200};

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("awakened").forGetter(PlayerData::awakened),
            Codec.INT.fieldOf("energy").forGetter(PlayerData::energy),
            Codec.INT.fieldOf("xp").forGetter(PlayerData::xp)
    ).apply(i, PlayerData::new));

    public static int levelOf(int xp) {
        int l = 0;
        for (int i = 0; i < LEVEL_XP.length; i++) if (xp >= LEVEL_XP[i]) l = i;
        return l;
    }

    public static int maxEnergy(int xp) {
        return 100 + 25 * levelOf(xp);
    }

    public int level() {
        return levelOf(xp);
    }

    public int max() {
        return maxEnergy(xp);
    }

    public PlayerData withAwakened(boolean v) { return new PlayerData(v, energy, xp); }
    public PlayerData withEnergy(int v) { return new PlayerData(awakened, Math.max(0, Math.min(max(), v)), xp); }
    public PlayerData withXp(int v) { return new PlayerData(awakened, energy, v); }
}
