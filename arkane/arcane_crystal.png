package com.arcane.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

/** "Голоса в голове" носителя Эйро: красный текст, всплывающий на экране. */
public final class VoiceHud {
    private static final String[] LINES = {
            "Покажи, кто ты",
            "Заставь их страдать",
            "Океаны крови"
    };
    private static final Random RNG = new Random();
    private static final int LIFE = 100;

    private static String text = null;
    private static int age = 0;
    private static int offX = 0;
    private static int offY = 0;

    private VoiceHud() {}

    public static void show(int index) {
        text = LINES[Math.floorMod(index, LINES.length)];
        age = 0;
        offX = RNG.nextInt(161) - 80;
        offY = RNG.nextInt(121) - 60;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.7f);
    }

    public static void tick() {
        if (text != null && ++age > LIFE) text = null;
    }

    public static void render(DrawContext ctx, float tickDelta) {
        if (text == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        float t = (age + tickDelta) / LIFE;
        float alpha = t < 0.2f ? t / 0.2f : (t > 0.7f ? (1.0f - t) / 0.3f : 1.0f);
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255);
        if (a < 10) return;

        int cx = mc.getWindow().getScaledWidth() / 2 + offX;
        int cy = mc.getWindow().getScaledHeight() / 2 + offY;
        float scale = 2.0f;

        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1.0f);
        ctx.drawCenteredTextWithShadow(mc.textRenderer,
                Text.literal(text).formatted(Formatting.ITALIC),
                (int) (cx / scale), (int) (cy / scale), (a << 24) | 0xB00000);
        ctx.getMatrices().pop();
    }
}
