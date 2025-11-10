package de.t0g3pii.toolguardian.monitor;

import de.t0g3pii.toolguardian.config.ConfigManager;
import de.t0g3pii.toolguardian.config.ConfigManager.Threshold;
import de.t0g3pii.toolguardian.config.LocaleManager;
import de.t0g3pii.toolguardian.storage.ToggleStorage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DurabilityMonitor {

    private final Plugin plugin;
    private final ConfigManager config;
    private final LocaleManager locale;
    private final ToggleStorage toggleStorage;

    private BukkitTask task;

    // Pro Spieler: pro Slot letzter gemeldeter Threshold-Index und der zuletzt gesehene Materialname zur Reset-Erkennung
    private final Map<UUID, Map<EquipmentSlot, Integer>> lastNotifiedIndex = new HashMap<>();
    private final Map<UUID, Map<EquipmentSlot, String>> lastMaterialPerSlot = new HashMap<>();

    public DurabilityMonitor(Plugin plugin, ConfigManager config, LocaleManager locale, ToggleStorage toggleStorage) {
        this.plugin = plugin;
        this.config = config;
        this.locale = locale;
        this.toggleStorage = toggleStorage;
    }

    public void start() {
        stop();
        int interval = config.getCheckIntervalTicks();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void tick() {
        List<Threshold> thresholds = config.getThresholds();
        if (thresholds.isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!toggleStorage.isEnabled(player.getUniqueId())) continue;
            GameMode gm = player.getGameMode();
            if (!config.isGamemodeEnabled(gm)) continue;

            PlayerInventory inv = player.getInventory();
            checkSlot(player, EquipmentSlot.HAND, inv.getItemInMainHand(), thresholds, config.isMonitorMainHand());
            checkSlot(player, EquipmentSlot.OFF_HAND, inv.getItemInOffHand(), thresholds, config.isMonitorOffHand());
            checkSlot(player, EquipmentSlot.HEAD, inv.getHelmet(), thresholds, config.isMonitorHead());
            checkSlot(player, EquipmentSlot.CHEST, inv.getChestplate(), thresholds, config.isMonitorChest());
            checkSlot(player, EquipmentSlot.LEGS, inv.getLeggings(), thresholds, config.isMonitorLegs());
            checkSlot(player, EquipmentSlot.FEET, inv.getBoots(), thresholds, config.isMonitorFeet());
        }
    }

    private void checkSlot(Player player, EquipmentSlot slot, ItemStack stack, List<Threshold> thresholds, boolean enabled) {
        if (!enabled) {
            resetSlot(player.getUniqueId(), slot);
            return;
        }
        if (stack == null || stack.getType().getMaxDurability() <= 0) {
            resetSlot(player.getUniqueId(), slot);
            return;
        }
        if (!(stack.getItemMeta() instanceof Damageable)) {
            resetSlot(player.getUniqueId(), slot);
            return;
        }
        Damageable dmg = (Damageable) stack.getItemMeta();
        int max = stack.getType().getMaxDurability();
        int remaining = Math.max(0, max - dmg.getDamage());
        if (remaining <= 0) {
            resetSlot(player.getUniqueId(), slot);
            return;
        }
        double percent = (remaining * 100.0) / max;

        // Reset bei Itemwechsel
        String materialKey = stack.getType().name();
        String lastKey = lastMaterialPerSlot
                .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(EquipmentSlot.class))
                .get(slot);
        if (lastKey == null || !lastKey.equals(materialKey)) {
            setLast(player.getUniqueId(), slot, -1, materialKey);
        }

        // Finde die erste Stufe (aufsteigend sortiert), bei der percent <= threshold.percent
        int targetIndex = -1;
        for (int i = 0; i < thresholds.size(); i++) {
            Threshold t = thresholds.get(i);
            if (percent <= t.percent) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex < 0) {
            // Oberhalb aller Schwellenwerte -> Reset, damit bei erneutem Fallen wieder gemeldet wird
            setLast(player.getUniqueId(), slot, -1, materialKey);
            return;
        }

        int lastIdx = lastNotifiedIndex
                .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(EquipmentSlot.class))
                .getOrDefault(slot, -1);

        // Nur benachrichtigen, wenn wir eine "strengere" Stufe als zuvor erreicht haben
        // (mit aufsteigender Sortierung bedeutet kleinerer Index eine strengere Stufe)
        if (lastIdx == -1 || targetIndex < lastIdx) {
            // Neue (strengere) Stufe erreicht -> melden
            Threshold t = thresholds.get(targetIndex);
            Map<String, String> ph = new HashMap<>();
            ph.put("item", humanReadableItemName(stack.getType().name()));
            ph.put("remaining", String.valueOf(remaining));
            ph.put("max", String.valueOf(max));
            ph.put("percent", String.valueOf(Math.max(0, Math.round(percent))));

            Component msg = locale.mm(t.messageKey, ph, t.messageKey);
            player.sendMessage(msg);
            if (t.sound != null) {
                player.playSound(player.getLocation(), t.sound, t.soundVolume, t.soundPitch);
            }

            setLast(player.getUniqueId(), slot, targetIndex, materialKey);
        }
    }

    private void resetSlot(UUID uuid, EquipmentSlot slot) {
        Map<EquipmentSlot, Integer> idx = lastNotifiedIndex.get(uuid);
        if (idx != null) {
            idx.remove(slot);
        }
        Map<EquipmentSlot, String> mat = lastMaterialPerSlot.get(uuid);
        if (mat != null) {
            mat.remove(slot);
        }
    }

    private void setLast(UUID uuid, EquipmentSlot slot, int index, String materialKey) {
        lastNotifiedIndex.computeIfAbsent(uuid, k -> new EnumMap<>(EquipmentSlot.class)).put(slot, index);
        lastMaterialPerSlot.computeIfAbsent(uuid, k -> new EnumMap<>(EquipmentSlot.class)).put(slot, materialKey);
    }

    private String humanReadableItemName(String key) {
        String s = key.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = s.split(" ");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            out.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                out.append(parts[i].substring(1));
            }
            if (i < parts.length - 1) out.append(' ');
        }
        return out.toString();
    }
}

