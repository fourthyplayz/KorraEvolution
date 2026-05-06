package me.furthyskills.skillstree.managers;

import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AbilityScannerManager {
    private final DataManager dataManager;
    private final Logger logger;

    private final Map<String, List<String>> sortedAbilitiesCache = new HashMap<>();

    public AbilityScannerManager(DataManager dataManager, Logger logger) {
        this.dataManager = dataManager;
        this.logger = logger;
    }

    public void scanAndLoadAbilities() {
        invalidateAbilityCache();
        try {
            FileConfiguration abilitiesConfig = dataManager.getAbilitiesConfig();
            for (CoreAbility ability : CoreAbility.getAbilities()) {
                if (ability.isHiddenAbility()) continue;
                String element = ability.getElement().getName();
                String name = ability.getName();
                sortedAbilitiesCache.computeIfAbsent(element, k -> new ArrayList<>()).add(name);
                
                // Also persist in abilities.yml for reference
                synchronized (dataManager.configLock) {
                    if (!abilitiesConfig.contains("Abilities." + name)) {
                        abilitiesConfig.set("Abilities." + name + ".element", element);
                        abilitiesConfig.set("Abilities." + name + ".cost", 1000);
                    }
                }
            }
            dataManager.saveAbilitiesConfig();
            
            // Sort each category alphabetically
            for (List<String> list : sortedAbilitiesCache.values()) {
                Collections.sort(list);
            }
            logger.info("Auto-scanned " + sortedAbilitiesCache.values().stream().mapToInt(List::size).sum() + " abilities across " + sortedAbilitiesCache.size() + " elements.");
        } catch (Exception e) {
            logger.warning("Could not auto-scan CoreAbilities: " + e.getMessage());
        }
    }

    public void invalidateAbilityCache() {
        sortedAbilitiesCache.clear();
    }

    public Map<String, List<String>> getSortedAbilitiesCache() {
        return sortedAbilitiesCache;
    }

    public int getAbilityCost(String abilityName) {
        synchronized (dataManager.configLock) {
            return dataManager.getAbilitiesConfig().getInt("Abilities." + abilityName + ".cost", 1000);
        }
    }
}
