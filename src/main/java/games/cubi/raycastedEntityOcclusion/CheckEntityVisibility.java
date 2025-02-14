package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CheckEntityVisibility {
    RaycastedEntityOcclusion plugin;
    private int alwaysShowRadius;
    private int raycastRadius;
    private int searchRadius;
    private int engineMode;
    private boolean cullPlayers;
    private boolean sneakCull;
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
        this.engineMode = plugin.engineMode;
        this.cullPlayers = plugin.cullPlayers;
        this.sneakCull = plugin.sneakCull;
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

    public void updateConfigValues() {
        this.alwaysShowRadius = plugin.alwaysShowRadius;
        this.raycastRadius = plugin.raycastRadius;
        this.searchRadius = plugin.searchRadius;
        this.engineMode = plugin.engineMode;
        this.cullPlayers = plugin.cullPlayers;
        this.sneakCull = plugin.sneakCull;
        this.recheckInterval = plugin.recheckInterval;
    }

    public void runVisibilityCheck(Player player) {
        World world = player.getWorld();
        List<Entity> nearbyEntities = player.getNearbyEntities(searchRadius, searchRadius, searchRadius);

        for (Entity entity : nearbyEntities) {
            if (world != player.getWorld()) {
                return;
            }
            if (entity instanceof Player && !cullPlayers) {
                continue;
            }
            if (entity instanceof Player p && sneakCull && !p.isSneaking()) {
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
            boolean canSee = player.canSee(entity);
            int check = tickCounter % recheckInterval;
            //plugin.getLogger().info("check: " + check + " cansee " + canSee);
            if (!canSee || check == 0) {
                runCheck(engineMode, player, entity, plugin);
            }
        }
    }




    public void addEntityToShow(Player player, Entity entity) {
        entitiesToShow.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(entity);
        entitiesToHide.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).remove(entity);
    }

    public void addEntityToHide(Player player, Entity entity) {
        entitiesToHide.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(entity);
        entitiesToShow.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).remove(entity);
    }


    private void applyVisibilityChanges() {
        for (Map.Entry<Player, Set<Entity>> entry : entitiesToShow.entrySet()) {
            Player player = entry.getKey();
            for (Entity entity : entry.getValue()) {
                if (entity instanceof Player p) {
                    player.showPlayer(plugin, p);
                } else player.showEntity(plugin, entity);
            }
        }
        for (Map.Entry<Player, Set<Entity>> entry : entitiesToHide.entrySet()) {
            Player player = entry.getKey();
            for (Entity entity : entry.getValue()) {
                if (entity instanceof Player p) {
                    player.hidePlayer(plugin, p);
                } else player.hideEntity(plugin, entity);
            }
        }
    }

    public static void runCheck(int engineMode, Player player, Entity entity, RaycastedEntityOcclusion plugin){
        switch (engineMode){
            case 1:
                engineModeOne(player, entity, plugin);
                break;
            case 2:
                engineModeTwo(player, entity, plugin);
                break;
            case 3:
                engineModeThree(player, entity, plugin);
                break;
            case 4:
                engineModeFour(player, entity, plugin);
                break;
        }
    }

    public static void engineModeOne /*Basic eye-to-eye check*/(Player player, Entity entity, RaycastedEntityOcclusion plugin) {
        Location entityLocation = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        Raycast.asyncRaycast(player.getEyeLocation(), entityLocation, plugin, canSeePlayer -> {
            if (canSeePlayer) {
                plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
            } else {
                plugin.getCheckEntityVisibility().addEntityToHide(player, entity);
            }
        });
    }

    public static void engineModeTwo /*Intensive four corners check*/ (Player player, Entity entity, RaycastedEntityOcclusion plugin) {
        double height = entity.getHeight();
        double width = entity.getWidth();
        Location checkOne = entity.getLocation().add(0, height - 0.01, 0);
        Location checkTwo = entity.getLocation().add(0, 0.01, 0);
        Location checkThree = entity.getLocation().add(width / 2, height / 2, 0);
        Location checkFour = entity.getLocation().add(-width / 2, height / 2, 0);

        Raycast.asyncRaycast(player.getEyeLocation(), checkOne, plugin, canSeePlayer -> {
            if (canSeePlayer) {
                plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
            } else {
                Raycast.asyncRaycast(player.getEyeLocation(), checkTwo, plugin, canSeePlayer2 -> {
                    if (canSeePlayer2) plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                    else {
                        Raycast.asyncRaycast(player.getEyeLocation(), checkThree, plugin, canSeePlayer3 -> {
                            if (canSeePlayer3) plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                            else {
                                Raycast.asyncRaycast(player.getEyeLocation(), checkFour, plugin, canSeePlayer4 -> {
                                    if (canSeePlayer4) plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                                    else plugin.getCheckEntityVisibility().addEntityToHide(player, entity);
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    public static void engineModeThree /*Doubled eye-to-eye check with one block penetration*/(Player player, Entity entity, RaycastedEntityOcclusion plugin) {
        Location entityLocation = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        Raycast.asyncRaycastWithLocationReturn(player.getEyeLocation(), entityLocation, plugin, location -> {
            if (location == null) {
                plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
            } else {
                //Debug particle at location
                player.getWorld().spawnParticle(org.bukkit.Particle.DUST, location, 10);
                // Run a new raycast from 1.5 blocks closer to player than location currently is
                Location playerEye = player.getEyeLocation();
                Vector direction = playerEye.toVector().subtract(location.toVector());

                // Avoid normalizing a zero-length vector
                if (direction.lengthSquared() > 0.01) { // Small threshold to prevent precision errors
                    direction.normalize().multiply(1.5); // Scale to 1.5 blocks
                    location.add(direction); // Move the location
                }
                //Debug particle at location
                player.getWorld().spawnParticle(org.bukkit.Particle.DUST, location, 10);
                Raycast.asyncRaycast(playerEye, location, plugin, canSeePlayer -> {
                    if (canSeePlayer) {
                        plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                    } else {
                        plugin.getCheckEntityVisibility().addEntityToHide(player, entity);
                    }
                });

            }
        });
    }



    public interface ModeThreeCallback {
        void onResult(boolean canSeePlayer);
    }


    public static void engineModeThreeAsyncReturn /*Doubled eye-to-eye check with one block penetration*/(Player player, Entity entity, RaycastedEntityOcclusion plugin, Location entityLocation, ModeThreeCallback callback) {
        Raycast.asyncRaycastWithLocationReturn(player.getEyeLocation(), entityLocation, plugin, location -> {
            if (location == null) {
                callback.onResult(true);
            } else {
                //Debug particle at location
                player.getWorld().spawnParticle(org.bukkit.Particle.DUST, location, 10);
                // Run a new raycast from 1.5 blocks closer to player than location currently is
                Location playerEye = player.getEyeLocation();
                Vector direction = playerEye.toVector().subtract(location.toVector());

                // Avoid normalizing a zero-length vector
                if (direction.lengthSquared() > 0.01) { // Small threshold to prevent precision errors
                    direction.normalize().multiply(1.5); // Scale to 1.5 blocks
                    location.add(direction); // Move the location
                }
                //Debug particle at location
                player.getWorld().spawnParticle(org.bukkit.Particle.DUST, location, 10);
                Raycast.asyncRaycast(playerEye, location, plugin, callback::onResult);

            }
        });
    }

    public static void engineModeFour /*Doubled four point check with one block penetration*/(Player player, Entity entity, RaycastedEntityOcclusion plugin){
        double height = entity.getHeight();
        double width = entity.getWidth();
        Location checkOne = entity.getLocation().add(0, height - 0.01, 0);
        Location checkTwo = entity.getLocation().add(0, 0.01, 0);
        Location checkThree = entity.getLocation().add(width / 2, height / 2, 0);
        Location checkFour = entity.getLocation().add(-width / 2, height / 2, 0);

        engineModeThreeAsyncReturn(player, entity, plugin, checkOne, canSeePlayer -> {
            if (canSeePlayer) {
                plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
            } else {
                engineModeThreeAsyncReturn(player, entity, plugin, checkTwo, canSeePlayer2 -> {
                    if (canSeePlayer2) plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                    else {
                        engineModeThreeAsyncReturn(player, entity, plugin, checkThree, canSeePlayer3 -> {
                            if (canSeePlayer3) plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                            else {
                                engineModeThreeAsyncReturn(player, entity, plugin, checkFour, canSeePlayer4 -> {
                                    if (canSeePlayer4) plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                                    else plugin.getCheckEntityVisibility().addEntityToHide(player, entity);
                                });
                            }
                        });
                    }
                });
            }
        });
    }
}