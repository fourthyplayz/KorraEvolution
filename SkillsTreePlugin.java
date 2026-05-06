package me.furthyskills.skillstree;

import me.furthyskills.skillstree.commands.SkillsTreeCommand;
import me.furthyskills.skillstree.gui.GuiListener;
import me.furthyskills.skillstree.gui.GuiManager;
import me.furthyskills.skillstree.listeners.PlayerActivityListener;
import me.furthyskills.skillstree.managers.AbilityScannerManager;
import me.furthyskills.skillstree.managers.CosmeticsManager;
import me.furthyskills.skillstree.managers.DataManager;
import me.furthyskills.skillstree.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main entry point for the SkillsTree plugin.
 * This class handles initialization, configuration loading, and manager setup.
 *
 * @author Furthy
 * @version 1.0.2
 */
public class SkillsTreePlugin extends JavaPlugin {
    
    /** The manager responsible for data persistence and configuration. */
    private DataManager dataManager;
    
    /** The manager handling economy interactions (Vault or local). */
    private EconomyManager economyManager;
    
    /** Scans and indexes ProjectKorra abilities. */
    private AbilityScannerManager abilityScanner;
    
    /** Manages player cosmetic effects and trails. */
    private CosmeticsManager cosmeticsManager;
    
    /** Handles GUI creation and navigation. */
    private GuiManager guiManager;

    private String permissionCommand;
    private String spawnWorldName;

    private final Map<UUID, Integer> unlockedCounts = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        permissionCommand = getConfig().getString("permission-command", "lp user {player} permission set bending.ability.{ability} true");
        long afkTimeoutMs = getConfig().getLong("afk-timeout-seconds", 120) * 1000;
        int afkDistanceThreshold = getConfig().getInt("afk-distance-threshold", 10);
        spawnWorldName = getConfig().getString("spawn-world-name", "spawn");
        int passivePointsActive = getConfig().getInt("passive-points.active", 100);
        int passivePointsAfk = getConfig().getInt("passive-points.afk", 54);
        long particleTaskInterval = getConfig().getLong("particle-task-interval-ticks", 5L);
        int maxParticlesPerTick = getConfig().getInt("max-particles-per-tick", 100);

        getLogger().info("Spawn world: " + spawnWorldName + " | Particle interval: " + particleTaskInterval + " ticks | Max particles/tick: " + maxParticlesPerTick);

        dataManager = new DataManager(getDataFolder(), getLogger());
        economyManager = new EconomyManager(dataManager, getLogger());
        economyManager.setupEconomy();
        
        abilityScanner = new AbilityScannerManager(dataManager, getLogger());
        Bukkit.getScheduler().runTaskLater(this, () -> abilityScanner.scanAndLoadAbilities(), 100L);

        cosmeticsManager = new CosmeticsManager(this, dataManager, getLogger());
        cosmeticsManager.loadPlayerCosmetics();
        cosmeticsManager.initializeShop();
        cosmeticsManager.startCosmeticEffects(particleTaskInterval, maxParticlesPerTick);

        guiManager = new GuiManager(this, economyManager, abilityScanner, cosmeticsManager);

        getServer().getPluginManager().registerEvents(new GuiListener(this, guiManager, economyManager, abilityScanner, cosmeticsManager, permissionCommand), this);
        getServer().getPluginManager().registerEvents(new PlayerActivityListener(this, economyManager, cosmeticsManager, afkTimeoutMs, passivePointsActive, passivePointsAfk), this);

        if (getCommand("st") != null) {
            getCommand("st").setExecutor(new SkillsTreeCommand(guiManager));
        }

        startAsyncPeriodicSave();

        getLogger().info("SkillsTree v3.0 Enabled (Refactored)");
    }

    @Override
    public void onDisable() {
        cosmeticsManager.prepareConfigSave();
        dataManager.saveCosmeticsConfigSync();
        if (dataManager.isPointsDirty()) {
            dataManager.savePointsConfigSync();
            getLogger().info("Final points save completed.");
        }
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public boolean isInSpawnWorld(Player player) {
        if (player == null) return false;
        World w = player.getWorld();
        return w != null && w.getName().equalsIgnoreCase(spawnWorldName);
    }

    public int getUnlockedCount(UUID uuid) {
        return unlockedCounts.getOrDefault(uuid, 0);
    }

    public void recalculateUnlockedCount(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        int count = 0;
        synchronized (dataManager.configLock) {
            if (dataManager.getAbilitiesConfig().getConfigurationSection("Abilities") != null) {
                for (String key : dataManager.getAbilitiesConfig().getConfigurationSection("Abilities").getKeys(false)) {
                    if (p.hasPermission("bending.ability." + key)) {
                        count++;
                    }
                }
            }
        }
        unlockedCounts.put(uuid, count);
    }

    public void removeUnlockedCount(UUID uuid) {
        unlockedCounts.remove(uuid);
    }

    private void startAsyncPeriodicSave() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            boolean pDirty = dataManager.isPointsDirty();
            boolean cDirty = dataManager.isCosmeticsDirty();
            if (cDirty) {
                cosmeticsManager.prepareConfigSave();
            }
            if (pDirty || cDirty) {
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    if (pDirty) {
                        dataManager.savePointsConfigAsync();
                        dataManager.setPointsDirty(false);
                    }
                    if (cDirty) {
                        dataManager.saveCosmeticsConfigAsync();
                        dataManager.setCosmeticsDirty(false);
                    }
                });
            }
        }, 1200L, 1200L);
    }
}
