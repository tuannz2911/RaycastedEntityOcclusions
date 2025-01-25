package games.cubi.raycastedEntityOcclusion;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    RaycastedEntityOcclusion plugin;

    public CommandHandler(RaycastedEntityOcclusion plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("RaycastedEntityOcclusion v" + plugin.getDescription().getVersion());

        return false;
    }
}
