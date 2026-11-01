package com.pyro.power;

import com.pyro.particle.Fx;
import com.pyro.util.Effects;
import com.pyro.util.Snd;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** What each power looks like and does. Every visual is made of 3D cube particles. */
public final class Powers {
    private Powers() {}

    private static final Random R = Fx.R;

    private static Vec3d rightOf(Vec3d look) {
        Vec3d r = look.crossProduct(new Vec3d(0, 1, 0));
        return r.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : r.normalize();
    }

    // ------------------------------------------------------------------ 1. Flamethrower
    public static void flamethrower(ServerWorld w, ServerPlayerEntity p, float mult) {
        Effects.add(t -> {
            if (t > 30 || !p.isAlive() || p.isRemoved()) return false;
            Vec3d eye = p.getEyePos();
            Vec3d look = p.getRotationVec(1f);
            Vec3d right = rightOf(look);
            Vec3d up = right.crossProduct(look);
            Vec3d o = eye.add(look.multiply(0.9)).add(right.multiply(0.3)).add(up.multiply(-0.3));
            for (int i = 0; i < 16; i++) {
                Vec3d v = look.multiply(0.5 + R.nextDouble() * 0.35)
                        .add(right.multiply(R.nextGaussian() * 0.06)).add(up.multiply(R.nextGaussian() * 0.06));
                Fx.vel(w, "cube_fire", o, v);
            }
            for (int i = 0; i < 3; i++) {
                Vec3d v = look.multiply(0.4 + R.nextDouble() * 0.3)
                        .add(right.multiply(R.nextGaussian() * 0.09)).add(up.multiply(R.nextGaussian() * 0.09 + 0.02));
                Fx.vel(w, "cube_ember", o, v);
            }
            if (t % 2 == 0) Fx.vel(w, "cube_big", o.add(look.multiply(0.6)), look.multiply(0.45).add(Fx.jitter(0.03)));
            if (t % 2 == 1) Fx.vel(w, "cube_core", o.add(look.multiply(0.3)), look.multiply(0.2));
            if (t % 4 == 1) Snd.play(w, eye, SoundEvents.ITEM_FIRECHARGE_USE, 0.7f, 0.7f);
            if (t % 3 == 0) {
                for (LivingEntity e : FireOps.targets(w, new Box(eye.subtract(10, 10, 10), eye.add(10, 10, 10)), p)) {
                    Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(eye);
                    double d = to.length();
                    if (d < 0.5 || d > 9.5) continue;
                    if (to.normalize().dot(look) < 0.92) continue;
                    FireOps.hurt(w, e, p, 2.4f * mult, 4);
                }
            }
            return true;
        });
    }

