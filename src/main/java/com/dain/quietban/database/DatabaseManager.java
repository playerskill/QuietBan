package com.dain.quietban.database;

import com.dain.quietban.QuietBanPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Подключение к MySQL и запросы к настраиваемой таблице бан-листа. */
public class DatabaseManager {

    private static final String[] COLUMN_KEYS = {"id", "object", "type", "reason", "initiator", "initrn", "expiry", "time"};
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]{1,64}$");

    private final QuietBanPlugin plugin;
    private final Map<String, String> columns = new HashMap<>();

    private String host = "localhost";
    private int port = 3306;
    private String database = "bans";
    private String user = "root";
    private String password = "";
    private String table = "banlist";
    private String matchBy = "NAME";
    private String typeFilter = "";
    private boolean checkExpiry = true;
    private boolean failOpen = true;
    private String dateFormat = "dd.MM.yyyy HH:mm";

    public DatabaseManager(QuietBanPlugin plugin) {
        this.plugin = plugin;
    }

    /** Читает настройки базы данных из config.yml (вызывается при старте и /quietban reload). */
    public void load() {
        FileConfiguration config = plugin.getConfig();
        this.host = config.getString("database.host", "localhost");
        this.port = config.getInt("database.port", 3306);
        this.database = config.getString("database.name", "bans");
        this.user = config.getString("database.user", "root");
        this.password = config.getString("database.password", "");
        this.matchBy = config.getString("database.match-by", "NAME");
        this.typeFilter = config.getString("database.type-filter", "");
        this.checkExpiry = config.getBoolean("database.check-expiry", true);
        this.failOpen = config.getBoolean("database.fail-open", true);
        this.dateFormat = config.getString("settings.date-format", "dd.MM.yyyy HH:mm");
        this.table = sanitize(config.getString("database.table", "banlist"), "banlist");
        for (String key : COLUMN_KEYS) {
            this.columns.put(key, sanitize(config.getString("database.columns." + key, key), key));
        }
    }

    /** Имена столбцов и таблицы приходят из конфига — жёстко валидируем, чтобы исключить SQL-инъекцию. */
    private String sanitize(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String cleaned = raw.replace("`", "").trim();
        return SAFE_IDENTIFIER.matcher(cleaned).matches() ? cleaned : fallback;
    }

    private Connection open() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8"
                + "&connectTimeout=10000&socketTimeout=15000";
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Ищет актуальную запись бана для игрока. Возвращает null, если игрока нет
     * в бан-листе или его бан уже истёк (при check-expiry: true).
     */
    public BanRecord findBan(String playerName, UUID uuid) throws SQLException {
        boolean byUuid = "UUID".equalsIgnoreCase(matchBy);
        if (byUuid && uuid == null) {
            return null;
        }
        String objectValue = byUuid ? uuid.toString() : playerName;

        String idColumn = columns.get("id");
        String typeColumn = columns.get("type");

        StringBuilder sql = new StringBuilder("SELECT `").append(idColumn).append("`");
        for (String key : COLUMN_KEYS) {
            if (!"id".equals(key)) {
                sql.append(", `").append(columns.get(key)).append("`");
            }
        }
        sql.append(" FROM `").append(table)
                .append("` WHERE `").append(columns.get("object")).append("` = ?");
        boolean filterType = typeFilter != null && !typeFilter.isEmpty();
        if (filterType) {
            sql.append(" AND `").append(typeColumn).append("` = ?");
        }
        sql.append(" ORDER BY `").append(idColumn).append("` DESC LIMIT 1");

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = open();
            statement = connection.prepareStatement(sql.toString());
            statement.setString(1, objectValue);
            if (filterType) {
                statement.setString(2, typeFilter);
            }
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return null;
            }

            BanRecord record = new BanRecord(
                    resultSet.getString(columns.get("type")),
                    resultSet.getString(columns.get("reason")),
                    resultSet.getString(columns.get("initiator")),
                    resultSet.getString(columns.get("initrn")),
                    resultSet.getString(columns.get("expiry")),
                    resultSet.getString(columns.get("time")));

            if (checkExpiry && record.getExpiryMillis() > 0L
                    && record.getExpiryMillis() <= System.currentTimeMillis()) {
                return null; // бан истёк
            }
            return record;
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public String getDateFormat() {
        return dateFormat;
    }
}
