package com.arcane;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Настройки в config/arcane.properties */
public final class ArcConfig {
    /** Ник игрока-легенды. */
    public static String legendName = "Ark";
    /** Шанс осколка в каждом сундуке подземелья (0.0 - 1.0). */
    public static float shardChance = 0.05f;

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("arcane.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path)) { p.load(r); }
            } else {
                p.setProperty("legendName", legendName);
                p.setProperty("shardChance", String.valueOf(shardChance));
                try (Writer w = Files.newBufferedWriter(path)) {
                    p.store(w, "Arcane config: legendName = ник легенды, shardChance = шанс осколка в сундуках");
                }
            }
            legendName = p.getProperty("legendName", legendName).trim();
            shardChance = Float.parseFloat(p.getProperty("shardChance", String.valueOf(shardChance)));
        } catch (IOException | NumberFormatException e) {
            ArcaneMod.LOGGER.warn("Не удалось прочитать конфиг, используются значения по умолчанию", e);
        }
    }
}
