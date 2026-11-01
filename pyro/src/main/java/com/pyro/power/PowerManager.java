package com.pyro.power;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pyro.Pyro;
import com.pyro.net.SyncPayload;
import com.pyro.particle.Fx;
import com.pyro.util.Effects;
import com.pyro.util.Scheduler;
import com.pyro.util.Snd;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Server brain: saved data, heat (energy), casting rules, cooldowns, awakening and HUD sync. */
public final class PowerManager {
    private PowerManager() {}

    public static final AttachmentType<PlayerData> DATA = AttachmentRegistry.<PlayerData>builder()
            .persistent(PlayerData.CODEC)
            .copyOnDeath()
            .buildAndRegister(Pyro.id("pyrokinetic"));

    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Effects.Handle> AURAS = new HashMap<>();
    private static final Set<UUID> AWAKENING = new HashSet<>();
    private static final Random R = new Random();

    public static PlayerData get(ServerPlayerEntity p) {
        return ((AttachmentTarget) p).getAttachedOrElse(DATA, PlayerData.DEFAULT);
    }

    public static void set(ServerPlayerEntity p, PlayerData d) {
        ((AttachmentTarget) p).setAttached(DATA, d);
    }

    private static void note(ServerPlayerEntity p, String key, Formatting f) {
        p.sendMessage(Text.translatable(key).formatted(f), true);
    }

