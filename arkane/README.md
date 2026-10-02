package com.arcane.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Небольшая панель карт Шута в правом верхнем углу. */
public final class JesterHud {
    private static int[] ids = new int[0];
    private static int[] ttl = new int[0];
    private JesterHud() {}
    public static void setCards(int[] newIds, int[] newTtl, boolean fresh) { ids = newIds; ttl = newTtl; }
    public static void clear() { ids = new int[0]; ttl = new int[0]; }
    public static void render(DrawContext ctx, float tickDelta) {
        if (ids.length == 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int x = mc.getWindow().getScaledWidth() - 8;
        int y = 8;
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal("ШУТ").formatted(Formatting.GOLD), x - 64, y, 0xFFFFFF);
        for (int i = 0; i < ids.length; i++) {
            String label = label(ids[i]);
            int color = ids[i] < 4 ? 0x55FF55 : 0xFF5555;
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label).formatted(Formatting.BOLD), x - 64, y + 14 + i * 13, color);
        }
    }
    private static String label(int id) {
        return switch (id) {
            case 0 -> "Удача ×1.5"; case 1 -> "Удача ×2"; case 2 -> "Удача ×3"; case 3 -> "Удача ×4";
            case 4 -> "Неудача ÷1.5"; case 5 -> "Неудача ÷2"; case 6 -> "Неудача ÷3"; case 7 -> "Неудача ÷4";
            default -> "?";
        };
    }
}
