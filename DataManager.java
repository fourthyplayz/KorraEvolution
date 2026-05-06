package me.furthyskills.skillstree.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public class DataManager {
    private final File dataFolder;
    private final Logger logger;

    private File abilitiesFile;
    private volatile FileConfiguration abilitiesConfig;

    private File pointsFile;
    private volatile FileConfiguration pointsConfig;

    private File cosmeticsFile;
    private volatile FileConfiguration cosmeticsConfig;

    public final Object configLock = new Object();
    private final Object pointsIoLock = new Object();
    private final Object cosmeticsIoLock = new Object();
    private volatile boolean pointsDirty = false;
    private volatile boolean cosmeticsDirty = false;

    public boolean isCosmeticsDirty() {
        return cosmeticsDirty;
    }

    public void setCosmeticsDirty(boolean dirty) {
        this.cosmeticsDirty = dirty;
    }

    public DataManager(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;

        createAbilitiesConfig();
        createPointsConfig();
        createCosmeticsConfig();
    }

    private void createAbilitiesConfig() {
        abilitiesFile = new File(dataFolder, "abilities.yml");
        if (!abilitiesFile.exists()) {
            abilitiesFile.getParentFile().mkdirs();
            try {
                abilitiesFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Failed to create abilities.yml: " + e.getMessage());
            }
        }
        abilitiesConfig = YamlConfiguration.loadConfiguration(abilitiesFile);
    }

    public void saveAbilitiesConfig() {
        try {
            synchronized (configLock) {
                abilitiesConfig.save(abilitiesFile);
            }
        } catch (IOException e) {
            logger.severe("Failed to save abilities.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getAbilitiesConfig() {
        return abilitiesConfig;
    }

    private void createPointsConfig() {
        pointsFile = new File(dataFolder, "playerpoints.yml");
        if (!pointsFile.exists()) {
            try {
                pointsFile.getParentFile().mkdirs();
                pointsFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Failed to create playerpoints.yml: " + e.getMessage());
            }
        }
        pointsConfig = YamlConfiguration.loadConfiguration(pointsFile);
    }

    public void savePointsConfigSync() {
        try {
            String yamlContent;
            synchronized (configLock) {
                yamlContent = pointsConfig.saveToString();
            }
            synchronized (pointsIoLock) {
                Files.write(pointsFile.toPath(), yamlContent.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.severe("Failed to save playerpoints.yml: " + e.getMessage());
        }
    }

    public void savePointsConfigAsync() {
        try {
            File tempFile = new File(pointsFile.getParentFile(), "playerpoints.yml.tmp");
            String yamlContent;
            synchronized (configLock) {
                yamlContent = pointsConfig.saveToString();
            }
            synchronized (pointsIoLock) {
                Files.write(tempFile.toPath(), yamlContent.getBytes(StandardCharsets.UTF_8));
                Files.move(tempFile.toPath(), pointsFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            logger.fine("Async saved player points.");
        } catch (IOException e) {
            logger.severe("Failed to async save playerpoints.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getPointsConfig() {
        return pointsConfig;
    }

    public boolean isPointsDirty() {
        return pointsDirty;
    }

    public void setPointsDirty(boolean dirty) {
        this.pointsDirty = dirty;
    }

    private void createCosmeticsConfig() {
        cosmeticsFile = new File(dataFolder, "cosmetics.yml");
        if (!cosmeticsFile.exists()) {
            try {
                cosmeticsFile.getParentFile().mkdirs();
                cosmeticsFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Failed to create cosmetics.yml: " + e.getMessage());
            }
        }
        cosmeticsConfig = YamlConfiguration.loadConfiguration(cosmeticsFile);
    }

    public FileConfiguration getCosmeticsConfig() {
        return cosmeticsConfig;
    }

    public void saveCosmeticsConfigSync() {
        try {
            String yamlContent;
            synchronized (configLock) {
                yamlContent = cosmeticsConfig.saveToString();
            }
            synchronized (cosmeticsIoLock) {
                Files.write(cosmeticsFile.toPath(), yamlContent.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.severe("Failed to save cosmetics.yml: " + e.getMessage());
        }
    }

    public void saveCosmeticsConfigAsync() {
        try {
            File tempFile = new File(cosmeticsFile.getParentFile(), "cosmetics.yml.tmp");
            String yamlContent;
            synchronized (configLock) {
                yamlContent = cosmeticsConfig.saveToString();
            }
            synchronized (cosmeticsIoLock) {
                Files.write(tempFile.toPath(), yamlContent.getBytes(StandardCharsets.UTF_8));
                Files.move(tempFile.toPath(), cosmeticsFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            logger.fine("Async saved cosmetics.");
        } catch (IOException e) {
            logger.severe("Failed to async save cosmetics.yml: " + e.getMessage());
        }
    }
}
