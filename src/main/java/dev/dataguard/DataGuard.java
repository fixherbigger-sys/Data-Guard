package dev.dataguard;

import dev.dataguard.commands.DataCommand;
import dev.dataguard.listeners.PlayerListener;
import dev.dataguard.managers.AutoSaveManager;
import dev.dataguard.managers.SnapshotManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DataGuard extends JavaPlugin {

    private static DataGuard instance;
    private SnapshotManager snapshotManager;
    private AutoSaveManager autoSaveManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Init managers
        snapshotManager = new SnapshotManager(this);
        autoSaveManager = new AutoSaveManager(this);

        // Register command
        DataCommand dataCommand = new DataCommand(this);
        getCommand("data").setExecutor(dataCommand);
        getCommand("data").setTabCompleter(dataCommand);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Start auto-save task
        autoSaveManager.start();

        getLogger().info("DataGuard enabled! Protecting your data.");
    }

    @Override
    public void onDisable() {
        if (autoSaveManager != null) {
            autoSaveManager.stop();
        }
        getLogger().info("DataGuard disabled.");
    }

    public static DataGuard getInstance() {
        return instance;
    }

    public SnapshotManager getSnapshotManager() {
        return snapshotManager;
    }

    public AutoSaveManager getAutoSaveManager() {
        return autoSaveManager;
    }

    public String getPrefix() {
        return colorize(getConfig().getString("prefix", "&8[&bDataGuard&8] &r"));
    }

    public String getMessage(String key) {
        String msg = getConfig().getString("messages." + key, "&cMessage not found: " + key);
        return colorize(msg);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public static String colorize(String text) {
        return text.replace("&", "\u00a7");
    }
}
