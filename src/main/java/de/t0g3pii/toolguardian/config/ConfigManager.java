package de.t0g3pii.toolguardian.config;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    public static class Threshold {
        public final int percent;
        public final String messageKey;
        public final Sound sound; // optional
        public final float soundVolume;
        public final float soundPitch;

        public Threshold(int percent, String messageKey, Sound sound, float soundVolume, float soundPitch) {
            this.percent = percent;
            this.messageKey = messageKey;
            this.sound = sound;
            this.soundVolume = soundVolume;
            this.soundPitch = soundPitch;
        }
    }

    private final Plugin plugin;
    private String language;
    private final List<Threshold> thresholds = new ArrayList<>();
    private int checkIntervalTicks;
    private boolean monitorMainHand;
    private boolean monitorOffHand;
    private boolean monitorHead;
    private boolean monitorChest;
    private boolean monitorLegs;
    private boolean monitorFeet;
    private final Set<GameMode> enabledGamemodes = EnumSet.noneOf(GameMode.class);

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        this.language = cfg.getString("language", "de");

        this.enabledGamemodes.clear();
        for (String gm : cfg.getStringList("enabled-gamemodes")) {
            try {
                this.enabledGamemodes.add(GameMode.valueOf(gm.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        ConfigurationSection monitorSection = cfg.getConfigurationSection("monitor");
        this.monitorMainHand = monitorSection == null || monitorSection.getBoolean("main-hand", true);
        this.monitorOffHand = monitorSection == null || monitorSection.getBoolean("off-hand", true);
        this.monitorHead = monitorSection == null || monitorSection.getBoolean("head", true);
        this.monitorChest = monitorSection == null || monitorSection.getBoolean("chest", true);
        this.monitorLegs = monitorSection == null || monitorSection.getBoolean("legs", true);
        this.monitorFeet = monitorSection == null || monitorSection.getBoolean("feet", true);

        this.thresholds.clear();
        List<Map<?, ?>> raw = cfg.getMapList("thresholds");
        if (raw != null) {
            for (Map<?, ?> m : raw) {
                int percent = getInt(m, "percent", 10);
                String message = getString(m, "message", "warn.low");
                Sound sound = null;
                String soundName = getString(m, "sound", null);
                if (soundName != null) {
                    try {
                        sound = Sound.valueOf(soundName);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                float vol = (float) getDouble(m, "sound-volume", 1.0);
                float pitch = (float) getDouble(m, "sound-pitch", 1.0);
                this.thresholds.add(new Threshold(percent, message, sound, vol, pitch));
            }
        }
        // Sort ascending by percent
        this.thresholds.sort(Comparator.comparingInt(t -> t.percent));

        this.checkIntervalTicks = Math.max(1, cfg.getInt("check-interval-ticks", 20));
    }

    private static int getInt(Map<?, ?> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static double getDouble(Map<?, ?> map, String key, double def) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static String getString(Map<?, ?> map, String key, String def) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : def;
    }

    public String getLanguage() {
        return language;
    }

    public List<Threshold> getThresholds() {
        return thresholds;
    }

    public int getCheckIntervalTicks() {
        return checkIntervalTicks;
    }

    public boolean isMonitorMainHand() {
        return monitorMainHand;
    }

    public boolean isMonitorOffHand() {
        return monitorOffHand;
    }

    public boolean isMonitorHead() {
        return monitorHead;
    }

    public boolean isMonitorChest() {
        return monitorChest;
    }

    public boolean isMonitorLegs() {
        return monitorLegs;
    }

    public boolean isMonitorFeet() {
        return monitorFeet;
    }

    public boolean isGamemodeEnabled(GameMode gm) {
        return enabledGamemodes.contains(gm);
    }
}

