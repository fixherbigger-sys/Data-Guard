package dev.dataguard.managers;

import dev.dataguard.DataGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class AutoSaveManager {

    private final DataGuard plugin;
    private BukkitTask task;
    private boolean serverAutoSaveEnabled;

    public AutoSaveManager(DataGuard plugin) {
        this.plugin = plugin;
        this.serverAutoSaveEnabled = plugin.getConfig().getBoolean("server-auto-save", true);
    }

    public void start() {
        int intervalMinutes = plugin.getConfig().getInt("auto-save-interval", 30);
        long intervalTicks = intervalMinutes * 60L * 20L; // convert minutes → ticks

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!serverAutoSaveEnabled) return;

            int playerCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!plugin.getSnapshotManager().isPlayerAutoSaveEnabled(player.getUniqueId())) continue;
                String snap = plugin.getSnapshotManager().createPlayerSnapshot(player, "now");
                if (snap != null) playerCount++;
            }

            // Also create a server-wide snapshot
            String serverSnap = plugin.getSnapshotManager().createServerSnapshot("now");
            final int savedCount = playerCount;
            final String snapName = serverSnap;

            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[AutoSave] Saved " + savedCount + " player(s). Server snapshot: " + snapName);
            });

        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void restart() {
        stop();
        start();
    }

    public boolean isServerAutoSaveEnabled() {
        return serverAutoSaveEnabled;
    }

    public void setServerAutoSaveEnabled(boolean enabled) {
        this.serverAutoSaveEnabled = enabled;
    }
}
