package com.pyro.client;

import com.pyro.net.SyncPayload;
import com.pyro.power.Power;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/** Heat bar + the five powers with key, cost and cooldown. */
public final class PyroHud {
    private PyroHud() {}

    private static int argb(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static void render(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SyncPayload s = ClientState.last;
        if (mc.player == null || mc.options.hudHidden || s == null || !s.awakened()) return;
        long t = ClientState.time();

        ClientState.shownEnergy += (s.energy() - ClientState.shownEnergy) * 0.2f;
        float pct = Math.max(0f, Math.min(1f, ClientState.shownEnergy / Math.max(1, s.max())));

        int w = 132, x = g.getScaledWindowWidth() - w - 12, rows = Power.values().length;
        int y = g.getScaledWindowHeight() - 20 - rows * 12 - 38;

        // title + level
        g.drawTextWithShadow(mc.textRenderer, Text.translatable("hud.pyro.level", Text.translatable("level.pyro." + s.level())), x, y, 0xFFFFB347);
        // heat bar
        int by = y + 12;
        g.fill(x - 3, by - 3, x + w + 3, by + 13, argb(0x0B0F14, 0xB4));
        g.drawBorder(x - 3, by - 3, w + 6, 16, argb(0xFF7A1A, 0xFF));
        int fw = (int) (w * pct);
        if (fw > 0) {
            g.fillGradient(x, by, x + fw, by + 5, 0xFFFFE27A, 0xFFFF8A1E);
            g.fillGradient(x, by + 5, x + fw, by + 10, 0xFFFF8A1E, 0xFFC4300A);
            int gx = x + (int) ((t * 2) % Math.max(1, fw));
            g.fill(gx, by, Math.min(gx + 3, x + fw), by + 10, argb(0xFFFFFF, 0x45));
        }
        g.drawTextWithShadow(mc.textRenderer, Text.translatable("hud.pyro.energy", s.energy(), s.max()), x + 3, by + 1, 0xFFFFFFFF);

        // power list
        for (int i = 0; i < rows; i++) {
            Power pw = Power.values()[i];
            int ry = by + 20 + i * 12;
            long left = Math.max(0, ClientState.cdEnd[i] - t);
            boolean afford = s.energy() >= pw.cost;
            boolean active = pw == Power.AURA && s.aura();
            int col = active ? 0xFFFFD35A : left > 0 ? 0xFF8A8A96 : afford ? 0xFFFFFFFF : 0xFFFF7777;
            Text key = PyroClient.KEYS[i].getBoundKeyLocalizedText();
            g.fill(x - 3, ry - 1, x + w + 3, ry + 10, argb(0x0B0F14, 0x80));
            g.drawTextWithShadow(mc.textRenderer, Text.literal("[").append(key).append("] ").append(pw.displayName()), x, ry + 1, col);
            String c = String.valueOf(pw.cost);
            g.drawTextWithShadow(mc.textRenderer, c, x + w - mc.textRenderer.getWidth(c), ry + 1, afford ? 0xFFFFC66B : 0xFFFF7777);
            if (left > 0) {
                int total = Math.max(1, pw.cooldown);
                int cw = (int) (w * (1f - Math.min(1f, (float) left / total)));
                g.fill(x, ry + 9, x + w, ry + 10, argb(0x000000, 0x90));
                g.fill(x, ry + 9, x + cw, ry + 10, argb(0xFFD966, 0xFF));
            }
        }
    }
}
