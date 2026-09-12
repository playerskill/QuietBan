package com.dain.quietban.command;

import com.dain.quietban.QuietBanPlugin;
import com.dain.quietban.database.BanRecord;
import com.dain.quietban.util.BanFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.logging.Level;

/** /quietban reload — перезагрузка конфигов; /quietban check <ник> — проверка бан-листа. */
public class QuietBanCommand implements CommandExecutor {

    private final QuietBanPlugin plugin;

    public QuietBanCommand(QuietBanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("quietban.admin")) {
            sender.sendMessage(msg("no-permission", "&cУ вас недостаточно прав."));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(msg("reload-success", "&aКонфигурация перезагружена."));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            handleCheck(sender, args[1]);
            return true;
        }

        sender.sendMessage(msg("command-usage", "&7Использование: &f/quietban <reload|check <ник>>"));
        return true;
    }

    private void handleCheck(final CommandSender sender, final String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BanRecord record = null;
            boolean error = false;
            try {
                record = plugin.getDatabaseManager().findBan(name, null);
            } catch (SQLException ex) {
                error = true;
                plugin.getLogger().log(Level.SEVERE, "Ошибка базы данных при /quietban check " + name, ex);
            }

            final boolean failed = error;
            final BanRecord found = record;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (failed) {
                    sender.sendMessage(msg("check-db-error", "&cОшибка базы данных. Подробности в консоли."));
                } else if (found == null) {
                    sender.sendMessage(msg("check-not-found", "&aИгрок &f{player} &aне найден в бан-листе.")
                            .replace("{player}", name));
                } else {
                    for (String line : BanFormatter.buildChatLines(plugin, "check-found", found, name)) {
                        sender.sendMessage(line);
                    }
                }
            });
        });
    }

    private String msg(String path, String def) {
        String prefix = plugin.getConfigManager().getMessages().getString("prefix", "");
        String text = plugin.getConfigManager().getMessages().getString(path, def);
        return BanFormatter.color(prefix + text);
    }
}
