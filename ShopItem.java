package me.furthyskills.skillstree.models;

import org.bukkit.Material;
import org.bukkit.Particle;

public class ShopItem {
    public final String id;
    public final String displayName;
    public final String type;
    public final int cost;
    public final Material icon;
    public final String effect;   // kept for title prefix and logging
    public final Particle particle; // cached at construction — eliminates repeated valueOf() calls

    public ShopItem(String id, String displayName, String type, int cost, Material icon, String effectString, java.util.logging.Logger logger) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.cost = cost;
        this.icon = icon;
        this.effect = effectString;

        // Parse once; fall back to HAPPY_VILLAGER so trail effects always render something.
        Particle parsed = Particle.HAPPY_VILLAGER;
        try {
            parsed = Particle.valueOf(effectString.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            if (logger != null) {
                logger.warning("ShopItem '" + id + "': unknown particle '" + effectString + "' — defaulted to HAPPY_VILLAGER");
            }
        }
        this.particle = parsed;
    }
}
