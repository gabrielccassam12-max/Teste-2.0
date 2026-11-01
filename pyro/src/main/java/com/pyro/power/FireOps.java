package com.pyro.power;

import com.pyro.particle.Fx;
import com.pyro.util.Effects;
import com.pyro.util.Snd;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

import java.util.List;
import java.util.Random;

/** Damage, explosions and fire-setting shared by every fire effect. */
public final class FireOps {
    private FireOps() {}

    /** Set to false to stop fire effects from lighting blocks on fire. */
    public static boolean SCORCH_BLOCKS = true;

    private static final Random R = new Random();

    public static List<LivingEntity> targets(ServerWorld w, Box box, PlayerEntity owner) {
        boolean pvp = w.getServer() != null && w.getServer().isPvpEnabled();
        return w.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != owner && !e.isSpectator()
                && !(e instanceof ArmorStandEntity) && (pvp || !(e instanceof PlayerEntity)));
    }

    public static void hurt(ServerWorld w, LivingEntity target, PlayerEntity owner, float amount, int fireSeconds) {
        DamageSource src = owner != null ? w.getDamageSources().playerAttack(owner) : w.getDamageSources().inFire();
        target.damage(src, amount);
        if (fireSeconds > 0) target.setOnFireFor(fireSeconds);
    }

    /** Fiery explosion: damage with fall-off, knockback, accent particles and (optionally) scorched ground. */
    public static void blast(ServerWorld w, Vec3d c, PlayerEntity owner, double radius, float damage, int particles, boolean scorch) {
        Box box = new Box(c.x - radius, c.y - radius, c.z - radius, c.x + radius, c.y + radius, c.z + radius);
        for (LivingEntity e : targets(w, box, owner)) {
            double d = e.getPos().add(0, e.getHeight() / 2, 0).distanceTo(c);
            if (d > radius) continue;
            float k = (float) (1.0 - 0.55 * d / radius);
            hurt(w, e, owner, damage * k, 8);
            Vec3d push = e.getPos().add(0, 0.5, 0).subtract(c);
            push = push.lengthSquared() < 0.01 ? new Vec3d(0, 1, 0) : push.normalize();
            double s = 1.1 * k;
            e.addVelocity(push.x * s, 0.35 + push.y * s * 0.6, push.z * s);
            e.velocityModified = true;
        }
        Fx.blastCubes(w, c, particles, radius * 0.1);
        Effects.groundRing(w, c.add(0, -0.3, 0), radius * 1.1, 10);
        Snd.play(w, c, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.6f, 0.9f);
        Snd.play(w, c, SoundEvents.ENTITY_BLAZE_SHOOT, 1.2f, 0.5f);
        if (scorch && SCORCH_BLOCKS && w.getGameRules().getBoolean(GameRules.DO_FIRE_TICK)
                && w.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            int tries = (int) (radius * 5);
            for (int i = 0; i < tries; i++) {
                BlockPos p = BlockPos.ofFloored(c.x + R.nextGaussian() * radius * 0.45, c.y + R.nextInt(3) - 1, c.z + R.nextGaussian() * radius * 0.45);
                if (w.getBlockState(p).isAir() && Blocks.FIRE.getDefaultState().canPlaceAt(w, p)) {
                    w.setBlockState(p, Blocks.FIRE.getDefaultState());
                }
            }
        }
    }
}
