package me.furthyskills.skillstree.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages economy transactions and point balances for players.
 * Supports both Vault economy and a fallback local point system.
 */
public class EconomyManager {
    private Economy econ = null;
    private final DataManager dataManager;
    private final Logger logger;

    /**
     * Constructs an EconomyManager.
     *
     * @param dataManager The data manager for persistence.
     * @param logger      The plugin logger.
     */
    public EconomyManager(DataManager dataManager, Logger logger) {
        this.dataManager = dataManager;
        this.logger = logger;
    }

    public boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        logger.info("Vault economy hooked: " + (econ != null));
        return econ != null;
    }

    public int getPoints(UUID uuid) {
        if (econ != null) {
            return (int) econ.getBalance(Bukkit.getOfflinePlayer(uuid));
        }
        synchronized (dataManager.configLock) {
            return dataManager.getPointsConfig().getInt("Players." + uuid + ".points", 0);
        }
    }

    public boolean has(UUID uuid, int amount) {
        if (econ != null) {
            return econ.has(Bukkit.getOfflinePlayer(uuid), amount);
        }
        return getPoints(uuid) >= amount;
    }

    public void withdraw(UUID uuid, int amount) {
        if (econ != null) {
            econ.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), amount);
            return;
        }
        synchronized (dataManager.configLock) {
            int current = dataManager.getPointsConfig().getInt("Players." + uuid + ".points", 0);
            dataManager.getPointsConfig().set("Players." + uuid + ".points", Math.max(0, current - amount));
        }
        dataManager.setPointsDirty(true);
    }

    public void addPoints(UUID uuid, int amount) {
        if (econ != null) {
            econ.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
            return;
        }
        synchronized (dataManager.configLock) {
            int current = dataManager.getPointsConfig().getInt("Players." + uuid + ".points", 0);
            dataManager.getPointsConfig().set("Players." + uuid + ".points", current + amount);
        }
        dataManager.setPointsDirty(true);
    }
}
