package dev.dataguard.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.*;
import java.util.*;

/**
 * Utility class that uses Bukkit's built-in YAML serialization to save/load
 * ItemStacks and PotionEffects. This approach is safe across all data components
 * including custom items, enchantments, NBT-heavy items (Netherite, etc).
 */
public class NBTUtil {

    public static void saveInventory(ItemStack[] contents, File file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("size", contents.length);
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                yaml.set("item." + i, contents[i]);
            }
        }
        yaml.save(file);
    }

    public static ItemStack[] loadInventory(File file, int defaultSize) throws IOException {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int size = yaml.getInt("size", defaultSize);
        ItemStack[] contents = new ItemStack[size];
        if (yaml.isConfigurationSection("item")) {
            for (String key : Objects.requireNonNull(yaml.getConfigurationSection("item")).getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < size) {
                        contents[slot] = yaml.getItemStack("item." + key);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return contents;
    }

    public static void savePotionEffects(Collection<PotionEffect> effects, File file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        int i = 0;
        for (PotionEffect effect : effects) {
            yaml.set("effects." + i + ".type", effect.getType().getKey().toString());
            yaml.set("effects." + i + ".duration", effect.getDuration());
            yaml.set("effects." + i + ".amplifier", effect.getAmplifier());
            yaml.set("effects." + i + ".ambient", effect.isAmbient());
            yaml.set("effects." + i + ".particles", effect.hasParticles());
            yaml.set("effects." + i + ".icon", effect.hasIcon());
            i++;
        }
        yaml.save(file);
    }

    public static List<PotionEffect> loadPotionEffects(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<PotionEffect> effects = new ArrayList<>();
        if (!yaml.isConfigurationSection("effects")) return effects;
        for (String key : Objects.requireNonNull(yaml.getConfigurationSection("effects")).getKeys(false)) {
            try {
                String typeName = yaml.getString("effects." + key + ".type", "");
                org.bukkit.potion.PotionEffectType type = org.bukkit.Registry.POTION_EFFECT_TYPE
                        .get(org.bukkit.NamespacedKey.fromString(typeName));
                if (type == null) continue;
                int duration = yaml.getInt("effects." + key + ".duration", 200);
                int amplifier = yaml.getInt("effects." + key + ".amplifier", 0);
                boolean ambient = yaml.getBoolean("effects." + key + ".ambient", false);
                boolean particles = yaml.getBoolean("effects." + key + ".particles", true);
                boolean icon = yaml.getBoolean("effects." + key + ".icon", true);
                effects.add(new PotionEffect(type, duration, amplifier, ambient, particles, icon));
            } catch (Exception ignored) {}
        }
        return effects;
    }
}
