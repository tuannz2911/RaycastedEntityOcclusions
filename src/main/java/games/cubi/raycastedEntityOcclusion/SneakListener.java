package games.cubi.raycastedEntityOcclusion;

import jdk.jfr.Label;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class SneakListener implements Listener {
    RaycastedEntityOcclusion plugin;
    public SneakListener(RaycastedEntityOcclusion plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        if (plugin.sneakCull) {
            Player sneakingPlayer = event.getPlayer();
            int searchRadius = plugin.searchRadius;
            List<Player> nearbyPlayers = sneakingPlayer.getNearbyEntities(searchRadius, searchRadius, searchRadius).stream()
                    .filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity)
                    .toList();
            for (Player nearbyPlayer : nearbyPlayers) {
                if (nearbyPlayer == sneakingPlayer) continue;
                CheckEntityVisibility.runCheck(plugin.engineMode, sneakingPlayer, nearbyPlayer, plugin);
            }
        }
    }
}
