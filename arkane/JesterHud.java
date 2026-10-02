package com.arcane;

/** 8 карт Шута: 4 карты удачи и 4 карты неудачи. */
public enum Card {
    LUCK_1(0, true, 1, 1.5f),
    LUCK_2(1, true, 2, 2.0f),
    LUCK_3(2, true, 3, 3.0f),
    LUCK_4(3, true, 4, 4.0f),
    BAD_1(4, false, 1, 1.0f / 1.5f),
    BAD_2(5, false, 2, 0.5f),
    BAD_3(6, false, 3, 1.0f / 3.0f),
    BAD_4(7, false, 4, 0.25f);

    public static final int[] CRAFT_LOSS_PERCENT = {10, 25, 75, 100};
    private static final String[] LUCK_LABELS = {"×1.5", "×2", "×3", "×4"};
    private static final String[] BAD_LABELS = {"÷1.5", "÷2", "÷3", "÷4"};
    public final int id;
    public final boolean luck;
    public final int tier;
    public final float damageMult;
    Card(int id, boolean luck, int tier, float damageMult) { this.id=id; this.luck=luck; this.tier=tier; this.damageMult=damageMult; }
    public String label() { return luck ? LUCK_LABELS[tier-1] : BAD_LABELS[tier-1]; }
    public static Card byId(int id) { for (Card c: values()) if (c.id==id) return c; return LUCK_1; }
}
