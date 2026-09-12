
package com.dain.quietban.listener;

import com.dain.quietban.QuietBanPlugin;
import com.dain.quietban.database.BanRecord;
import com.dain.quietban.util.BanFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Проверяет игрока при входе в лобби.
 * Добавлена защита по TPS: если сервер лагает, проверка бана пропускается,
 * чтобы не создавать дополнительную нагрузку и не задерживать вход игроков.
 */
public class JoinListener implements Listener {

    private final QuietBanPlugin plugin;

    public JoinListener(QuietBanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // --- ШАГ 1: ПРОВЕРКА TPS В ГЛАВНОМ ПОТОКЕ ---
        // Мы вызываем метод из главного класса. Если TPS низкий, метод вернет false.
        // В этом случае мы НЕ запускаем асинхронную задачу (runTaskAsynchronously),
        // тем самым полностью отключая воздействие плагина на игроков в момент лагов.
        if (!plugin.isServerStable()) {
            // Игрок просто заходит на сервер. В консоль уже упало предупреждение
            // из метода isServerStable().
            return;
        }
        // --------------------------------------------

        // --- ШАГ 2: ЗАПУСК ПРОВЕРКИ В БД (ТОЛЬКО ЕСЛИ СЕРВЕР СТАБИЛЕН) ---
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BanRecord record = null;
            boolean error = false;
            
            try {
                record = plugin.getDatabaseManager().findBan(player.getName(), player.getUniqueId());
            } catch (SQLException ex) {
                error = true;
                plugin.getLogger().log(Level.SEVERE,
                        "Не удалось выполнить запрос к базе данных для " + player.getName(), ex);
            }

            final boolean failed = error;
            final BanRecord ban = record;

            if (failed) {
                // Логика обработки ошибок БД (fail-open / fail-closed)
                if (!plugin.getDatabaseManager().isFailOpen()) {
                    final String message = BanFormatter.color(plugin.getConfigManager().getMessages()
                            .getString("kick-db-error", "&cНе удалось проверить бан-лист. Попробуйте зайти позже."));
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.kickPlayer(message);
                        }
                    });
                }
                return;
            }

            if (ban == null) {
                return; // не забанен — пусть играет
            }

            final String kickReason = BanFormatter.buildKickScreen(plugin, ban, player.getName());
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kickPlayer(kickReason); // тихий разрыв соединения
                }
            });
        });
    }
}
