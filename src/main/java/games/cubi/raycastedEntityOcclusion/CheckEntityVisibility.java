package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CheckEntityVisibility {
    RaycastedEntityOcclusion plugin;
    private int alwaysShowRadius;
    private int raycastRadius;
    private int searchRadius;
    private boolean moreChecks;
    private boolean occludePlayers;
    private int recheckInterval;
    private int tickCounter = 0;
    //ConcurrentHashMap allows for Thread-safe operations. Don't use regular hashmaps
    private final Map<Player, Set<Entity>> entitiesToShow = new ConcurrentHashMap<>();
    private final Map<Player, Set<Entity>> entitiesToHide = new ConcurrentHashMap<>();

    public CheckEntityVisibility(RaycastedEntityOcclusion plugin) {
        this.plugin = plugin;
        this.alwaysShowRadius = plugin.alwaysShowRadius;
        this.raycastRadius = plugin.raycastRadius;
        this.searchRadius = plugin.searchRadius;
        this.moreChecks = plugin.moreChecks;
        this.occludePlayers = plugin.occludePlayers;
        this.recheckInterval = plugin.recheckInterval;
        new BukkitRunnable() {
            @Override
            public void run() {
                entitiesToShow.clear();
                entitiesToHide.clear();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    runVisibilityCheck(player);
                }

                applyVisibilityChanges();

                tickCounter++;
                if (tickCounter > 1000000) {
                    tickCounter = 0;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void runVisibilityCheck(Player player) {
        World world = player.getWorld();
        List<Entity> nearbyEntities = player.getNearbyEntities(searchRadius, searchRadius, searchRadius);

        for (Entity entity : nearbyEntities) {
            if (world != player.getWorld()) {
                return;
            }
            if (entity instanceof Player && !occludePlayers) {
                continue;
            }
            double distance = player.getLocation().distance(entity.getLocation());
            if (distance <= alwaysShowRadius) {
                // Always show entities within this radius
                addEntityToShow(player, entity);
                continue;
            }
            if (distance > raycastRadius) {
                // Hide entities outside the raycast radius
                addEntityToHide(player, entity);
                continue;
            }
            if (!player.canSee(entity) || plugin.tickCounter % recheckInterval == 0) {
                //check whether to run intensive or light checks
                Double height = entity.getHeight();
                if (moreChecks) {
                    Double width = entity.getWidth();
                    Location checkOne = entity.getLocation().add(0, height - 0.01, 0);
                    Location checkTwo = entity.getLocation().add(0, 0.01, 0);
                    Location checkThree = entity.getLocation().add(width / 2, height / 2, 0);
                    Location checkFour = entity.getLocation().add(-width / 2, height / 2, 0);

                    Raycast.asyncRaycast(player.getEyeLocation(), checkOne, plugin, canSeePlayer -> {
                        if (canSeePlayer) {
                            addEntityToShow(player, entity);
                        } else {
                            Raycast.asyncRaycast(player.getEyeLocation(), checkTwo, plugin, canSeePlayer2 -> {
                                if (canSeePlayer2) addEntityToShow(player, entity);
                                else {
                                    Raycast.asyncRaycast(player.getEyeLocation(), checkThree, plugin, canSeePlayer3 -> {
                                        if (canSeePlayer3) addEntityToShow(player, entity);
                                        else {
                                            Raycast.asyncRaycast(player.getEyeLocation(), checkFour, plugin, canSeePlayer4 -> {
                                                if (canSeePlayer4) addEntityToShow(player, entity);
                                                else addEntityToHide(player, entity);
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                } else {
                    Location entityLocation = entity.getLocation().add(0, height / 2, 0);

                    Raycast.asyncRaycast(player.getEyeLocation(), entityLocation, plugin, canSeePlayer -> {
                        if (canSeePlayer) {
                            addEntityToShow(player, entity);
                        } else {
                            addEntityToHide(player, entity);
                        }
                    });
                }
            }
        }
    }


    private void addEntityToShow(Player player, Entity entity) {
        entitiesToShow.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(entity);
        entitiesToHide.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).remove(entity);
    }

    private void addEntityToHide(Player player, Entity entity) {
        entitiesToHide.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(entity);
        entitiesToShow.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).remove(entity);
    }


    private void applyVisibilityChanges() {
        for (Map.Entry<Player, Set<Entity>> entry : entitiesToShow.entrySet()) {
            Player player = entry.getKey();
            for (Entity entity : entry.getValue()) {
                player.showEntity(plugin, entity);
            }
        }
        for (Map.Entry<Player, Set<Entity>> entry : entitiesToHide.entrySet()) {
            Player player = entry.getKey();
            for (Entity entity : entry.getValue()) {
                player.hideEntity(plugin, entity);
            }
        }
    }
}