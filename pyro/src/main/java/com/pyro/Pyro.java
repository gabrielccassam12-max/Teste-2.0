package com.pyro;

import com.pyro.item.ModItems;
import com.pyro.net.ActionPayload;
import com.pyro.net.SyncPayload;
import com.pyro.particle.ModParticles;
import com.pyro.power.PowerManager;
import com.pyro.util.Effects;
import com.pyro.util.Scheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pyro implements ModInitializer {
    public static final String ID = "pyro";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    public static Identifier id(String path) {
        return Identifier.of(ID, path);
    }

    @Override
    public void onInitialize() {
        ModParticles.init();
        Effects.init();
        ModItems.init();
        Scheduler.init();
        PowerManager.init();

        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, context) ->
                PowerManager.cast(context.player(), payload.slot()));
        LOG.info("Pyro loaded: 3D cube-particle fire effects.");
    }
}
