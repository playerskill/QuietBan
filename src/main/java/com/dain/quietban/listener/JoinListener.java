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
 * Проверяет игрока при входе в лобби. Запрос к базе выполняется асинхронно,
 * чтобы не задерживать главный поток. Если игрок забанен — соединение
 * разрывается своим методом (kickPlayer): причина показывается только ему
 * на экране, в чат ничего не выводится.
 */
public class JoinListener implements Listener {

    private final QuietBanPlugin plugin;

    public JoinListener(QuietBanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

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
                    player.kickPlayer(kickReason); // тихий разрыв соединения, без чата
                }
            });
        });
    }
}
