package de.t0g3pii.toolguardian.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LocaleManager {

    private final Plugin plugin;
    private FileConfiguration messages;

    public LocaleManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void reload(String language) {
        String path = "lang" + File.separator + language + ".yml";
        File out = new File(plugin.getDataFolder(), path);
        if (!out.exists()) {
            plugin.saveResource(path.replace(File.separatorChar, '/'), false);
        }
        this.messages = new YamlConfiguration();
        try {
            this.messages.load(out);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to load locale file: " + out.getName() + " -> " + e.getMessage());
            // Fallback to embedded
            try {
                File fallback = new File(plugin.getDataFolder(), "lang" + File.separator + "en.yml");
                if (!fallback.exists()) {
                    plugin.saveResource("lang/en.yml", false);
                }
                this.messages = YamlConfiguration.loadConfiguration(fallback);
            } catch (Exception ex) {
                this.messages = new YamlConfiguration();
            }
        }
    }

    public String raw(String key, String def) {
        if (messages == null) return def;
        String val = messages.getString(key);
        return val != null ? val : def;
    }

    public Component mm(String key, Map<String, String> placeholders, String def) {
        String template = raw(key, def != null ? def : key);
        MiniMessage mm = MiniMessage.miniMessage();
        if (placeholders == null || placeholders.isEmpty()) {
            return mm.deserialize(template);
        }
        // Unterstütze Nachrichten mit {platzhaltern} indem wir sie auf <platzhalter> mappen
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            String curly = "{" + e.getKey() + "}";
            String angled = "<" + e.getKey() + ">";
            template = template.replace(curly, angled);
        }
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(e -> Placeholder.unparsed(e.getKey(), e.getValue()))
                .toArray(TagResolver[]::new);
        return mm.deserialize(template, resolvers);
    }
}

