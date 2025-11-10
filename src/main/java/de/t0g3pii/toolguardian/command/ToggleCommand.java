package de.t0g3pii.toolguardian.command;

import de.t0g3pii.toolguardian.config.LocaleManager;
import de.t0g3pii.toolguardian.storage.ToggleStorage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToggleCommand implements CommandExecutor {

    private final Plugin plugin;
    private final ToggleStorage storage;
    private final LocaleManager locale;

    public ToggleCommand(Plugin plugin, ToggleStorage storage, LocaleManager locale) {
        this.plugin = plugin;
        this.storage = storage;
        this.locale = locale;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("toolguardian.toggle")) {
            player.sendMessage(locale.mm("toggle.no-permission", Collections.emptyMap(), "<red>No permission.</red>"));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            UUID uuid = player.getUniqueId();
            boolean enabled = storage.isEnabled(uuid);
            boolean newState = !enabled;
            storage.setEnabled(uuid, newState);
            storage.saveAsync();
            Component msg = locale.mm(newState ? "toggle.now-enabled" : "toggle.now-disabled", Collections.emptyMap(), newState ? "<green>Enabled</green>" : "<red>Disabled</red>");
            player.sendMessage(msg);
            return true;
        }
        player.sendMessage(locale.mm("toggle.usage", Collections.emptyMap(), "<gray>Usage: /toolguardian toggle</gray>"));
        return true;
    }
}

