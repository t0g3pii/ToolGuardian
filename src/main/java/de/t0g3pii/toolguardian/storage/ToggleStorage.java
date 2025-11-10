package de.t0g3pii.toolguardian.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToggleStorage {

    private final Plugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Boolean> toggles = new HashMap<>();

    public ToggleStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "toggles.yml");
        this.config = new YamlConfiguration();
    }

    public void load() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create toggles.yml: " + e.getMessage());
            }
        }
        try {
            this.config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to load toggles.yml: " + e.getMessage());
        }
        this.toggles.clear();
        if (config.isConfigurationSection("players")) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    boolean enabled = config.getBoolean("players." + key, true);
                    toggles.put(uuid, enabled);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, Boolean> e : toggles.entrySet()) {
            config.set("players." + e.getKey(), e.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save toggles.yml: " + e.getMessage());
        }
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public boolean isEnabled(UUID uuid) {
        return toggles.getOrDefault(uuid, true);
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        toggles.put(uuid, enabled);
    }
}

