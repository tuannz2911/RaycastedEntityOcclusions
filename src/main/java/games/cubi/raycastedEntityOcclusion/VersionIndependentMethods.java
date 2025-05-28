package games.cubi.raycastedEntityOcclusion;

import org.bukkit.entity.Player;

public class VersionIndependentMethods {
    RaycastedEntityOcclusion plugin;
    ConfigManager cfg;
    String version;
    public VersionIndependentMethods(RaycastedEntityOcclusion plugin, ConfigManager cfg, String version) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.version = version;
    }
    public static void showTabName(Player p, Player other) {
        // This method is a placeholder for the actual implementation.
        // The original code uses PacketEvents to show the tab name.
        // You can implement the logic here based on your requirements.
    }
}
