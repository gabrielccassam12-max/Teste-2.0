package com.pyro.client;

import com.pyro.net.ActionPayload;
import com.pyro.net.SyncPayload;
import com.pyro.particle.ModParticles;
import com.pyro.particle.CubeProfile;
import com.pyro.power.Power;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.SimpleParticleType;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class PyroClient implements ClientModInitializer {
    public static final KeyBinding[] KEYS = new KeyBinding[Power.values().length];

    @Override
    public void onInitializeClient() {
        // ---- the mod's own particles
        for (Map.Entry<String, SimpleParticleType> en : ModParticles.TYPES.entrySet()) {
            CubeProfile pf = ModParticles.PROFILES.get(en.getKey());
            ParticleFactoryRegistry.getInstance().register(en.getValue(), sprites ->
                    (ParticleFactory<SimpleParticleType>) (type, world, x, y, z, vx, vy, vz) ->
                            new CubeParticle(world, x, y, z, vx, vy, vz, sprites, pf));
        }

        // ---- keys Z X C V B N
        int[] codes = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N};
        for (int i = 0; i < KEYS.length; i++) {
            KEYS[i] = KeyBindingHelper.registerKeyBinding(
                    new KeyBinding("key.pyro.s" + (i + 1), InputUtil.Type.KEYSYM, codes[i], "key.categories.pyro"));
        }

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> ClientState.set(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.set(null));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientState.tick(client);
            if (client.player == null || client.currentScreen != null) return;
            for (int i = 0; i < KEYS.length; i++) {
                while (KEYS[i].wasPressed()) ClientPlayNetworking.send(new ActionPayload(i + 1));
            }
        });

        HudRenderCallback.EVENT.register(PyroHud::render);
    }
}
