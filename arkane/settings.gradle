package com.arcane.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Какая форма сейчас включена у каких игроков (0 - нет, 1 - Саир, 2 - Эйро). */
public final class ClientForms {
    private static final Map<UUID, Integer> FORMS = new ConcurrentHashMap<>();

    private ClientForms() {}

    public static int get(UUID id) { return FORMS.getOrDefault(id, 0); }

    public static void set(UUID id, int form) {
        if (form == 0) FORMS.remove(id); else FORMS.put(id, form);
    }

    public static void clear() { FORMS.clear(); }
}
