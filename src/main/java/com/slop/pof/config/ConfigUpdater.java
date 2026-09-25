package com.slop.pof.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.util.logging.Level;

/**
 * Adds keys that newer jars introduced. Existing values are left alone.
 * {@code config-version} is how the plugin knows an update is needed. Do not change it by hand.
 */
public final class ConfigUpdater {
    public static final int CURRENT = 2;

    private ConfigUpdater() {
    }

    /** @return how many keys were added, or 0 when the file is already current */
    public static int update(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        int found = config.getInt("config-version", 0);
        if (config.contains("config-version") && found >= CURRENT) {
            return 0;
        }
        int added = 0;
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(in);
                added = merge(defaults, config, "");
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Could not read the default config.yml", ex);
        }
        config.set("config-version", CURRENT);
        plugin.saveConfig();
        plugin.getLogger().info("Updated config.yml from version " + found + " to " + CURRENT
                + " (" + added + " new keys). Your existing values were kept. Do not change config-version.");
        return added;
    }

    private static int merge(ConfigurationSection defaults, FileConfiguration config, String prefix) {
        int added = 0;
        for (String key : defaults.getKeys(false)) {
            if ("config-version".equals(key) && prefix.isEmpty()) {
                continue;
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            ConfigurationSection child = defaults.getConfigurationSection(key);
            if (child != null) {
                if (!config.isConfigurationSection(path)) {
                    config.set(path, defaults.get(key));
                    added++;
                } else {
                    added += merge(child, config, path);
                }
            } else if (!config.contains(path)) {
                config.set(path, defaults.get(key));
                added++;
            }
        }
        return added;
    }
}
