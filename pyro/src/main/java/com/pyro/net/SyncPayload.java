package com.pyro.net;

import com.pyro.Pyro;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> owner: everything the HUD shows. cd = remaining cooldown ticks per power. */
public record SyncPayload(boolean awakened, int energy, int max, int xp, int level, int[] cd,
                          boolean aura) implements CustomPayload {
    public static final CustomPayload.Id<SyncPayload> ID = new CustomPayload.Id<>(Pyro.id("sync"));
    public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC = PacketCodec.of(SyncPayload::write, SyncPayload::read);

    private static void write(SyncPayload p, RegistryByteBuf b) {
        b.writeBoolean(p.awakened());
        b.writeVarInt(p.energy());
        b.writeVarInt(p.max());
        b.writeVarInt(p.xp());
        b.writeVarInt(p.level());
        b.writeVarInt(p.cd().length);
        for (int v : p.cd()) b.writeVarInt(v);
        b.writeBoolean(p.aura());
    }

    private static SyncPayload read(RegistryByteBuf b) {
        boolean awakened = b.readBoolean();
        int energy = b.readVarInt();
        int max = b.readVarInt();
        int xp = b.readVarInt();
        int level = b.readVarInt();
        int n = b.readVarInt();
        int[] cd = new int[n];
        for (int i = 0; i < n; i++) cd[i] = b.readVarInt();
        boolean aura = b.readBoolean();
        return new SyncPayload(awakened, energy, max, xp, level, cd, aura);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
