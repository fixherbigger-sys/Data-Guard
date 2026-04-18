package dev.dataguard.listeners;

import dev.dataguard.DataGuard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final DataGuard plugin;

    public PlayerListener(DataGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Run async so it doesn't delay the join
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.getSnapshotManager().isPlayerAutoSaveEnabled(event.getPlayer().getUniqueId())) return;
            String snap = plugin.getSnapshotManager().createPlayerSnapshot(event.getPlayer(), "now");
            if (snap != null) {
                plugin.getLogger().info("[DataGuard] Join snapshot saved for " + event.getPlayer().getName() + ": " + snap);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save on quit too — this is important!
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.getSnapshotManager().isPlayerAutoSaveEnabled(event.getPlayer().getUniqueId())) return;
            plugin.getSnapshotManager().createPlayerSnapshot(event.getPlayer(), "now");
        });
    }
}
