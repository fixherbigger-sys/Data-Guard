package dev.dataguard.commands;

import dev.dataguard.DataGuard;
import dev.dataguard.managers.SnapshotManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class DataCommand implements CommandExecutor, TabCompleter {

    private final DataGuard plugin;

    public DataCommand(DataGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ─── /data reload ───
            case "reload" -> {
                if (!hasAdmin(sender)) return true;
                plugin.reloadConfig();
                plugin.getAutoSaveManager().restart();
                sender.sendMessage(plugin.getPrefix() + plugin.getMessage("config-reloaded"));
            }

            // ─── /data server ... ───
            case "server" -> handleServer(sender, args);

            // ─── /data player ... ───
            case "player" -> handlePlayer(sender, args);

            // ─── /data automatic [true/false] ───
            case "automatic" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + "&cOnly players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getPrefix() + "&cUsage: /data automatic <true|false>");
                    return true;
                }
                boolean val = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("truth");
                plugin.getSnapshotManager().setPlayerAutoSave(player.getUniqueId(), val);
                sender.sendMessage(plugin.getPrefix() + (val
                        ? plugin.getMessage("auto-save-enabled")
                        : plugin.getMessage("auto-save-disabled")));
            }

            // ─── /data load [player|server] [time] ───
            case "load" -> {
                if (!hasAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(plugin.getPrefix() + "&cUsage: /data load <player <name> <snapshot>|server <snapshot>>");
                    return true;
                }
                handleLoad(sender, args);
            }

            // ─── /data list [player <name>|server] ───
            case "list" -> handleList(sender, args);

            default -> sendHelp(sender);
        }

        return true;
    }

    // ─────────────────────────────────────────────
    //  /data server <create|delete|automatic> [label]
    // ─────────────────────────────────────────────
    private void handleServer(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) return;
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "&cUsage: /data server <create|delete|automatic> [label]");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> {
                String label = args.length >= 3 ? args[2] : "now";
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String snap = plugin.getSnapshotManager().createServerSnapshot(label);
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getPrefix() + plugin.getMessage("snapshot-created", "{snapshot}", snap)));
                });
            }
            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getPrefix() + "&cUsage: /data server delete <label|now>");
                    return;
                }
                String label = args[2];
                boolean deleted = plugin.getSnapshotManager().deleteServerSnapshot(label);
                sender.sendMessage(plugin.getPrefix() + (deleted
                        ? plugin.getMessage("snapshot-deleted", "{snapshot}", label)
                        : plugin.getMessage("snapshot-not-found", "{snapshot}", label)));
            }
            case "automatic" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getPrefix() + "&cUsage: /data server automatic <true|false>");
                    return;
                }
                boolean val = args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("truth");
                plugin.getAutoSaveManager().setServerAutoSaveEnabled(val);
                sender.sendMessage(plugin.getPrefix() + (val
                        ? plugin.getMessage("auto-save-enabled")
                        : plugin.getMessage("auto-save-disabled")));
            }
            default -> sender.sendMessage(plugin.getPrefix() + "&cUnknown server subcommand.");
        }
    }

    // ─────────────────────────────────────────────
    //  /data player <create|delete> [label]
    // ─────────────────────────────────────────────
    private void handlePlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "&cUsage: /data player <create|delete> [label]");
            return;
        }

        boolean isAdmin = hasAdmin(sender);

        switch (args[1].toLowerCase()) {
            case "create" -> {
                // Members can create their own snapshot; admins can target others
                Player target = getTargetPlayer(sender, args, 2, isAdmin);
                if (target == null) return;
                String label = args.length >= (isAdmin && !(sender instanceof Player p && p.equals(target)) ? 4 : 3) ? args[args.length - 1] : "now";
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String snap = plugin.getSnapshotManager().createPlayerSnapshot(target, label);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (snap != null) {
                            sender.sendMessage(plugin.getPrefix() + plugin.getMessage("snapshot-created", "{snapshot}", snap));
                            if (!sender.equals(target)) {
                                target.sendMessage(plugin.getPrefix() + "&aA snapshot of your data was created by an admin.");
                            }
                        }
                    });
                });
            }
            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getPrefix() + "&cUsage: /data player delete <label|now>");
                    return;
                }
                // Members can only delete their own; admins can target others
                Player target;
                String label;
                if (isAdmin && args.length >= 4) {
                    // /data player delete <playername> <label>
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(plugin.getPrefix() + plugin.getMessage("player-not-found", "{player}", args[2]));
                        return;
                    }
                    label = args[3];
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getPrefix() + "&cConsole must specify a player: /data player delete <player> <label>");
                        return;
                    }
                    target = (Player) sender;
                    label = args[2];
                }
                boolean deleted = plugin.getSnapshotManager().deletePlayerSnapshot(target, label);
                sender.sendMessage(plugin.getPrefix() + (deleted
                        ? plugin.getMessage("snapshot-deleted", "{snapshot}", label)
                        : plugin.getMessage("snapshot-not-found", "{snapshot}", label)));
            }
            default -> sender.sendMessage(plugin.getPrefix() + "&cUnknown player subcommand.");
        }
    }

    // ─────────────────────────────────────────────
    //  /data load <player <name> <snap> | server <snap>>
    // ─────────────────────────────────────────────
    private void handleLoad(CommandSender sender, String[] args) {
        // args[0]=load, args[1]=player|server
        if (args[1].equalsIgnoreCase("player")) {
            // /data load player <name> <snapshot>
            if (args.length < 4) {
                sender.sendMessage(plugin.getPrefix() + "&cUsage: /data load player <playerName> <snapshot>");
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getPrefix() + plugin.getMessage("player-not-found", "{player}", args[2]));
                return;
            }
            String snap = args[3];
            boolean loaded = plugin.getSnapshotManager().loadPlayerSnapshot(target, snap);
            sender.sendMessage(plugin.getPrefix() + (loaded
                    ? plugin.getMessage("snapshot-loaded", "{snapshot}", snap)
                    : plugin.getMessage("snapshot-not-found", "{snapshot}", snap)));
            if (loaded) {
                target.sendMessage(plugin.getPrefix() + "&eYour data has been restored to snapshot: &b" + snap);
            }

        } else if (args[1].equalsIgnoreCase("server")) {
            // /data load server <snapshot>
            if (args.length < 3) {
                sender.sendMessage(plugin.getPrefix() + "&cUsage: /data load server <snapshot>");
                return;
            }
            String snap = args[2];
            int restored = plugin.getSnapshotManager().loadServerSnapshot(snap);
            if (restored < 0) {
                sender.sendMessage(plugin.getPrefix() + plugin.getMessage("snapshot-not-found", "{snapshot}", snap));
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getMessage("snapshot-loaded", "{snapshot}", snap)
                        + " &7(&e" + restored + " players restored&7)");
                Bukkit.getOnlinePlayers().forEach(p ->
                        p.sendMessage(plugin.getPrefix() + "&eServer data has been restored to snapshot: &b" + snap));
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + "&cUsage: /data load <player|server> ...");
        }
    }

    // ─────────────────────────────────────────────
    //  /data list [player <name> | server]
    // ─────────────────────────────────────────────
    private void handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "&cUsage: /data list <player [name]|server>");
            return;
        }

        if (args[1].equalsIgnoreCase("server")) {
            if (!hasAdmin(sender)) return;
            List<String> snaps = plugin.getSnapshotManager().listServerSnapshots();
            if (snaps.isEmpty()) {
                sender.sendMessage(plugin.getPrefix() + plugin.getMessage("no-snapshots"));
                return;
            }
            sender.sendMessage(plugin.getPrefix() + plugin.getMessage("snapshot-list-header", "{target}", "Server"));
            snaps.forEach(s -> sender.sendMessage("  &7- &e" + s));

        } else if (args[1].equalsIgnoreCase("player")) {
            UUID uuid;
            String displayName;
            if (args.length >= 3 && hasAdmin(sender)) {
                Player t = Bukkit.getPlayer(args[2]);
                if (t == null) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getMessage("player-not-found", "{player}", args[2]));
                    return;
                }
                uuid = t.getUniqueId();
                displayName = t.getName();
            } else if (sender instanceof Player p) {
                uuid = p.getUniqueId();
                displayName = p.getName();
            } else {
                sender.sendMessage(plugin.getPrefix() + "&cConsole must specify a player name.");
                return;
            }
            List<String> snaps = plugin.getSnapshotManager().listPlayerSnapshots(uuid);
            if (snaps.isEmpty()) {
                sender.sendMessage(plugin.getPrefix() + plugin.getMessage("no-snapshots"));
                return;
            }
            sender.sendMessage(plugin.getPrefix() + plugin.getMessage("snapshot-list-header", "{target}", displayName));
            snaps.forEach(s -> sender.sendMessage("  &7- &e" + DataGuard.colorize(s)));
        }
    }

    // ─────────────────────────────────────────────
    //  HELP
    // ─────────────────────────────────────────────
    private void sendHelp(CommandSender sender) {
        boolean isAdmin = hasAdmin(sender);
        sender.sendMessage(DataGuard.colorize("&8&m----&r &bDataGuard Help &8&m----"));
        sender.sendMessage(DataGuard.colorize("&e/data player create [label] &7- Save your data"));
        sender.sendMessage(DataGuard.colorize("&e/data player delete <label> &7- Delete your snapshot"));
        sender.sendMessage(DataGuard.colorize("&e/data automatic <true|false> &7- Toggle your auto-save"));
        sender.sendMessage(DataGuard.colorize("&e/data list player &7- List your snapshots"));
        if (isAdmin) {
            sender.sendMessage(DataGuard.colorize("&c--- Admin ---"));
            sender.sendMessage(DataGuard.colorize("&e/data server create [label] &7- Server-wide snapshot"));
            sender.sendMessage(DataGuard.colorize("&e/data server delete <label> &7- Delete server snapshot"));
            sender.sendMessage(DataGuard.colorize("&e/data server automatic <true|false> &7- Toggle server auto-save"));
            sender.sendMessage(DataGuard.colorize("&e/data load player <name> <snapshot> &7- Restore player data"));
            sender.sendMessage(DataGuard.colorize("&e/data load server <snapshot> &7- Restore server data"));
            sender.sendMessage(DataGuard.colorize("&e/data list server &7- List server snapshots"));
            sender.sendMessage(DataGuard.colorize("&e/data reload &7- Reload config"));
        }
    }

    // ─────────────────────────────────────────────
    //  TAB COMPLETION
    // ─────────────────────────────────────────────
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        boolean isAdmin = hasAdmin(sender);

        if (args.length == 1) {
            completions.addAll(Arrays.asList("player", "automatic", "list"));
            if (isAdmin) completions.addAll(Arrays.asList("server", "load", "reload"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "player" -> completions.addAll(Arrays.asList("create", "delete"));
                case "server" -> { if (isAdmin) completions.addAll(Arrays.asList("create", "delete", "automatic")); }
                case "load" -> { if (isAdmin) completions.addAll(Arrays.asList("player", "server")); }
                case "automatic" -> completions.addAll(Arrays.asList("true", "false"));
                case "list" -> {
                    completions.add("player");
                    if (isAdmin) completions.add("server");
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "player" -> {
                    if (args[1].equalsIgnoreCase("create")) completions.add("now");
                    if (args[1].equalsIgnoreCase("delete") && sender instanceof Player p) {
                        completions.addAll(plugin.getSnapshotManager().listPlayerSnapshots(p.getUniqueId()));
                    }
                }
                case "server" -> {
                    if (isAdmin) {
                        if (args[1].equalsIgnoreCase("create")) completions.add("now");
                        if (args[1].equalsIgnoreCase("delete"))
                            completions.addAll(plugin.getSnapshotManager().listServerSnapshots());
                        if (args[1].equalsIgnoreCase("automatic"))
                            completions.addAll(Arrays.asList("true", "false"));
                    }
                }
                case "load" -> {
                    if (isAdmin) {
                        if (args[1].equalsIgnoreCase("player"))
                            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        if (args[1].equalsIgnoreCase("server"))
                            completions.addAll(plugin.getSnapshotManager().listServerSnapshots());
                    }
                }
            }
        } else if (args.length == 4 && isAdmin && args[0].equalsIgnoreCase("load") && args[1].equalsIgnoreCase("player")) {
            Player t = Bukkit.getPlayer(args[2]);
            if (t != null) completions.addAll(plugin.getSnapshotManager().listPlayerSnapshots(t.getUniqueId()));
        }

        String partial = args[args.length - 1].toLowerCase();
        return completions.stream().filter(c -> c.toLowerCase().startsWith(partial)).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────

    private boolean hasAdmin(CommandSender sender) {
        if (sender.hasPermission("dataguard.admin")) return true;
        sender.sendMessage(plugin.getPrefix() + plugin.getMessage("no-permission"));
        return false;
    }

    /** Returns null and sends error if target can't be resolved. */
    private Player getTargetPlayer(CommandSender sender, String[] args, int nameIndex, boolean isAdmin) {
        if (isAdmin && args.length > nameIndex + 1) {
            // Admin specifying another player: /data player create <name> [label]
            Player t = Bukkit.getPlayer(args[nameIndex]);
            if (t != null) return t;
        }
        if (sender instanceof Player p) return p;
        sender.sendMessage(plugin.getPrefix() + "&cConsole must specify a player name.");
        return null;
    }
}
