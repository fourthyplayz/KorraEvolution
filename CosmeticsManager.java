package me.furthyskills.skillstree.managers;

import me.furthyskills.skillstree.SkillsTreePlugin;
import me.furthyskills.skillstree.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public class CosmeticsManager {
    private final SkillsTreePlugin plugin;
    private final DataManager dataManager;
    private final Logger logger;

    private final Map<UUID, Set<String>> ownedCosmetics = new ConcurrentHashMap<>();
    private final Map<UUID, String> equippedTrail = new ConcurrentHashMap<>();
    private final Map<UUID, String> equippedAura = new ConcurrentHashMap<>();
    private final Map<UUID, String> equippedTitle = new ConcurrentHashMap<>();
    private final Map<String, ShopItem> shopItems = new HashMap<>();

    public CosmeticsManager(SkillsTreePlugin plugin, DataManager dataManager, Logger logger) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.logger = logger;
    }

    public void loadPlayerCosmetics() {
        FileConfiguration config = dataManager.getCosmeticsConfig();
        if (config.getConfigurationSection("Players") == null) return;

        for (String uuidStr : config.getConfigurationSection("Players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);

                List<String> owned = config.getStringList("Players." + uuidStr + ".owned");
                ownedCosmetics.put(uuid, new CopyOnWriteArraySet<>(owned));

                String trail = config.getString("Players." + uuidStr + ".equippedTrail");
                if (trail != null && !trail.isEmpty()) equippedTrail.put(uuid, trail);

                String aura = config.getString("Players." + uuidStr + ".equippedAura");
                if (aura != null && !aura.isEmpty()) equippedAura.put(uuid, aura);

                String title = config.getString("Players." + uuidStr + ".equippedTitle");
                if (title != null && !title.isEmpty()) equippedTitle.put(uuid, title);

            } catch (IllegalArgumentException e) {
                logger.warning("Invalid UUID in cosmetics.yml: " + uuidStr);
            }
        }
        logger.info("Loaded cosmetics for " + ownedCosmetics.size() + " players.");
    }

    public void prepareConfigSave() {
        FileConfiguration config = dataManager.getCosmeticsConfig();
        synchronized (dataManager.configLock) {
            for (Map.Entry<UUID, Set<String>> entry : ownedCosmetics.entrySet()) {
                String uuidStr = entry.getKey().toString();
                config.set("Players." + uuidStr + ".owned", new ArrayList<>(entry.getValue()));

                String trail = equippedTrail.get(entry.getKey());
                if (trail != null) config.set("Players." + uuidStr + ".equippedTrail", trail);

                String aura = equippedAura.get(entry.getKey());
                if (aura != null) config.set("Players." + uuidStr + ".equippedAura", aura);

                String title = equippedTitle.get(entry.getKey());
                if (title != null) config.set("Players." + uuidStr + ".equippedTitle", title);
            }
        }
    }

    public void initializeShop() {
        shopItems.put("trail_hearts",   new ShopItem("trail_hearts",   "§c💕 Heart Trail",         "trail",  500,  Material.RED_DYE,            "HEART", logger));
        shopItems.put("trail_flames",   new ShopItem("trail_flames",   "§6🔥 Flame Trail",          "trail",  800,  Material.BLAZE_POWDER,       "FLAME", logger));
        shopItems.put("trail_souls",    new ShopItem("trail_souls",    "§b👻 Soul Trail",           "trail", 1000,  Material.SOUL_SAND,          "SOUL_FIRE_FLAME", logger));
        shopItems.put("trail_enchant",  new ShopItem("trail_enchant",  "§d✨ Enchant Trail",        "trail",  600,  Material.ENCHANTED_BOOK,     "ENCHANT", logger));
        shopItems.put("trail_cloud",    new ShopItem("trail_cloud",    "§f☁ Cloud Trail",          "trail",  400,  Material.WHITE_WOOL,         "CLOUD", logger));
        shopItems.put("trail_notes",    new ShopItem("trail_notes",    "§e🎵 Note Trail",           "trail",  700,  Material.NOTE_BLOCK,         "NOTE", logger));
        shopItems.put("trail_redstone", new ShopItem("trail_redstone", "§c⚡ Redstone Trail",       "trail",  600,  Material.REDSTONE,           "GUST", logger));
        shopItems.put("trail_endrod",   new ShopItem("trail_endrod",   "§f✨ Sparkle Trail",        "trail",  900,  Material.END_ROD,            "END_ROD", logger));
        shopItems.put("trail_cherry",   new ShopItem("trail_cherry",   "§d🌸 Cherry Blossom Trail", "trail", 1200,  Material.PINK_PETALS,        "CHERRY_LEAVES", logger));
        shopItems.put("trail_slime",    new ShopItem("trail_slime",    "§a💚 Slime Trail",          "trail",  500,  Material.SLIME_BALL,         "ITEM_SLIME", logger));

        shopItems.put("aura_fire",      new ShopItem("aura_fire",      "§6🔥 Fire Aura",            "aura",  1500, Material.BLAZE_POWDER,        "FLAME", logger));
        shopItems.put("aura_water",     new ShopItem("aura_water",     "§b💧 Water Aura",           "aura",  1500, Material.WATER_BUCKET,        "DRIPPING_WATER", logger));
        shopItems.put("aura_earth",     new ShopItem("aura_earth",     "§a🌿 Earth Aura",           "aura",  1500, Material.GRASS_BLOCK,         "WITCH", logger));
        shopItems.put("aura_air",       new ShopItem("aura_air",       "§f💨 Air Aura",             "aura",  1500, Material.FEATHER,             "CLOUD", logger));
        shopItems.put("aura_lightning", new ShopItem("aura_lightning", "§e⚡ Lightning Aura",       "aura",  2500, Material.END_ROD,             "ELECTRIC_SPARK", logger));
        shopItems.put("aura_ice",       new ShopItem("aura_ice",       "§b❄ Ice Aura",             "aura",  2000, Material.ICE,                 "SNOWFLAKE", logger));
        shopItems.put("aura_lava",      new ShopItem("aura_lava",      "§c🌋 Lava Aura",            "aura",  2500, Material.LAVA_BUCKET,         "DRIPPING_LAVA", logger));
        shopItems.put("aura_ender",     new ShopItem("aura_ender",     "§5🌀 Ender Aura",           "aura",  3000, Material.ENDER_PEARL,         "PORTAL", logger));
        shopItems.put("aura_rainbow",   new ShopItem("aura_rainbow",   "§c🌈 Rainbow Aura",         "aura",  3500, Material.PRISMARINE_CRYSTALS, "GLOW", logger));
        shopItems.put("aura_void",      new ShopItem("aura_void",      "§8🕳 Void Aura",            "aura",  4000, Material.OBSIDIAN,            "SMOKE", logger));
        shopItems.put("aura_cherry",    new ShopItem("aura_cherry",    "§d🌸 Cherry Aura",          "aura",  3000, Material.CHERRY_SAPLING,      "CHERRY_LEAVES", logger));
        shopItems.put("aura_golden",    new ShopItem("aura_golden",    "§6✨ Golden Aura",          "aura",  5000, Material.GOLD_BLOCK,          "WAX_OFF", logger));
        shopItems.put("aura_cosmic",    new ShopItem("aura_cosmic",    "§5🌌 Cosmic Aura",          "aura",  6000, Material.END_CRYSTAL,         "END_ROD", logger));
        shopItems.put("aura_toxic",     new ShopItem("aura_toxic",     "§a☢ Toxic Aura",           "aura",  4500, Material.SLIME_BLOCK,         "FALLING_SPORE_BLOSSOM", logger));
        shopItems.put("aura_soul",      new ShopItem("aura_soul",      "§b👻 Soul Aura",            "aura",  5500, Material.SOUL_LANTERN,        "SOUL_FIRE_FLAME", logger));

        shopItems.put("title_master",   new ShopItem("title_master",   "§e[Master]",   "title",  5000, Material.DIAMOND_SWORD,         "§e[Master] ", logger));
        shopItems.put("title_legend",   new ShopItem("title_legend",   "§6[Legend]",   "title", 10000, Material.NETHERITE_SWORD,       "§6[Legend] ", logger));
        shopItems.put("title_champion", new ShopItem("title_champion", "§c[Champion]", "title",  7000, Material.ENCHANTED_GOLDEN_APPLE,"§c[Champion] ", logger));
        shopItems.put("title_elite",    new ShopItem("title_elite",    "§b[Elite]",    "title",  3000, Material.DIAMOND,               "§b[Elite] ", logger));
        shopItems.put("title_grinder",  new ShopItem("title_grinder",  "§a[No Life]",  "title",  8000, Material.DIAMOND_PICKAXE,       "§a[No Life] ", logger));
        shopItems.put("title_god",      new ShopItem("title_god",      "§6[God]",      "title", 15000, Material.TOTEM_OF_UNDYING,      "§6[God] ", logger));
        shopItems.put("title_pro",      new ShopItem("title_pro",      "§b[Pro]",      "title",  2000, Material.DIAMOND,               "§b[Pro] ", logger));
        shopItems.put("title_noob",     new ShopItem("title_noob",     "§7[Noob]",     "title",   100, Material.WOODEN_SWORD,          "§7[Noob] ", logger));

        logger.info("Loaded " + shopItems.size() + " shop items.");
    }

    public Map<String, ShopItem> getShopItems() {
        return shopItems;
    }

    public Set<String> getOwned(UUID uuid) {
        return ownedCosmetics.getOrDefault(uuid, new HashSet<>());
    }

    public boolean isOwned(UUID uuid, String shopId) {
        return getOwned(uuid).contains(shopId);
    }

    public boolean isEquipped(UUID uuid, String shopId) {
        return shopId.equals(equippedTrail.get(uuid)) ||
               shopId.equals(equippedAura.get(uuid)) ||
               shopId.equals(equippedTitle.get(uuid));
    }

    public void addOwned(UUID uuid, String shopId) {
        ownedCosmetics.computeIfAbsent(uuid, k -> new CopyOnWriteArraySet<>()).add(shopId);
        dataManager.setCosmeticsDirty(true);
    }

    public void toggleEquip(UUID uuid, ShopItem item) {
        switch (item.type) {
            case "trail":
                if (item.id.equals(equippedTrail.get(uuid))) { equippedTrail.remove(uuid); }
                else { equippedTrail.put(uuid, item.id); }
                break;
            case "aura":
                if (item.id.equals(equippedAura.get(uuid))) { equippedAura.remove(uuid); }
                else { equippedAura.put(uuid, item.id); }
                break;
            case "title":
                if (item.id.equals(equippedTitle.get(uuid))) { equippedTitle.remove(uuid); }
                else { equippedTitle.put(uuid, item.id); }
                break;
        }
        dataManager.setCosmeticsDirty(true);
    }

    public void unequipAll(UUID uuid) {
        equippedAura.remove(uuid);
        equippedTrail.remove(uuid);
        equippedTitle.remove(uuid);
        dataManager.setCosmeticsDirty(true);
    }

    public void updatePlayerTitle(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        String titleId = equippedTitle.get(uuid);
        if (titleId != null && shopItems.containsKey(titleId)) {
            ShopItem item = shopItems.get(titleId);
            if (item != null) {
                player.setPlayerListName(item.effect + player.getName());
                return;
            }
        }
        player.setPlayerListName(player.getName());
    }

    // Effect tick handling
    private int particleRoundRobinOffset = 0;

    public void startCosmeticEffects(long particleTaskInterval, int maxParticlesPerTick) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            final long now = System.currentTimeMillis();
            int particleCount = 0;

            Player[] online = Bukkit.getOnlinePlayers().toArray(new Player[0]);
            if (online.length == 0) return;

            for (int idx = 0; idx < online.length; idx++) {
                int i = (particleRoundRobinOffset + idx) % online.length;
                Player p = online[i];
                if (p == null || !p.isOnline()) continue;
                if (!plugin.isInSpawnWorld(p)) continue;

                World world = p.getWorld();
                if (world == null) continue;

                Location loc = p.getLocation();
                UUID uuid = p.getUniqueId();

                // TRAIL
                String trailId = equippedTrail.get(uuid);
                if (trailId != null) {
                    ShopItem item = shopItems.get(trailId);
                    if (item != null && p.getVelocity().lengthSquared() > 0.01 && particleCount < maxParticlesPerTick) {
                        spawnSafeParticle(world, item.particle, loc.getX(), loc.getY() + 0.1, loc.getZ(), 3, 0.2, 0.2, 0.2, 0);
                        particleCount += 3;
                    }
                }

                // AURA
                if (particleCount >= maxParticlesPerTick) { particleRoundRobinOffset = i + 1; break; }

                String auraId = equippedAura.get(uuid);
                if (auraId == null || !shopItems.containsKey(auraId)) continue;

                double lx = loc.getX();
                double ly = loc.getY();
                double lz = loc.getZ();

                try {
                    switch (auraId) {
                        case "aura_fire":      particleCount += renderFireAura(lx, ly, lz, world, now);      break;
                        case "aura_water":     particleCount += renderWaterAura(lx, ly, lz, world, now);     break;
                        case "aura_earth":     particleCount += renderEarthAura(lx, ly, lz, world, now);     break;
                        case "aura_air":       particleCount += renderAirAura(lx, ly, lz, world, now);       break;
                        case "aura_lightning": particleCount += renderLightningAura(lx, ly, lz, world, now); break;
                        case "aura_ice":       particleCount += renderIceAura(lx, ly, lz, world, now);       break;
                        case "aura_lava":      particleCount += renderLavaAura(lx, ly, lz, world, now);      break;
                        case "aura_ender":     particleCount += renderEnderAura(lx, ly, lz, world, now);     break;
                        case "aura_rainbow":   particleCount += renderRainbowAura(lx, ly, lz, world, now);   break;
                        case "aura_void":      particleCount += renderVoidAura(lx, ly, lz, world, now);      break;
                        case "aura_cherry":    particleCount += renderCherryAura(lx, ly, lz, world, now);    break;
                        case "aura_golden":    particleCount += renderGoldenAura(lx, ly, lz, world, now);    break;
                        case "aura_cosmic":    particleCount += renderCosmicAura(lx, ly, lz, world, now);    break;
                        case "aura_toxic":     particleCount += renderToxicAura(lx, ly, lz, world, now);     break;
                        case "aura_soul":      particleCount += renderSoulAura(lx, ly, lz, world, now);      break;
                    }
                } catch (Exception e) {
                    logger.warning("[CosmeticEffects] Error rendering " + auraId + " for " + p.getName() + ": " + e.getMessage());
                }
            }
        }, particleTaskInterval, particleTaskInterval);
    }

    private void spawnSafeParticle(World world, String particleName, double lx, double ly, double lz, int count, double offX, double offY, double offZ, double extra) {
        if (world == null || particleName == null) return;
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase(java.util.Locale.ROOT));
            world.spawnParticle(particle, lx, ly, lz, count, offX, offY, offZ, extra);
        } catch (Exception e) {
            logger.severe("[Cosmetics DEBUG] spawnSafeParticle(String) failed for '" + particleName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void spawnSafeParticle(World world, Particle particle, double lx, double ly, double lz, int count, double offX, double offY, double offZ, double extra) {
        if (world == null || particle == null) return;
        try {
            world.spawnParticle(particle, lx, ly, lz, count, offX, offY, offZ, extra);
        } catch (Exception e) {
            logger.severe("[Cosmetics DEBUG] spawnSafeParticle(Particle) failed for '" + particle.name() + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    // AURA RENDERS
    private int renderFireAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        for (int i = 0; i < 3; i++) {
            double angle = (now / 200.0 + i * 2) % (Math.PI * 2);
            double x = Math.cos(angle) * 0.8;
            double z = Math.sin(angle) * 0.8;
            double y = (now % 2000) / 1000.0;
            spawnSafeParticle(world, "FLAME", lx + (x), ly + (y), lz + (z), 1, 0, 0, 0, 0);
            count++;
        }
        spawnSafeParticle(world, "SMOKE", lx + (0), ly + (0.5), lz + (0), 2, 0.3, 0.3, 0.3, 0.01);
        return count + 2;
    }

    private int renderWaterAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        for (int i = 0; i < 2; i++) {
            double offsetX = (Math.random() - 0.5) * 1.2;
            double offsetZ = (Math.random() - 0.5) * 1.2;
            spawnSafeParticle(world, "DRIPPING_WATER", lx + (offsetX), ly + (2), lz + (offsetZ), 1, 0, 0, 0, 0);
            spawnSafeParticle(world, "BUBBLE_POP",     lx + (offsetX), ly + (0.2), lz + (offsetZ), 1, 0, 0.2, 0, 0);
            count += 2;
        }
        return count;
    }

    private int renderEarthAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double earthRadius = 1.3;
        for (int i = 0; i < 6; i++) {
            double angle = (now / 800.0 + i * Math.PI / 3) % (Math.PI * 2);
            double x = Math.cos(angle) * earthRadius;
            double z = Math.sin(angle) * earthRadius;
            spawnSafeParticle(world, "HAPPY_VILLAGER", lx + (x), ly + (0.3), lz + (z), 3, 0.1, 0.1, 0.1, 0);
            count += 3;
        }
        return count;
    }

    private int renderAirAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double airRadius = 1.0;
        for (int i = 0; i < 12; i++) {
            double angle = (now / 100.0 + i * Math.PI / 6) % (Math.PI * 2);
            double x = Math.cos(angle) * airRadius;
            double z = Math.sin(angle) * airRadius;
            spawnSafeParticle(world, "CLOUD", lx + (x), ly + (1), lz + (z), 1, 0, 0, 0, 0.02);
            count++;
        }
        return count;
    }

    private int renderLightningAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 2;
            spawnSafeParticle(world, "ELECTRIC_SPARK", lx + (offsetX), ly + (offsetY), lz + (offsetZ), 2, 0.1, 0.1, 0.1, 0);
            count += 2;
        }
        return count;
    }

    private int renderIceAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double iceRadius = 1.2;
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * Math.PI * 2;
            double x = Math.cos(angle) * iceRadius;
            double z = Math.sin(angle) * iceRadius;
            spawnSafeParticle(world, "SNOWFLAKE", lx + (x), ly + (2.5), lz + (z), 1, 0, -0.5, 0, 0);
            count++;
        }
        spawnSafeParticle(world, "SNOWFLAKE", lx + (0), ly + (1), lz + (0), 3, 0.5, 0.5, 0.5, 0);
        return count + 3;
    }

    private int renderLavaAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        for (int i = 0; i < 3; i++) {
            double offsetX = (Math.random() - 0.5) * 1.5;
            double offsetZ = (Math.random() - 0.5) * 1.5;
            spawnSafeParticle(world, "DRIPPING_LAVA", lx + (offsetX), ly + (2), lz + (offsetZ), 1, 0, 0, 0, 0);
            count++;
        }
        double lavaRadius = 0.8;
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * Math.PI * 2;
            double x = Math.cos(angle) * lavaRadius;
            double z = Math.sin(angle) * lavaRadius;
            spawnSafeParticle(world, "FLAME", lx + (x), ly + (0.1), lz + (z), 1, 0, 0, 0, 0);
            count++;
        }
        return count;
    }

    private int renderEnderAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double enderRadius = 1.5;
        for (int i = 0; i < 10; i++) {
            double angle  = (now / 300.0 - i * 0.3) % (Math.PI * 2);
            double radius = enderRadius * (1 - i * 0.1);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            spawnSafeParticle(world, "PORTAL", lx + (x), ly + (1 + i * 0.1), lz + (z), 1, 0, 0, 0, 0.5);
            count++;
        }
        return count;
    }

    private int renderRainbowAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double rainbowRadius = 1.2;
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 8; i++) {
                double angle = (now / 500.0 + i * Math.PI / 4 + ring) % (Math.PI * 2);
                double x = Math.cos(angle) * rainbowRadius;
                double z = Math.sin(angle) * rainbowRadius;
                spawnSafeParticle(world, "GLOW", lx + (x), ly + (0.5 + ring * 0.5), lz + (z), 1, 0, 0, 0, 0);
                count++;
            }
        }
        return count;
    }

    private int renderVoidAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double voidRadius = 1.0 + Math.sin(now / 500.0) * 0.3;
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * Math.PI * 2;
            double x = Math.cos(angle) * voidRadius;
            double z = Math.sin(angle) * voidRadius;
            spawnSafeParticle(world, "SMOKE", lx + (x), ly + (1), lz + (z), 2, 0.1, 0.1, 0.1, 0.01);
            count += 2;
        }
        spawnSafeParticle(world, "SQUID_INK", lx + (0), ly + (1), lz + (0), 1, 0.3, 0.3, 0.3, 0);
        return count + 1;
    }

    private int renderCherryAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        for (int i = 0; i < 5; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 2 + 0.5;
            double offsetZ = (Math.random() - 0.5) * 2;
            spawnSafeParticle(world, "CHERRY_LEAVES", lx + (offsetX), ly + (offsetY), lz + (offsetZ), 1, 0, -0.2, 0, 0);
            count++;
        }
        return count;
    }

    private int renderGoldenAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double goldenRadius = 1.0;
        for (int i = 0; i < 6; i++) {
            double angle = (now / 400.0 + i * Math.PI / 3) % (Math.PI * 2);
            double x = Math.cos(angle) * goldenRadius;
            double z = Math.sin(angle) * goldenRadius;
            double y = Math.random() * 2;
            spawnSafeParticle(world, "WAX_OFF", lx + (x), ly + (y), lz + (z), 2, 0, 0.2, 0, 0);
            count += 2;
        }
        return count;
    }

    private int renderCosmicAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double cosmicRadius = 1.4;
        for (int i = 0; i < 15; i++) {
            double angle = (now / 600.0 + i * 0.4) % (Math.PI * 2);
            double x = Math.cos(angle) * cosmicRadius;
            double z = Math.sin(angle) * cosmicRadius;
            double y = Math.sin(now / 400.0 + i) * 1.5 + 1;
            spawnSafeParticle(world, "END_ROD", lx + (x), ly + (y), lz + (z), 1, 0, 0, 0, 0);
            count++;
        }
        return count;
    }

    private int renderToxicAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        for (int i = 0; i < 6; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 2;
            spawnSafeParticle(world, "FALLING_SPORE_BLOSSOM", lx + (offsetX), ly + (offsetY), lz + (offsetZ), 1, 0, 0.2, 0, 0);
            count++;
        }
        return count;
    }

    private int renderSoulAura(double lx, double ly, double lz, World world, long now) {
        int count = 0;
        double soulRadius = 1.1;
        for (int i = 0; i < 8; i++) {
            double angle = (now / 350.0 + i * Math.PI / 4) % (Math.PI * 2);
            double x = Math.cos(angle) * soulRadius;
            double z = Math.sin(angle) * soulRadius;
            spawnSafeParticle(world, "SOUL_FIRE_FLAME", lx + (x), ly + (0.5), lz + (z), 2, 0, 0.2, 0, 0.01);
            count += 2;
        }
        return count;
    }
}
