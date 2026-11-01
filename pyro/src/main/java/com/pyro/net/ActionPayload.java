package com.pyro.net;

import com.pyro.Pyro;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client -> server: a power key was pressed (slot 1..5). */
public record ActionPayload(int slot) implements CustomPayload {
    public static final CustomPayload.Id<ActionPayload> ID = new CustomPayload.Id<>(Pyro.id("action"));
    public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ActionPayload::slot,
            ActionPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
