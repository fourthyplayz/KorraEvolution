package me.furthyskills.skillstree.commands;

import me.furthyskills.skillstree.gui.GuiManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillsTreeCommand implements CommandExecutor {
    private final GuiManager guiManager;

    public SkillsTreeCommand(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("skillstree.use")) {
            p.sendMessage(Component.text("You do not have permission to access the Skills Tree.", NamedTextColor.RED));
            return true;
        }
        guiManager.openMainMenu(p);
        return true;
    }
}
