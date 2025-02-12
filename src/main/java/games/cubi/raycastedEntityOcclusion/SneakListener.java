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
            Player player = event.getPlayer();
            int searchRadius = plugin.searchRadius;
            List<Player> nearbyPlayers = player.getNearbyEntities(searchRadius, searchRadius, searchRadius).stream()
                    .filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity)
                    .toList();
            for (Player nearbyPlayer : nearbyPlayers) {
                if (plugin.moreChecks){
                    double height = nearbyPlayer.getHeight();
                    double width = player.getWidth();
                    Location checkOne = player.getLocation().add(0, height - 0.01, 0);
                    Location checkTwo = player.getLocation().add(0, 0.01, 0);
                    Location checkThree = player.getLocation().add(width / 2, height / 2, 0);
                    Location checkFour = player.getLocation().add(-width / 2, height / 2, 0);
                    CheckEntityVisibility checkEntityVisibility = plugin.getCheckEntityVisibility();
                    Raycast.asyncRaycast(nearbyPlayer.getEyeLocation(), checkOne, plugin, canSeePlayer -> {
                        if (canSeePlayer) {
                            checkEntityVisibility.addEntityToShow(nearbyPlayer, player);
                        } else {
                            Raycast.asyncRaycast(player.getEyeLocation(), checkTwo, plugin, canSeePlayer2 -> {
                                if (canSeePlayer2) checkEntityVisibility.addEntityToShow(nearbyPlayer, player);
                                else {
                                    Raycast.asyncRaycast(player.getEyeLocation(), checkThree, plugin, canSeePlayer3 -> {
                                        if (canSeePlayer3) checkEntityVisibility.addEntityToShow(nearbyPlayer, player);
                                        else {
                                            Raycast.asyncRaycast(player.getEyeLocation(), checkFour, plugin, canSeePlayer4 -> {
                                                if (canSeePlayer4) checkEntityVisibility.addEntityToShow(nearbyPlayer, player);
                                                else checkEntityVisibility.addEntityToHide(nearbyPlayer, player);
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
                else {
                    Raycast.asyncRaycast(nearbyPlayer.getEyeLocation(), player.getEyeLocation(), plugin, canSeePlayer -> {
                        if (canSeePlayer) {
                            plugin.checkEntityVisibility.addEntityToShow(nearbyPlayer, player);
                        } else {
                            plugin.checkEntityVisibility.addEntityToHide(nearbyPlayer, player);
                        }
                    });
                }
            }
        }
    }
}