    // ------------------------------------------------------------ init
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTicks();
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                PlayerData d = get(p);
                if (!d.awakened() || !p.isAlive()) continue;
                boolean dirty = false;
                if (tick % 8 == 0 && d.energy() < d.max()) {
                    d = d.withEnergy(d.energy() + 1 + d.level() / 2);
                    dirty = true;
                }
                if (auraActive(p) && tick % 20 == 0) {
                    d = d.withEnergy(d.energy() - 3);
                    dirty = true;
                    if (d.energy() <= 0) {
                        stopAura(p);
                        note(p, "message.pyro.aura_off", Formatting.GOLD);
                    }
                }
                if (dirty) set(p, d);
                if (dirty || tick % 10 == 0) sync(p);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            COOLDOWNS.remove(id);
            AURAS.remove(id);
            AWAKENING.remove(id);
        });

        // Natural awakening: surviving a brush with death can wake the fire (chance, once)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity sp && !get(sp).awakened() && !AWAKENING.contains(sp.getUuid())
                    && sp.getHealth() - amount <= 6.0f && sp.getHealth() - amount > 0 && R.nextFloat() < 0.15f) {
                sp.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 4));
                startAwakening(sp);
            }
            return true;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("pyro")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("awaken").executes(ctx -> {
                            startAwakening(ctx.getSource().getPlayerOrThrow());
                            return 1;
                        }))
                        .then(CommandManager.literal("instant").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            PlayerData d = get(p).withAwakened(true);
                            set(p, d.withEnergy(d.max()));
                            sync(p);
                            return 1;
                        }))
                        .then(CommandManager.literal("master").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            PlayerData d = get(p).withAwakened(true).withXp(1200);
                            set(p, d.withEnergy(d.max()));
                            sync(p);
                            return 1;
                        }))
                        .then(CommandManager.literal("energy").then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 1000))
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                    set(p, get(p).withEnergy(IntegerArgumentType.getInteger(ctx, "amount")));
                                    sync(p);
                                    return 1;
                                })))
                        .then(CommandManager.literal("reset").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            stopAura(p);
                            set(p, PlayerData.DEFAULT);
                            note(p, "message.pyro.reset", Formatting.GRAY);
                            sync(p);
                            return 1;
                        }))));
    }

    // ------------------------------------------------------------ sync
    public static void sync(ServerPlayerEntity p) {
        PlayerData d = get(p);
        long now = p.getServerWorld().getTime();
        long[] cd = COOLDOWNS.get(p.getUuid());
        int[] left = new int[Power.values().length];
        for (int i = 0; i < left.length; i++) left[i] = cd == null ? 0 : (int) Math.max(0, cd[i] - now);
        if (ServerPlayNetworking.canSend(p, SyncPayload.ID)) {
            ServerPlayNetworking.send(p, new SyncPayload(d.awakened(), d.energy(), d.max(), d.xp(), d.level(), left, auraActive(p)));
        }
    }

    // ------------------------------------------------------------ aura helpers
    public static boolean auraActive(ServerPlayerEntity p) {
        Effects.Handle h = AURAS.get(p.getUuid());
        if (h == null) return false;
        if (h.dead) {
            AURAS.remove(p.getUuid());
            return false;
        }
        return true;
    }

    public static void stopAura(ServerPlayerEntity p) {
        Effects.Handle h = AURAS.remove(p.getUuid());
        if (h != null) h.dead = true;
    }

    // ------------------------------------------------------------ casting
    public static void cast(ServerPlayerEntity p, int slot) {
        if (slot < 1 || slot > Power.values().length || !p.isAlive() || p.isSpectator()) return;
        PlayerData d = get(p);
        if (!d.awakened()) {
            note(p, "message.pyro.cold", Formatting.GRAY);
            return;
        }
        Power pw = Power.values()[slot - 1];
        ServerWorld w = (ServerWorld) p.getWorld();
        long now = w.getTime();
        long[] cd = COOLDOWNS.computeIfAbsent(p.getUuid(), k -> new long[Power.values().length]);

        if (pw == Power.AURA && auraActive(p)) {
            stopAura(p);
            cd[slot - 1] = now + pw.cooldown;
            note(p, "message.pyro.aura_off", Formatting.GOLD);
            sync(p);
            return;
        }
        if (now < cd[slot - 1]) {
            note(p, "message.pyro.cooldown", Formatting.GRAY);
            return;
        }
        if (d.energy() < pw.cost) {
            note(p, "message.pyro.energy", Formatting.RED);
            return;
        }

        float mult = 1f + 0.2f * d.level();
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);

        switch (pw) {
            case FLAMETHROWER -> Powers.flamethrower(w, p, mult);
            case JETS -> Powers.jets(w, p, mult);
            case RING -> Powers.ring(w, p, mult);
            case FIREBALL -> Powers.fireball(w, p, mult);
            case METEOR -> Powers.meteor(w, p, aimPoint(w, p, eye, look, 60.0, true), mult);
            case AURA -> AURAS.put(p.getUuid(), Powers.aura(w, p, mult, 100000));
        }

        cd[slot - 1] = now + pw.cooldown;
        int xpGain = 2 + pw.cost / 10;
        PlayerData nd = d.withEnergy(d.energy() - pw.cost).withXp(d.xp() + xpGain);
        if (nd.level() > d.level()) {
            p.sendMessage(Text.literal("★ ").formatted(Formatting.GOLD)
                    .append(Text.translatable("level.pyro." + nd.level()).formatted(Formatting.GOLD)), true);
        }
        set(p, nd);
        sync(p);
    }

    /** Point the player is aiming at (block hit), or a spot in front of them on the ground. */
    private static Vec3d aimPoint(ServerWorld w, ServerPlayerEntity p, Vec3d eye, Vec3d look, double range, boolean ground) {
        Vec3d end = eye.add(look.multiply(range));
        BlockHitResult bh = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) {
            Vec3d h = bh.getPos();
            return bh.getSide().getAxis().isVertical() ? h : h.add(bh.getSide().getOffsetX() * 0.6, 0, bh.getSide().getOffsetZ() * 0.6);
        }
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        flat = flat.lengthSquared() < 1e-4 ? new Vec3d(0, 0, 1) : flat.normalize();
        return p.getPos().add(flat.multiply(Math.min(range, 9.0)));
    }

    // ------------------------------------------------------------ awakening
    public static boolean startAwakening(ServerPlayerEntity p) {
        if (get(p).awakened()) {
            note(p, "message.pyro.already", Formatting.YELLOW);
            return false;
        }
        if (!AWAKENING.add(p.getUuid())) return false;
        ServerWorld w = (ServerWorld) p.getWorld();
        note(p, "message.pyro.begin", Formatting.GOLD);
        Snd.play(w, p.getPos(), SoundEvents.BLOCK_FIRE_AMBIENT, 1.5f, 0.5f);
        Powers.aura(w, p, 0f, 90);

        Scheduler.later(25, () -> {
            if (!stillHere(p)) return;
            Snd.play(w, p.getPos(), SoundEvents.ENTITY_BLAZE_SHOOT, 1.4f, 0.5f);
            Effects.groundRing(w, p.getPos().add(0, -0.3, 0), 6.0, 12);
            Fx.blastCubes(w, p.getPos().add(0, 0.3, 0), 50, 0.4);
        });
        Scheduler.later(50, () -> {
            if (!stillHere(p)) return;
            Powers.column(w, p.getPos(), 22);
            Fx.blastCubes(w, p.getPos().add(0, 1, 0), 120, 0.6);
            Effects.groundRing(w, p.getPos().add(0, -0.3, 0), 9.0, 14);
        });
        Scheduler.later(70, () -> {
            AWAKENING.remove(p.getUuid());
            if (!stillHere(p)) return;
            PlayerData d = get(p).withAwakened(true);
            set(p, d.withEnergy(d.max()));
            Snd.play(w, p.getPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(8, 60, 25));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("message.pyro.sub").formatted(Formatting.YELLOW)));
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("message.pyro.awakened").formatted(Formatting.GOLD, Formatting.BOLD)));
            sync(p);
        });
        return true;
    }

    private static boolean stillHere(ServerPlayerEntity p) {
        boolean ok = p.isAlive() && !p.isRemoved();
        if (!ok) AWAKENING.remove(p.getUuid());
        return ok;
    }
}
