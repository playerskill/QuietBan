package com.dain.quietban.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Одна запись бан-листа из базы данных. */
public class BanRecord {

    private final String type;
    private final String reason;
    private final String initiator;
    private final String initrn;
    private final String rawExpiry;
    private final String rawTime;
    private final long expiryMillis; // 0 = вечный бан / не определено
    private final long timeMillis;   // 0 = не определено

    public BanRecord(String type, String reason, String initiator, String initrn,
                     String rawExpiry, String rawTime) {
        this.type = type == null ? "" : type;
        this.reason = reason == null ? "" : reason;
        this.initiator = initiator == null ? "" : initiator;
        this.initrn = initrn == null ? "" : initrn;
        this.rawExpiry = rawExpiry == null ? "" : rawExpiry.trim();
        this.rawTime = rawTime == null ? "" : rawTime.trim();
        this.expiryMillis = parseTimestamp(this.rawExpiry);
        this.timeMillis = parseTimestamp(this.rawTime);
    }

    /**
     * Понимает timestamp в секундах или миллисекундах, а также строковые даты
     * вида "YYYY-MM-DD HH:MM:SS". Значение <= 0 или нераспознаваемое = 0 (вечный бан).
     */
    static long parseTimestamp(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0L;
        }
        try {
            long value = Long.parseLong(raw);
            if (value <= 0L) {
                return 0L;
            }
            // секунды (10 знаков покрывают даты до 2286 года) -> миллисекунды
            return value < 100000000000L ? value * 1000L : value;
        } catch (NumberFormatException ignored) {
        }
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setLenient(false);
            try {
                Date parsed = format.parse(raw);
                return parsed.getTime();
            } catch (ParseException ignored) {
            }
        }
        return 0L;
    }

    public String getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public String getInitiator() {
        return initiator;
    }

    public String getInitrn() {
        return initrn;
    }

    public String getRawExpiry() {
        return rawExpiry;
    }

    public String getRawTime() {
        return rawTime;
    }

    public long getExpiryMillis() {
        return expiryMillis;
    }

    public long getTimeMillis() {
        return timeMillis;
    }
}
