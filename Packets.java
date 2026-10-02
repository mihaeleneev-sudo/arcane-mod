package com.arcane;

public enum Form {
    NONE(0, "none"),
    SAIR(1, "sair"),
    EIRO(2, "eiro"),
    JESTER(3, "jester");

    public final int id;
    public final String key;

    Form(int id, String key) {
        this.id = id;
        this.key = key;
    }

    public static Form fromId(int id) {
        for (Form f : values()) if (f.id == id) return f;
        return NONE;
    }
}
