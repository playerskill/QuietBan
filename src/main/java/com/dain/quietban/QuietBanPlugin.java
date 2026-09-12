package com.dain.quietban;

import com.dain.quietban.command.QuietBanCommand;
import com.dain.quietban.config.ConfigManager;
import com.dain.quietban.database.DatabaseManager;
import com.dain.quietban.listener.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * QuietBan — при входе игрока в лобби проверяет бан-лист в базе данных
 * и тихо разрывает соединение с забаненным игроком, показав причину
 * только на его экране. В чат ничего не выводится.
 */
public final class QuietBanPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.load();
        ensureDriverLoaded();

        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);

        PluginCommand command = getCommand("quietban");
        if (command != null) {
            command.setExecutor(new QuietBanCommand(this));
        } else {
            getLogger().severe("Команда quietban не объявлена в plugin.yml!");
        }

        getLogger().info("QuietBan запущен: тихая проверка банов при входе в лобби включена.");
    }

    /** Перечитывает config.yml и messages.yml и перезагружает настройки базы данных. */
    public void reloadAll() {
        configManager.loadAll();
        databaseManager.load();
    }

    /** На 1.12.2 сервер уже содержит драйвер MySQL, но на всякий случай проверяем оба класса. */
    private void ensureDriverLoaded() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            getLogger().warning("MySQL JDBC driver не найден — проверка банов работать не будет!");
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
