package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RaycastedEntityOcclusion extends JavaPlugin {

    public int alwaysShowRadius;
    public int raycastRadius;
    public int searchRadius;
    public int tickCounter = 0;
    public RaycastedEntityOcclusion plugin = this;

    // Use ConcurrentHashMap for thread-safe access
    private final Map<Player, Set<Entity>> entitiesToShow = new ConcurrentHashMap<>();
    private final Map<Player, Set<Entity>> entitiesToHide = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        loadConfig();

        new BukkitRunnable() {
            @Override
            public void run() {
                // Clear the maps at the start of each tick
                entitiesToShow.clear();
                entitiesToHide.clear();

                // Check visibility for all players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkEntityVisibility(player);
                }

                // Apply visibility changes at the end of the tick
                applyVisibilityChanges();

                tickCounter++;
                if (tickCounter > 1000000) {
                    tickCounter = 0;
                }
            }
        }.runTaskTimer(this, 0L, 1L); // Run every tick (20 times per second)
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void checkEntityVisibility(Player player) {
        List<Entity> nearbyEntities = player.getNearbyEntities(searchRadius, searchRadius, searchRadius);

        for (Entity entity : nearbyEntities) {
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
            if (!player.canSee(entity) || tickCounter % 10 == 0) {
                // Perform raycast to check visibility
                Location entityLocation = entity.getLocation().add(0, entity.getHeight() / 2, 0);
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

    private void loadConfig() {
        // Ensure the config file exists
        saveDefaultConfig();

        // Load values from config
        alwaysShowRadius = getConfig().getInt("AlwaysShowRadius", 4);
        raycastRadius = getConfig().getInt("RaycastRadius", 64);
        searchRadius = getConfig().getInt("SearchRadius", 72);

        // Log loaded values
        getLogger().info("AlwaysShowRadius: " + alwaysShowRadius);
        getLogger().info("RaycastRadius: " + raycastRadius);
        getLogger().info("SearchRadius: " + searchRadius);
    }
}