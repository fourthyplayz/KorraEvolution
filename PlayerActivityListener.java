package me.furthyskills.skillstree.listeners;

import me.furthyskills.skillstree.SkillsTreePlugin;
import me.furthyskills.skillstree.managers.CosmeticsManager;
import me.furthyskills.skillstree.managers.EconomyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerActivityListener implements Listener {
    private final SkillsTreePlugin plugin;
    private final EconomyManager economyManager;
    private final CosmeticsManager cosmeticsManager;

    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivityTime = new ConcurrentHashMap<>();

    private final long afkTimeoutMs;
    private final int passivePointsActive;
    private final int passivePointsAfk;

    public PlayerActivityListener(SkillsTreePlugin plugin, EconomyManager economyManager, CosmeticsManager cosmeticsManager, long afkTimeoutMs, int passivePointsActive, int passivePointsAfk) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.cosmeticsManager = cosmeticsManager;
        this.afkTimeoutMs = afkTimeoutMs;
        this.passivePointsActive = passivePointsActive;
        this.passivePointsAfk = passivePointsAfk;
        
        startPassivePointEarner();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            lastActivityTime.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer() == null) return;
        lastActivityTime.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    private boolean isPlayerActive(UUID uuid) {
        long lastActivity = lastActivityTime.getOrDefault(uuid, System.currentTimeMillis());
        return (System.currentTimeMillis() - lastActivity) < afkTimeoutMs;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        if (!plugin.isInSpawnWorld(player)) return;

        lastActivityTime.put(uuid, System.currentTimeMillis());
        plugin.recalculateUnlockedCount(uuid);
        cosmeticsManager.updatePlayerTitle(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            String lastLoginStr;
            int currentStreak;
            
            synchronized (plugin.getDataManager().configLock) {
                lastLoginStr = plugin.getDataManager().getPointsConfig().getString("Players." + uuid + ".lastLoginDate", "");
                currentStreak = plugin.getDataManager().getPointsConfig().getInt("Players." + uuid + ".streak", 0);
            }

            LocalDate lastLoginDate = null;
            if (!lastLoginStr.isEmpty()) {
                try {
                    lastLoginDate = LocalDate.parse(lastLoginStr);
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid date format for player " + uuid + ", resetting streak.");
                }
            }

            if (lastLoginDate == null) {
                currentStreak = 1;
            } else if (lastLoginDate.equals(today)) {
                return; // already claimed today
            } else if (lastLoginDate.plusDays(1).equals(today)) {
                currentStreak++;
            } else {
                currentStreak = 1;
            }

            int reward = Math.min(currentStreak * 50, 500);
            int finalStreak = currentStreak;

            synchronized (plugin.getDataManager().configLock) {
                plugin.getDataManager().getPointsConfig().set("Players." + uuid + ".lastLoginDate", today.toString());
                plugin.getDataManager().getPointsConfig().set("Players." + uuid + ".streak", finalStreak);
            }
            plugin.getDataManager().setPointsDirty(true);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                economyManager.addPoints(uuid, reward);
                player.sendMessage(Component.text("Daily Login! ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("+" + reward + " points | Streak: " + finalStreak + " days", NamedTextColor.YELLOW)));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastLocations.remove(uuid);
        lastActivityTime.remove(uuid);
        plugin.removeUnlockedCount(uuid);
    }

    private void startPassivePointEarner() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p == null || !p.isOnline()) continue;
                if (!plugin.isInSpawnWorld(p)) continue;

                UUID uuid = p.getUniqueId();
                Location currentLoc = p.getLocation();

                boolean isActive = isPlayerActive(uuid);
                lastLocations.put(uuid, currentLoc.clone());

                int baseAmount = isActive ? passivePointsActive : passivePointsAfk;
                int finalAmount = isActive ? (int) (baseAmount * getMasteryMultiplier(uuid)) : baseAmount;

                economyManager.addPoints(uuid, finalAmount);

                Component message = isActive
                        ? Component.text("+" + finalAmount + " Skill Points", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                        : Component.text("+" + finalAmount + " Skill Points (AFK)", NamedTextColor.GRAY).decorate(TextDecoration.BOLD);

                p.sendActionBar(message);
                if (isActive) {
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1f);
                }
            }
        }, 12000L, 12000L);
    }

    private double getMasteryMultiplier(UUID uuid) {
        int unlockedCount = plugin.getUnlockedCount(uuid);
        double bonus = (unlockedCount / 5.0) * 0.05;
        return Math.min(1.30, 1.0 + bonus);
    }
}
