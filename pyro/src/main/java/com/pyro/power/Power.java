package com.pyro.power;

import net.minecraft.text.Text;

/** The six Pyrokinetic powers (key slots 1..6). */
public enum Power {
    FLAMETHROWER(18, 70),
    JETS(25, 100),
    RING(28, 110),
    FIREBALL(12, 20),
    METEOR(60, 360),
    AURA(15, 50);

    public final int cost;
    public final int cooldown;

    Power(int cost, int cooldown) {
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public Text displayName() {
        return Text.translatable("power.pyro." + (ordinal() + 1));
    }
}