    // ------------------------------------------------------------------ 2. Fire jets (fly forward on flames)
    public static void jets(ServerWorld w, ServerPlayerEntity p, float mult) {
        Set<Integer> hit = new HashSet<>();
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_BLAZE_SHOOT, 1.4f, 0.6f);
        Effects.add(t -> {
            if (t > 22 || !p.isAlive() || p.isRemoved()) {
                if (p.isAlive()) p.fallDistance = 0;
                return false;
            }
            Vec3d look = p.getRotationVec(1f);
            Vec3d dir = new Vec3d(look.x, look.y * 0.9 + 0.08, look.z).normalize();
            double speed = Math.min(0.95, 0.35 + t * 0.12);
            p.setVelocity(dir.multiply(speed));
            p.velocityModified = true;
            p.fallDistance = 0;

            Vec3d back = dir.multiply(-1);
            Vec3d right = rightOf(dir);
            Vec3d up = right.crossProduct(dir);
            Vec3d base = p.getPos().add(0, 0.45, 0);
            for (int side = -1; side <= 1; side += 2) {
                Vec3d o = base.add(right.multiply(0.22 * side)).add(back.multiply(0.2));
                for (int i = 0; i < 9; i++) {
                    Vec3d v = back.multiply(0.3 + R.nextDouble() * 0.45)
                            .add(right.multiply(R.nextGaussian() * 0.05)).add(up.multiply(R.nextGaussian() * 0.05));
                    Fx.vel(w, "cube_fire", o, v);
                }
                Fx.vel(w, "cube_ember", o, back.multiply(0.35).add(Fx.jitter(0.08)));
            }
            Fx.vel(w, "cube_big", base.add(back.multiply(0.5)), back.multiply(0.25).add(Fx.jitter(0.03)));
            Fx.vel(w, "cube_core", base.add(back.multiply(0.3)), back.multiply(0.1));
            if (t % 6 == 1) Snd.play(w, p.getPos(), SoundEvents.ITEM_FIRECHARGE_USE, 0.8f, 0.6f);

            for (LivingEntity e : FireOps.targets(w, p.getBoundingBox().expand(1.4), p)) {
                if (!hit.add(e.getId())) continue;
                FireOps.hurt(w, e, p, 5f * mult, 5);
                Vec3d k = dir.multiply(0.8);
                e.addVelocity(k.x, 0.3, k.z);
                e.velocityModified = true;
            }
            return true;
        });
    }

    // ------------------------------------------------------------------ 3. Fire ring (nova)
    public static void ring(ServerWorld w, ServerPlayerEntity p, float mult) {
        Vec3d c = p.getPos();
        double maxR = 9.0;
        int ticks = 14;
        Fx.blastCubes(w, c.add(0, 0.2, 0), 90, 0.6);
        Effects.groundRing(w, c.add(0, -0.3, 0), maxR, ticks);
        Snd.play(w, c, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0f, 1.3f);
        Snd.play(w, c, SoundEvents.ENTITY_BLAZE_SHOOT, 1.3f, 0.5f);
        Set<Integer> hit = new HashSet<>();
        Effects.add(t -> {
            if (t > ticks) return false;
            double r = Effects.ringRadius(maxR, t, ticks);
            Box box = new Box(c.x - r - 2, c.y - 1, c.z - r - 2, c.x + r + 2, c.y + 2.5, c.z + r + 2);
            for (LivingEntity e : FireOps.targets(w, box, p)) {
                if (hit.contains(e.getId())) continue;
                double dx = e.getX() - c.x, dz = e.getZ() - c.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(d - r) > 1.8) continue;
                hit.add(e.getId());
                FireOps.hurt(w, e, p, 7f * mult, 6);
                double n = Math.max(0.2, d);
                e.addVelocity(dx / n * 1.2, 0.45, dz / n * 1.2);
                e.velocityModified = true;
            }
            return true;
        });
    }

    // ------------------------------------------------------------------ 4. Fireball
    public static void fireball(ServerWorld w, ServerPlayerEntity p, float mult) {
        Vec3d look = p.getRotationVec(1f);
        Vec3d dir = look.multiply(1.2);
        Vec3d[] pos = {p.getEyePos().add(look.multiply(1.2)).add(0, -0.25, 0)};
        Snd.play(w, p.getEyePos(), SoundEvents.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);
        Effects.add(t -> {
            if (t > 60 || !p.isAlive()) return false;
            Vec3d old = pos[0], next = old.add(dir);
            BlockHitResult bh = w.raycast(new RaycastContext(old, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, p));
            boolean block = bh.getType() != HitResult.Type.MISS;
            Vec3d at = block ? bh.getPos().subtract(dir.normalize().multiply(0.4)) : next;
            List<LivingEntity> near = FireOps.targets(w, new Box(old, next).expand(0.8), p);
            if (block || !near.isEmpty()) {
                FireOps.blast(w, at, p, 4.4, 9f * mult, 100, true);
                return false;
            }
            pos[0] = next;
            for (int i = 0; i < 6; i++) Fx.vel(w, "cube_core", next.add(Fx.jitter(0.13)), Vec3d.ZERO);
            for (int i = 0; i < 10; i++)
                Fx.vel(w, "cube_fire", next.add(Fx.jitter(0.25)), dir.multiply(-0.12).add(Fx.jitter(0.03)));
            Fx.vel(w, "cube_big", next.add(Fx.jitter(0.15)), dir.multiply(-0.05).add(Fx.jitter(0.02)));
            Fx.vel(w, "cube_big", next.add(Fx.jitter(0.15)), Vec3d.ZERO);
            for (int i = 0; i < 2; i++) Fx.vel(w, "cube_ember", next, Fx.jitter(0.07).add(0, 0.03, 0));
            return true;
        });
    }

    // ------------------------------------------------------------------ 5. Meteor
    public static void meteor(ServerWorld w, ServerPlayerEntity p, Vec3d target, float mult) {
        Vec3d start = target.add(4, 38, 2);
        Vec3d dir = target.subtract(start).normalize().multiply(1.6);
        Vec3d[] pos = {start};
        Snd.play(w, p.getEyePos(), SoundEvents.ENTITY_GHAST_SHOOT, 1.5f, 0.5f);
        Effects.add(t -> {
            if (t > 70) return false;
            if (t % 5 == 1) Effects.groundRing(w, target.add(0, -0.3, 0), 3.2, 5);
            Vec3d old = pos[0], next = old.add(dir);
            BlockHitResult bh = w.raycast(new RaycastContext(old, next.add(dir.normalize().multiply(1.5)),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (bh.getType() != HitResult.Type.MISS) {
                Vec3d at = bh.getPos().add(0, 0.5, 0);
                FireOps.blast(w, at, p, 7.5, 20f * mult, 260, true);
                Snd.play(w, at, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.0f, 0.6f);
                Effects.groundRing(w, at.add(0, -0.8, 0), 4.0, 8);
                return false;
            }
            pos[0] = next;
            if (t % 6 == 1) Snd.play(w, next, SoundEvents.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);
            for (int i = 0; i < 14; i++) Fx.vel(w, "cube_core", next.add(Fx.jitter(0.4)), Vec3d.ZERO);
            for (int i = 0; i < 6; i++) Fx.vel(w, "cube_big", next.add(Fx.jitter(0.6)), new Vec3d(0, 0.05, 0).add(Fx.jitter(0.03)));
            for (int i = 0; i < 24; i++)
                Fx.vel(w, "cube_fire", next.add(Fx.jitter(0.5)), dir.multiply(-0.18).add(0, 0.05, 0).add(Fx.jitter(0.05)));
            for (int i = 0; i < 6; i++) Fx.vel(w, "cube_ember", next.add(Fx.jitter(0.5)), Fx.jitter(0.1).add(0, 0.1, 0));
            for (int i = 0; i < 2; i++) Fx.vel(w, "cube_smoke", next.add(Fx.jitter(0.5)), new Vec3d(0, 0.03, 0));
            return true;
        });
    }

    // ------------------------------------------------------------------ 6. Fire aura (toggle)
    public static Effects.Handle aura(ServerWorld w, ServerPlayerEntity p, float mult, int maxTicks) {
        Effects.Handle h = new Effects.Handle();
        Snd.play(w, p.getPos(), SoundEvents.ITEM_FIRECHARGE_USE, 1.2f, 0.6f);
        Effects.add(t -> {
            if (h.dead || t > maxTicks || !p.isAlive() || p.isRemoved()) {
                h.dead = true;
                return false;
            }
            Vec3d c = p.getPos();
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
            for (int i = 0; i < 7; i++) {
                double a = R.nextDouble() * Math.PI * 2, rr = 0.4 + R.nextDouble() * 0.5;
                Fx.vel(w, "cube_fire", c.x + Math.cos(a) * rr, c.y + R.nextDouble() * 1.9, c.z + Math.sin(a) * rr,
                        0, 0.05 + R.nextDouble() * 0.08, 0);
            }
            for (int k = 0; k < 3; k++) {
                double a = t * 0.35 + k * 2.094;
                double y = ((t * 2 + k * 13) % 40) / 40.0 * 1.9;
                Fx.vel(w, "cube_core", c.x + Math.cos(a) * 0.8, c.y + y, c.z + Math.sin(a) * 0.8,
                        -Math.sin(a) * 0.03, 0.03, Math.cos(a) * 0.03);
            }
            if (t % 3 == 0) {
                double a = R.nextDouble() * Math.PI * 2;
                Fx.vel(w, "cube_ember", c.x + Math.cos(a) * 0.7, c.y + R.nextDouble() * 1.2, c.z + Math.sin(a) * 0.7, 0, 0.1, 0);
            }
            if (t % 10 == 0) {
                for (LivingEntity e : FireOps.targets(w, new Box(c.x - 3.4, c.y - 1, c.z - 3.4, c.x + 3.4, c.y + 3, c.z + 3.4), p)) {
                    if (e.squaredDistanceTo(c) > 3.4 * 3.4) continue;
                    FireOps.hurt(w, e, p, 2.5f * mult, 4);
                }
            }
            if (t % 40 == 1) Snd.play(w, c, SoundEvents.BLOCK_FIRE_AMBIENT, 0.8f, 0.8f);
            return true;
        });
        return h;
    }

    // ------------------------------------------------------------------ awakening column (cosmetic)
    public static void column(ServerWorld w, Vec3d base, int ticks) {
        Effects.add(t -> {
            if (t > ticks) return false;
            for (int i = 0; i < 18; i++) {
                double a = R.nextDouble() * Math.PI * 2, rr = R.nextDouble() * 1.3;
                Fx.vel(w, "cube_fire", base.x + Math.cos(a) * rr, base.y + R.nextDouble() * 2, base.z + Math.sin(a) * rr,
                        -Math.sin(a) * 0.05, 0.25 + R.nextDouble() * 0.3, Math.cos(a) * 0.05);
            }
            for (int i = 0; i < 3; i++) Fx.vel(w, "cube_big", base.x + R.nextGaussian() * 0.5, base.y + 0.5, base.z + R.nextGaussian() * 0.5, 0, 0.2 + R.nextDouble() * 0.15, 0);
            return true;
        });
    }
}
