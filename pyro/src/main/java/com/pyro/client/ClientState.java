package com.pyro.client;

import com.pyro.net.SyncPayload;
import net.minecraft.client.MinecraftClient;

/** Latest server data for the HUD, with cooldowns converted to client tick deadlines. */
public final class ClientState {
    private ClientState() {}

    public static SyncPayload last;
    public static long[] cdEnd = new long[8];
    public static float shownEnergy;
    private static long clock;

    public static long time() {
        return clock;
    }

    public static void tick(MinecraftClient mc) {
        clock++;
    }

    public static void set(SyncPayload p) {
        last = p;
        if (p == null) return;
        for (int i = 0; i < cdEnd.length && i < p.cd().length; i++) cdEnd[i] = clock + p.cd()[i];
    }
}
