package me.furthyskills.skillstree.gui;

import me.furthyskills.skillstree.SkillsTreePlugin;
import me.furthyskills.skillstree.managers.AbilityScannerManager;
import me.furthyskills.skillstree.managers.CosmeticsManager;
import me.furthyskills.skillstree.managers.EconomyManager;
import me.furthyskills.skillstree.models.ShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GuiManager {
    public final NamespacedKey ABILITY_ID_KEY;
    public final NamespacedKey GUI_TYPE_KEY;
    public final NamespacedKey GUI_ELEMENT_KEY;
    public final NamespacedKey GUI_PAGE_KEY;

    private final SkillsTreePlugin plugin;
    private final EconomyManager economyManager;
    private final AbilityScannerManager abilityScanner;
    private final CosmeticsManager cosmeticsManager;

    public GuiManager(SkillsTreePlugin plugin, EconomyManager economyManager, AbilityScannerManager abilityScanner, CosmeticsManager cosmeticsManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.abilityScanner = abilityScanner;
        this.cosmeticsManager = cosmeticsManager;

        ABILITY_ID_KEY  = new NamespacedKey(plugin, "ability_id");
        GUI_TYPE_KEY    = new NamespacedKey(plugin, "gui_type");
        GUI_ELEMENT_KEY = new NamespacedKey(plugin, "gui_element");
        GUI_PAGE_KEY    = new NamespacedKey(plugin, "gui_page");
    }

    private Material getMaterialForElement(String element) {
        if (element == null) return Material.BOOK;
        switch (element.toLowerCase()) {
            case "fire":   return Material.BLAZE_POWDER;
            case "water":  return Material.WATER_BUCKET;
            case "air":    return Material.FEATHER;
            case "earth":  return Material.GRASS_BLOCK;
            case "chi":    return Material.ARROW;
            case "avatar": return Material.BEACON;
            default:       return Material.NETHER_STAR;
        }
    }

    public void openMainMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("⚡ Skills Menu", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));

        String[] elements = {"Air", "Water", "Earth", "Fire", "Chi", "Avatar"};
        int[] slots = {10, 11, 12, 14, 15, 16};
        for (int i = 0; i < elements.length; i++) {
            String el = elements[i];
            int count = abilityScanner.getSortedAbilitiesCache().getOrDefault(el, Collections.emptyList()).size();
            ItemStack item = new ItemStack(getMaterialForElement(el));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(el + " Abilities", NamedTextColor.AQUA, TextDecoration.BOLD));
            meta.lore(List.of(Component.text(count + " abilities", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "category");
            meta.getPersistentDataContainer().set(GUI_ELEMENT_KEY, PersistentDataType.STRING, el);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        ItemStack shop = new ItemStack(Material.EMERALD);
        ItemMeta shopMeta = shop.getItemMeta();
        shopMeta.displayName(Component.text("✦ Cosmetics Shop", NamedTextColor.GOLD, TextDecoration.BOLD));
        shopMeta.lore(List.of(Component.text("Trails, Auras & Titles", NamedTextColor.GRAY)));
        shopMeta.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "shop_main");
        shop.setItemMeta(shopMeta);
        inv.setItem(22, shop);

        ItemStack bal = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balMeta = bal.getItemMeta();
        balMeta.displayName(Component.text("Balance: $" + economyManager.getPoints(p.getUniqueId()), NamedTextColor.YELLOW));
        bal.setItemMeta(balMeta);
        inv.setItem(4, bal);

        Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(inv));
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openCategoryMenu(Player p, String element, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(element + " Abilities", NamedTextColor.AQUA, TextDecoration.BOLD));
        List<String> abilities = abilityScanner.getSortedAbilitiesCache().getOrDefault(element, new ArrayList<>());

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, abilities.size());

        for (int i = start; i < end; i++) {
            String abl = abilities.get(i);
            boolean has = p.hasPermission("bending.ability." + abl.toLowerCase());
            int cost = abilityScanner.getAbilityCost(abl);
            
            ItemStack item = new ItemStack(has ? Material.LIME_DYE : getMaterialForElement(element));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(abl, has ? NamedTextColor.GREEN : NamedTextColor.RED));
            meta.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "ability");
            meta.getPersistentDataContainer().set(ABILITY_ID_KEY, PersistentDataType.STRING, abl);
            meta.getPersistentDataContainer().set(GUI_ELEMENT_KEY, PersistentDataType.STRING, element);
            meta.getPersistentDataContainer().set(GUI_PAGE_KEY, PersistentDataType.INTEGER, page);
            
            List<Component> lore = new ArrayList<>();
            lore.add(has ? Component.text("✔ Unlocked", NamedTextColor.GREEN) : Component.text("Cost: $" + cost, NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        addPaginationItems(inv, page, end < abilities.size(), element, "category");

        Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(inv));
    }

    public void openShopMenu(Player p, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("✦ Cosmetics Shop", NamedTextColor.GOLD, TextDecoration.BOLD));
        UUID uuid = p.getUniqueId();
        Set<String> owned = cosmeticsManager.getOwned(uuid);

        List<ShopItem> allItems = new ArrayList<>(cosmeticsManager.getShopItems().values());
        
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, allItems.size());

        for (int i = start; i < end; i++) {
            ShopItem si = allItems.get(i);
            boolean isOwned = owned.contains(si.id);
            boolean isEquipped = cosmeticsManager.isEquipped(uuid, si.id);
            
            ItemStack item = new ItemStack(si.icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(si.displayName));
            meta.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "shop_item");
            meta.getPersistentDataContainer().set(ABILITY_ID_KEY, PersistentDataType.STRING, si.id);
            meta.getPersistentDataContainer().set(GUI_PAGE_KEY, PersistentDataType.INTEGER, page);
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Type: " + si.type.substring(0, 1).toUpperCase() + si.type.substring(1), NamedTextColor.GRAY));
            if (isEquipped) {
                lore.add(Component.text("✔ EQUIPPED — click to unequip", NamedTextColor.GREEN));
            } else if (isOwned) {
                lore.add(Component.text("Owned — click to equip", NamedTextColor.AQUA));
            } else {
                lore.add(Component.text("Cost: $" + si.cost, NamedTextColor.YELLOW));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        // Unequip all button
        ItemStack unequip = new ItemStack(Material.BARRIER);
        ItemMeta um = unequip.getItemMeta();
        um.displayName(Component.text("Unequip All", NamedTextColor.YELLOW));
        um.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "unequip_all");
        unequip.setItemMeta(um);
        inv.setItem(49, unequip);

        addPaginationItems(inv, page, end < allItems.size(), null, "shop");

        Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(inv));
    }

    public void openConfirmMenu(Player p, String shopId, int cost, int sourcePage, ShopItem item) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Confirm Purchase?", NamedTextColor.RED, TextDecoration.BOLD));
        
        ItemStack confirm = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta cm = confirm.getItemMeta();
        cm.displayName(Component.text("✔ CONFIRM PURCHASE", NamedTextColor.GREEN));
        cm.lore(List.of(Component.text("Cost: $" + cost, NamedTextColor.YELLOW)));
        cm.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "shop_confirm_yes");
        cm.getPersistentDataContainer().set(ABILITY_ID_KEY, PersistentDataType.STRING, shopId);
        cm.getPersistentDataContainer().set(GUI_PAGE_KEY, PersistentDataType.INTEGER, sourcePage);
        confirm.setItemMeta(cm);
        
        ItemStack cancel = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cx = cancel.getItemMeta();
        cx.displayName(Component.text("✖ CANCEL", NamedTextColor.RED));
        cx.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "shop_confirm_no");
        cx.getPersistentDataContainer().set(GUI_PAGE_KEY, PersistentDataType.INTEGER, sourcePage);
        cancel.setItemMeta(cx);
        
        ItemStack info = new ItemStack(item.icon);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text(item.displayName));
        info.setItemMeta(infoMeta);
        
        inv.setItem(11, confirm);
        inv.setItem(13, info);
        inv.setItem(15, cancel);
        
        Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(inv));
    }
    
    private void addPaginationItems(Inventory inv, int page, boolean hasNext, String element, String context) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(Component.text("← Back", NamedTextColor.RED));
        bm.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "back_to_main");
        back.setItemMeta(bm);
        inv.setItem(45, back);

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.displayName(Component.text("← Prev Page", NamedTextColor.YELLOW));
            pm.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "page_prev_" + context);
            if (element != null) pm.getPersistentDataContainer().set(GUI_ELEMENT_KEY, PersistentDataType.STRING, element);
            pm.getPersistentDataContainer().set(GUI_PAGE_KEY, PersistentDataType.INTEGER, page - 1);
            prev.setItemMeta(pm);
            inv.setItem(48, prev);
        }
        
        if (hasNext) {
            ItemStack next = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.displayName(Component.text("Next Page →", NamedTextColor.YELLOW));
            nm.getPersistentDataContainer().set(GUI_TYPE_KEY, PersistentDataType.STRING, "page_next_" + context);
            if (element != null) nm.getPersistentDataContainer().set(GUI_ELEMENT_KEY, PersistentDataType.STRING, element);
            nm.getPersistentDataContainer().set(GUI_PAGE_KEY, PersistentDataType.INTEGER, page + 1);
            next.setItemMeta(nm);
            inv.setItem(50, next);
        }
    }
}
