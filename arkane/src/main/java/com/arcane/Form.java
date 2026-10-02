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
    /** Шанс Сердца Эйро в каждом сундуке данжей Нижнего мира (0.0 - 1.0). */
    public static float eiroHeartChance = 0.05f;
    /** Шанс Карты Шута в каждом сундуке деревни. */
    public static float jesterCardChance = 0.03f;

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("arcane.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path)) { p.load(r); }
            } else {
                p.setProperty("legendName", legendName);
                p.setProperty("shardChance", String.valueOf(shardChance));
                p.setProperty("eiroHeartChance", String.valueOf(eiroHeartChance));
                p.setProperty("jesterCardChance", String.valueOf(jesterCardChance));
                try (Writer w = Files.newBufferedWriter(path)) {
                    p.store(w, "Arcane config: legendName = ник легенды, shardChance = шанс осколка в сундуках");
                }
            }
            legendName = p.getProperty("legendName", legendName).trim();
            shardChance = Float.parseFloat(p.getProperty("shardChance", String.valueOf(shardChance)));
            eiroHeartChance = Float.parseFloat(p.getProperty("eiroHeartChance", String.valueOf(eiroHeartChance)));
            jesterCardChance = Float.parseFloat(p.getProperty("jesterCardChance", String.valueOf(jesterCardChance)));
        } catch (IOException | NumberFormatException e) {
            ArcaneMod.LOGGER.warn("Не удалось прочитать конфиг, используются значения по умолчанию", e);
        }
    }
}
