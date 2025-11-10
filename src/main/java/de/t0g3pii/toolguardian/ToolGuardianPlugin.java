package de.t0g3pii.toolguardian;

import de.t0g3pii.toolguardian.command.ToggleCommand;
import de.t0g3pii.toolguardian.config.ConfigManager;
import de.t0g3pii.toolguardian.config.LocaleManager;
import de.t0g3pii.toolguardian.monitor.DurabilityMonitor;
import de.t0g3pii.toolguardian.storage.ToggleStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ToolGuardianPlugin extends JavaPlugin {

    private static ToolGuardianPlugin instance;

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private ToggleStorage toggleStorage;
    private DurabilityMonitor durabilityMonitor;

    public static ToolGuardianPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.localeManager = new LocaleManager(this);
        this.toggleStorage = new ToggleStorage(this);

        this.configManager.reload();
        this.localeManager.reload(configManager.getLanguage());
        this.toggleStorage.load();

        getCommand("toolguardian").setExecutor(new ToggleCommand(this, toggleStorage, localeManager));

        this.durabilityMonitor = new DurabilityMonitor(this, configManager, localeManager, toggleStorage);
        this.durabilityMonitor.start();

        getLogger().info("ToolGuardian enabled.");
    }

    @Override
    public void onDisable() {
        if (durabilityMonitor != null) {
            durabilityMonitor.stop();
        }
        if (toggleStorage != null) {
            toggleStorage.save();
        }
        getLogger().info("ToolGuardian disabled.");
    }
}

