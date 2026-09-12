package com.dain.quietban.util;

import com.dain.quietban.QuietBanPlugin;
import com.dain.quietban.database.BanRecord;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Собирает экран кика и строки для /quietban check из шаблонов messages.yml. */
public final class BanFormatter {

    private BanFormatter() {
    }

    /** Экран отключения (причина видна только игроку, в чат не пишется). */
    public static String buildKickScreen(QuietBanPlugin plugin, BanRecord record, String playerName) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();
        List<String> lines = messages.getStringList("kick-screen");
        if (lines == null || lines.isEmpty()) {
            lines = Collections.singletonList("&cВы заблокированы на этом сервере: {reason}");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(color(applyPlaceholders(lines.get(i), plugin, record, playerName)));
        }
        return builder.toString();
    }

    /** Строки для ответа в чат администратору (только ему — не глобальный чат). */
    public static List<String> buildChatLines(QuietBanPlugin plugin, String path, BanRecord record, String playerName) {
        List<String> lines = plugin.getConfigManager().getMessages().getStringList(path);
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>(lines.size());
        for (String line : lines) {
            result.add(color(applyPlaceholders(line, plugin, record, playerName)));
        }
        return result;
    }

    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String applyPlaceholders(String line, QuietBanPlugin plugin, BanRecord record, String playerName) {
        return line.replace("{player}", playerName == null ? "" : playerName)
                .replace("{reason}", reasonText(plugin, record))
                .replace("{type}", empty(record.getType(), "—"))
                .replace("{initiator}", empty(record.getInitiator(),
                        plugin.getConfigManager().getMessages().getString("kick-no-initiator", "&7—")))
                .replace("{initrn}", empty(record.getInitrn(), "—"))
                .replace("{time}", timeText(plugin, record))
                .replace("{expiry}", expiryText(plugin, record));
    }

    private static String reasonText(QuietBanPlugin plugin, BanRecord record) {
        String reason = record.getReason();
        if (reason == null || reason.isEmpty()) {
            return color(plugin.getConfigManager().getMessages().getString("kick-no-reason", "&7Причина не указана"));
        }
        return reason;
    }

    private static String expiryText(QuietBanPlugin plugin, BanRecord record) {
        if (record.getExpiryMillis() > 0L) {
            return formatDate(plugin, record.getExpiryMillis());
        }
        String raw = record.getRawExpiry();
        if (!raw.isEmpty() && !isNumeric(raw)) {
            return raw; // в базе лежит текстовая дата — показываем как есть
        }
        return color(plugin.getConfigManager().getMessages().getString("ban-forever", "&cНавсегда"));
    }

    private static String timeText(QuietBanPlugin plugin, BanRecord record) {
        if (record.getTimeMillis() > 0L) {
            return formatDate(plugin, record.getTimeMillis());
        }
        String raw = record.getRawTime();
        return raw.isEmpty() || isNumeric(raw) ? "" : raw;
    }

    private static String formatDate(QuietBanPlugin plugin, long millis) {
        try {
            return new SimpleDateFormat(plugin.getDatabaseManager().getDateFormat()).format(new java.util.Date(millis));
        } catch (Exception ex) {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(millis));
        }
    }

    private static boolean isNumeric(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String empty(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
