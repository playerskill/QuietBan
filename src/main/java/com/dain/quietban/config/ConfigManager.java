package com.dain.quietban.config;

import com.dain.quietban.QuietBanPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Загружает config.yml и messages.yml (с генерацией дефолтов при первом запуске). */
public class ConfigManager {

    private final QuietBanPlugin plugin;
    private FileConfiguration messages;

    public ConfigManager(QuietBanPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        try {
            this.messages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        } catch (FileNotFoundException ex) {
            this.messages = new YamlConfiguration();
        }
        // Дефолты из ресурсов — чтобы getString не возвращал null, если ключ удалили
        InputStream defaults = plugin.getResource("messages.yml");
        if (defaults != null) {
            this.messages.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}
