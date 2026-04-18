package dev.dataguard.managers;

import dev.dataguard.DataGuard;
import dev.dataguard.util.NBTUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SnapshotManager {

    private final DataGuard plugin;
    private final File snapshotsDir;

    // Per-player auto-save toggle (UUID -> enabled)
    private final Map<UUID, Boolean> playerAutoSave = new HashMap<>();

    public SnapshotManager(DataGuard plugin) {
        this.plugin = plugin;
        this.snapshotsDir = new File(plugin.getDataFolder(), "snapshots");
        snapshotsDir.mkdirs();

        new File(snapshotsDir, "server").mkdirs();
        new File(snapshotsDir, "players").mkdirs();
    }

    // ─────────────────────────────────────────────
    //  TIMESTAMP
    // ─────────────────────────────────────────────

    public String nowTimestamp() {
        String fmt = plugin.getConfig().getString("date-format", "yyyy-MM-dd_HH-mm-ss");
        return new SimpleDateFormat(fmt).format(new Date());
    }

    // ─────────────────────────────────────────────
    //  PLAYER SNAPSHOTS
    // ─────────────────────────────────────────────

    /**
     * Creates a snapshot of the given player's data.
     * @param player the online player
     * @param label  "now" or a custom timestamp/name
     * @return the snapshot folder name, or null on failure
     */
    public String createPlayerSnapshot(Player player, String label) {
        String name = label.equalsIgnoreCase("now") ? nowTimestamp() : sanitize(label);
        File dir = new File(snapshotsDir, "players/" + player.getUniqueId() + "/" + name);
        dir.mkdirs();

        try {
            savePlayerData(player, dir);
            pruneSnapshots(new File(snapshotsDir, "players/" + player.getUniqueId()),
                    plugin.getConfig().getInt("max-player-snapshots", 20));
            return name;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create player snapshot for " + player.getName(), e);
            return null;
        }
    }

    /**
     * Deletes a player snapshot by label. Use "now" to delete the most recent one.
     */
    public boolean deletePlayerSnapshot(Player player, String label) {
        File dir = resolvePlayerSnapshot(player.getUniqueId(), label);
        if (dir == null || !dir.exists()) return false;
        return deleteDir(dir);
    }

    /**
     * Loads a player snapshot. Restores inventory, armor, ender chest, XP, health, food, location.
     */
    public boolean loadPlayerSnapshot(Player player, String label) {
        File dir = resolvePlayerSnapshot(player.getUniqueId(), label);
        if (dir == null || !dir.exists()) return false;

        try {
            loadPlayerData(player, dir);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player snapshot", e);
            return false;
        }
    }

    /**
     * Returns a list of snapshot names for a player, newest first.
     */
    public List<String> listPlayerSnapshots(UUID uuid) {
        File dir = new File(snapshotsDir, "players/" + uuid);
        if (!dir.exists()) return Collections.emptyList();
        File[] files = dir.listFiles(File::isDirectory);
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(File::getName)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    //  SERVER SNAPSHOTS
    // ─────────────────────────────────────────────

    /**
     * Creates a snapshot of every online player.
     * @param label "now" or custom name
     * @return snapshot name
     */
    public String createServerSnapshot(String label) {
        String name = label.equalsIgnoreCase("now") ? nowTimestamp() : sanitize(label);
        File dir = new File(snapshotsDir, "server/" + name);
        dir.mkdirs();

        for (Player p : Bukkit.getOnlinePlayers()) {
            File pDir = new File(dir, p.getUniqueId().toString());
            pDir.mkdirs();
            try {
                savePlayerData(p, pDir);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save " + p.getName() + " in server snapshot", e);
            }
        }

        // Save server metadata
        try {
            Properties meta = new Properties();
            meta.setProperty("timestamp", nowTimestamp());
            meta.setProperty("online_players", String.valueOf(Bukkit.getOnlinePlayers().size()));
            meta.setProperty("world_time", String.valueOf(Bukkit.getWorlds().get(0).getTime()));
            try (FileOutputStream fos = new FileOutputStream(new File(dir, "server_meta.properties"))) {
                meta.store(fos, "DataGuard Server Snapshot Metadata");
            }
        } catch (Exception ignored) {}

        pruneSnapshots(new File(snapshotsDir, "server"),
                plugin.getConfig().getInt("max-server-snapshots", 10));
        return name;
    }

    /**
     * Deletes a server snapshot by label.
     */
    public boolean deleteServerSnapshot(String label) {
        File dir = resolveServerSnapshot(label);
        if (dir == null || !dir.exists()) return false;
        return deleteDir(dir);
    }

    /**
     * Loads a server snapshot — restores all players who are currently online and were in the snapshot.
     */
    public int loadServerSnapshot(String label) {
        File dir = resolveServerSnapshot(label);
        if (dir == null || !dir.exists()) return -1;

        int restored = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            File pDir = new File(dir, p.getUniqueId().toString());
            if (pDir.exists()) {
                try {
                    loadPlayerData(p, pDir);
                    restored++;
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to restore " + p.getName() + " from server snapshot", e);
                }
            }
        }
        return restored;
    }

    /**
     * Returns list of server snapshot names, newest first.
     */
    public List<String> listServerSnapshots() {
        File dir = new File(snapshotsDir, "server");
        if (!dir.exists()) return Collections.emptyList();
        File[] files = dir.listFiles(File::isDirectory);
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(File::getName)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    //  PER-PLAYER AUTO-SAVE TOGGLE
    // ─────────────────────────────────────────────

    public boolean isPlayerAutoSaveEnabled(UUID uuid) {
        return playerAutoSave.getOrDefault(uuid, true);
    }

    public void setPlayerAutoSave(UUID uuid, boolean enabled) {
        playerAutoSave.put(uuid, enabled);
    }

    // ─────────────────────────────────────────────
    //  INTERNAL – DATA SERIALIZATION
    // ─────────────────────────────────────────────

    private void savePlayerData(Player player, File dir) throws IOException {
        Properties props = new Properties();

        // Basic stats
        props.setProperty("health", String.valueOf(player.getHealth()));
        props.setProperty("max_health", String.valueOf(player.getAttribute(
                org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null
                ? Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)).getValue()
                : 20.0));
        props.setProperty("food_level", String.valueOf(player.getFoodLevel()));
        props.setProperty("saturation", String.valueOf(player.getSaturation()));
        props.setProperty("exhaustion", String.valueOf(player.getExhaustion()));
        props.setProperty("xp_level", String.valueOf(player.getLevel()));
        props.setProperty("xp_progress", String.valueOf(player.getExp()));
        props.setProperty("total_xp", String.valueOf(player.getTotalExperience()));
        props.setProperty("game_mode", player.getGameMode().name());
        props.setProperty("fire_ticks", String.valueOf(player.getFireTicks()));

        // Location
        Location loc = player.getLocation();
        props.setProperty("world", loc.getWorld().getName());
        props.setProperty("x", String.valueOf(loc.getX()));
        props.setProperty("y", String.valueOf(loc.getY()));
        props.setProperty("z", String.valueOf(loc.getZ()));
        props.setProperty("yaw", String.valueOf(loc.getYaw()));
        props.setProperty("pitch", String.valueOf(loc.getPitch()));

        // Player name at time of snapshot
        props.setProperty("player_name", player.getName());
        props.setProperty("snapshot_time", nowTimestamp());

        try (FileOutputStream fos = new FileOutputStream(new File(dir, "player_stats.properties"))) {
            props.store(fos, "DataGuard Player Snapshot");
        }

        // Inventory
        NBTUtil.saveInventory(player.getInventory().getContents(), new File(dir, "inventory.dat"));

        // Armor
        NBTUtil.saveInventory(player.getInventory().getArmorContents(), new File(dir, "armor.dat"));

        // Off-hand
        NBTUtil.saveInventory(new ItemStack[]{player.getInventory().getItemInOffHand()}, new File(dir, "offhand.dat"));

        // Ender chest
        NBTUtil.saveInventory(player.getEnderChest().getContents(), new File(dir, "enderchest.dat"));

        // Active potion effects
        NBTUtil.savePotionEffects(player.getActivePotionEffects(), new File(dir, "potions.dat"));
    }

    private void loadPlayerData(Player player, File dir) throws IOException {
        File propsFile = new File(dir, "player_stats.properties");
        if (propsFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                props.load(fis);
            }

            // Health
            double maxHealth = Double.parseDouble(props.getProperty("max_health", "20.0"));
            var maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(maxHealth);

            double health = Math.min(Double.parseDouble(props.getProperty("health", "20.0")), maxHealth);
            player.setHealth(health);

            player.setFoodLevel(Integer.parseInt(props.getProperty("food_level", "20")));
            player.setSaturation(Float.parseFloat(props.getProperty("saturation", "5.0")));
            player.setExhaustion(Float.parseFloat(props.getProperty("exhaustion", "0.0")));
            player.setLevel(Integer.parseInt(props.getProperty("xp_level", "0")));
            player.setExp(Float.parseFloat(props.getProperty("xp_progress", "0.0")));
            player.setTotalExperience(Integer.parseInt(props.getProperty("total_xp", "0")));
            player.setFireTicks(Integer.parseInt(props.getProperty("fire_ticks", "0")));

            String gmName = props.getProperty("game_mode", "SURVIVAL");
            try { player.setGameMode(GameMode.valueOf(gmName)); } catch (Exception ignored) {}

            // Teleport to saved location
            String worldName = props.getProperty("world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : player.getWorld();
            if (world != null) {
                double x = Double.parseDouble(props.getProperty("x", "0"));
                double y = Double.parseDouble(props.getProperty("y", "64"));
                double z = Double.parseDouble(props.getProperty("z", "0"));
                float yaw = Float.parseFloat(props.getProperty("yaw", "0"));
                float pitch = Float.parseFloat(props.getProperty("pitch", "0"));
                player.teleport(new Location(world, x, y, z, yaw, pitch));
            }
        }

        // Inventory
        File invFile = new File(dir, "inventory.dat");
        if (invFile.exists()) {
            ItemStack[] inv = NBTUtil.loadInventory(invFile, 36);
            player.getInventory().setContents(inv);
        }

        // Armor
        File armorFile = new File(dir, "armor.dat");
        if (armorFile.exists()) {
            ItemStack[] armor = NBTUtil.loadInventory(armorFile, 4);
            player.getInventory().setArmorContents(armor);
        }

        // Off-hand
        File offhandFile = new File(dir, "offhand.dat");
        if (offhandFile.exists()) {
            ItemStack[] offhand = NBTUtil.loadInventory(offhandFile, 1);
            if (offhand.length > 0 && offhand[0] != null) {
                player.getInventory().setItemInOffHand(offhand[0]);
            }
        }

        // Ender chest
        File ecFile = new File(dir, "enderchest.dat");
        if (ecFile.exists()) {
            ItemStack[] ec = NBTUtil.loadInventory(ecFile, 27);
            player.getEnderChest().setContents(ec);
        }

        // Potions
        File potFile = new File(dir, "potions.dat");
        if (potFile.exists()) {
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
            NBTUtil.loadPotionEffects(potFile).forEach(player::addPotionEffect);
        }

        player.updateInventory();
    }

    // ─────────────────────────────────────────────
    //  INTERNAL – HELPERS
    // ─────────────────────────────────────────────

    /** Resolve a player snapshot folder. "now"/"latest" returns the most recent. */
    private File resolvePlayerSnapshot(UUID uuid, String label) {
        if (label.equalsIgnoreCase("now") || label.equalsIgnoreCase("latest")) {
            List<String> snaps = listPlayerSnapshots(uuid);
            if (snaps.isEmpty()) return null;
            return new File(snapshotsDir, "players/" + uuid + "/" + snaps.get(0));
        }
        return new File(snapshotsDir, "players/" + uuid + "/" + sanitize(label));
    }

    /** Resolve a server snapshot folder. "now"/"latest" returns the most recent. */
    private File resolveServerSnapshot(String label) {
        if (label.equalsIgnoreCase("now") || label.equalsIgnoreCase("latest")) {
            List<String> snaps = listServerSnapshots();
            if (snaps.isEmpty()) return null;
            return new File(snapshotsDir, "server/" + snaps.get(0));
        }
        return new File(snapshotsDir, "server/" + sanitize(label));
    }

    /** Delete oldest snapshots if over the limit. */
    private void pruneSnapshots(File parentDir, int maxCount) {
        if (maxCount <= 0) return;
        File[] dirs = parentDir.listFiles(File::isDirectory);
        if (dirs == null) return;
        List<File> sorted = Arrays.stream(dirs)
                .sorted(Comparator.comparingLong(File::lastModified))
                .collect(Collectors.toList());
        while (sorted.size() > maxCount) {
            deleteDir(sorted.remove(0));
        }
    }

    /** Recursively delete a directory. */
    private boolean deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        return dir.delete();
    }

    /** Strip unsafe characters from snapshot names. */
    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }
}
