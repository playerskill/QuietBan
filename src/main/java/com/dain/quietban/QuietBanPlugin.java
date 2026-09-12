package com.dain.quietban;

import com.dain.quietban.command.QuietBanCommand;
import com.dain.quietban.config.ConfigManager;
import com.dain.quietban.database.DatabaseManager;
import com.dain.quietban.listener.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * QuietBan — при входе игрока в лобби проверяет бан-лист в базе данных
 * и тихо разрывает соединение с забаненным игроком, показав причину
 * только на его экране. В чат ничего не выводится.
 */
public final class QuietBanPlugin extends JavaPlugin {

    // Статический экземпляр для доступа из других классов (например, JoinListener)
    private static QuietBanPlugin instance;

    // Поля для настроек TPS
    private boolean tpsProtectionEnabled;
    private double minTpsThreshold;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // Инициализируем статический экземпляр
        instance = this;

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.load();
        ensureDriverLoaded();

        // Загружаем настройки TPS из конфига
        loadTpsSettings();

        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);

        PluginCommand command = getCommand("quietban");
        if (command != null) {
            command.setExecutor(new QuietBanCommand(this));
        } else {
            getLogger().severe("Команда quietban не объявлена в plugin.yml!");
        }

        getLogger().info("QuietBan запущен: тихая проверка банов при входе в лобби включена.");
        getLogger().info("TPS Protection: Enabled=" + tpsProtectionEnabled + ", Threshold=" + minTpsThreshold);
    }

    /**
     * Загружает параметры защиты по TPS из config.yml.
     * Использует безопасные значения по умолчанию, если параметры отсутствуют.
     */
    private void loadTpsSettings() {
        FileConfiguration config = getConfig();
        this.tpsProtectionEnabled = config.getBoolean("tps-protection.enabled", true);
        this.minTpsThreshold = config.getDouble("tps-protection.min-tps-threshold", 18.0);
    }

    /**
     * Проверяет, находится ли сервер в стабильном состоянии (TPS выше порога).
     * @return true, если сервер стабилен или защита отключена; false, если TPS ниже порога.
     */
    public boolean isServerStable() {
        if (!tpsProtectionEnabled) {
            return true; // Если защита выключена, считаем сервер стабильным
        }

        double currentTps = Bukkit.getServer().getTPS();

        if (currentTps < minTpsThreshold) {
            // Сообщаем в консоль об проблеме с TPS (как требовалось в ТЗ)
            getLogger().warning("[TPS PROTECTION] Critical Lag Detected! Current TPS (" + currentTps + ") is below threshold (" + minTpsThreshold + "). Skipping player checks and DB writes.");
            return false;
        }

        return true;
    }

    /**
     * Геттер для получения экземпляра плагина из других классов.
     */
    public static QuietBanPlugin getInstance() {
        return instance;
    }

    /** Перечитывает config.yml и messages.yml и перезагружает настройки базы данных. */
    public void reloadAll() {
        configManager.loadAll();
        databaseManager.load();
        // При перезагрузке конфига также обновляем настройки TPS
        loadTpsSettings();
        getLogger().info("Настройки и TPS-параметры перезагружены.");
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
