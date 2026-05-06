package me.furthyskills.skillstree.gui;

import me.furthyskills.skillstree.SkillsTreePlugin;
import me.furthyskills.skillstree.managers.AbilityScannerManager;
import me.furthyskills.skillstree.managers.CosmeticsManager;
import me.furthyskills.skillstree.managers.EconomyManager;
import me.furthyskills.skillstree.models.ShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.regex.Pattern;

public class GuiListener implements Listener {
    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final SkillsTreePlugin plugin;
    private final GuiManager guiManager;
    private final EconomyManager economyManager;
    private final AbilityScannerManager abilityScanner;
    private final CosmeticsManager cosmeticsManager;
    private final String permissionCommand;

    public GuiListener(SkillsTreePlugin plugin, GuiManager guiManager, EconomyManager economyManager, AbilityScannerManager abilityScanner, CosmeticsManager cosmeticsManager, String permissionCommand) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.economyManager = economyManager;
        this.abilityScanner = abilityScanner;
        this.cosmeticsManager = cosmeticsManager;
        this.permissionCommand = permissionCommand;
    }

    private boolean isSafeAbilityId(String id) {
        return id != null && SAFE_ID_PATTERN.matcher(id).matches();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String guiType = meta.getPersistentDataContainer().get(guiManager.GUI_TYPE_KEY, PersistentDataType.STRING);
        if (guiType == null) return;

        // Valid GUI element found
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);

        switch (guiType) {
            case "category": {
                String el = meta.getPersistentDataContainer().get(guiManager.GUI_ELEMENT_KEY, PersistentDataType.STRING);
                if (el != null) guiManager.openCategoryMenu(p, el, 0);
                break;
            }
            case "ability": {
                String ability = meta.getPersistentDataContainer().get(guiManager.ABILITY_ID_KEY, PersistentDataType.STRING);
                String element = meta.getPersistentDataContainer().get(guiManager.GUI_ELEMENT_KEY, PersistentDataType.STRING);
                Integer pg = meta.getPersistentDataContainer().get(guiManager.GUI_PAGE_KEY, PersistentDataType.INTEGER);
                int page = (pg != null) ? pg : 0;
                
                if (ability == null || !isSafeAbilityId(ability)) break;
                
                if (p.hasPermission("bending.ability." + ability.toLowerCase())) {
                    p.sendMessage(Component.text("You already have " + ability + "!", NamedTextColor.YELLOW));
                } else {
                    int cost = abilityScanner.getAbilityCost(ability);
                    if (economyManager.has(p.getUniqueId(), cost)) {
                        economyManager.withdraw(p.getUniqueId(), cost);
                        String safePlayer = p.getName().replaceAll("[^a-zA-Z0-9_]", "");
                        String cmd = permissionCommand.replace("{player}", safePlayer).replace("{ability}", ability);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        p.sendMessage(Component.text("✔ Unlocked " + ability + "!", NamedTextColor.GREEN));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
                        plugin.recalculateUnlockedCount(p.getUniqueId());
                    } else {
                        p.sendMessage(Component.text("Not enough money! Need $" + cost, NamedTextColor.RED));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
                    }
                }
                if (element != null) guiManager.openCategoryMenu(p, element, page);
                break;
            }
            case "shop_main": {
                guiManager.openShopMenu(p, 0);
                break;
            }
            case "shop_item": {
                String shopId = meta.getPersistentDataContainer().get(guiManager.ABILITY_ID_KEY, PersistentDataType.STRING);
                Integer pg = meta.getPersistentDataContainer().get(guiManager.GUI_PAGE_KEY, PersistentDataType.INTEGER);
                int page = (pg != null) ? pg : 0;
                
                if (shopId != null) {
                    ShopItem item = cosmeticsManager.getShopItems().get(shopId);
                    if (item == null) break;
                    if (!cosmeticsManager.isOwned(p.getUniqueId(), shopId) && item.cost >= 1000) {
                        guiManager.openConfirmMenu(p, shopId, item.cost, page, item);
                    } else {
                        handleShopPurchase(p, shopId, page);
                    }
                }
                break;
            }
            case "shop_confirm_yes": {
                String shopId = meta.getPersistentDataContainer().get(guiManager.ABILITY_ID_KEY, PersistentDataType.STRING);
                Integer pg = meta.getPersistentDataContainer().get(guiManager.GUI_PAGE_KEY, PersistentDataType.INTEGER);
                int page = (pg != null) ? pg : 0;
                if (shopId != null) handleShopPurchase(p, shopId, page);
                break;
            }
            case "shop_confirm_no": {
                Integer pg = meta.getPersistentDataContainer().get(guiManager.GUI_PAGE_KEY, PersistentDataType.INTEGER);
                int page = (pg != null) ? pg : 0;
                guiManager.openShopMenu(p, page);
                break;
            }
            case "back_to_main": {
                guiManager.openMainMenu(p);
                break;
            }
            case "unequip_all": {
                UUID uuid = p.getUniqueId();
                cosmeticsManager.unequipAll(uuid);
                cosmeticsManager.updatePlayerTitle(p);
                p.sendMessage(Component.text("Unequipped all cosmetics.", NamedTextColor.YELLOW));
                guiManager.openShopMenu(p, 0);
                break;
            }
            case "page_prev_category":
            case "page_next_category": {
                String el = meta.getPersistentDataContainer().get(guiManager.GUI_ELEMENT_KEY, PersistentDataType.STRING);
                Integer pg = meta.getPersistentDataContainer().get(guiManager.GUI_PAGE_KEY, PersistentDataType.INTEGER);
                if (el != null && pg != null) guiManager.openCategoryMenu(p, el, pg);
                break;
            }
            case "page_prev_shop":
            case "page_next_shop": {
                Integer pg = meta.getPersistentDataContainer().get(guiManager.GUI_PAGE_KEY, PersistentDataType.INTEGER);
                if (pg != null) guiManager.openShopMenu(p, pg);
                break;
            }
        }
    }

    private void handleShopPurchase(Player p, String shopId, int page) {
        UUID uuid = p.getUniqueId();
        ShopItem item = cosmeticsManager.getShopItems().get(shopId);
        if (item == null) return;
        
        if (cosmeticsManager.isOwned(uuid, shopId)) {
            // Toggle equip/unequip
            cosmeticsManager.toggleEquip(uuid, item);
            if (item.type.equals("title")) {
                cosmeticsManager.updatePlayerTitle(p);
            }
            p.sendMessage(Component.text("Toggled " + item.displayName, NamedTextColor.GREEN));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.5f);
        } else {
            // Purchase
            if (economyManager.has(uuid, item.cost)) {
                economyManager.withdraw(uuid, item.cost);
                cosmeticsManager.addOwned(uuid, shopId);
                p.sendMessage(Component.text("✔ Purchased " + item.displayName + "!", NamedTextColor.GREEN));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
            } else {
                p.sendMessage(Component.text("Not enough money! Need $" + item.cost, NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
            }
        }
        guiManager.openShopMenu(p, page);
    }
}
