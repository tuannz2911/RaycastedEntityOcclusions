package games.cubi.raycastedEntityOcclusion;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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

                for (Player player : Bukkit.getOnlinePlayers()) {
                    runVisibilityCheck(player);
                }

                //applyVisibilityChanges();
                renderParticles();
                tickCounter++;
                if (tickCounter > 1000000) {
                    tickCounter = 0;
                    //Prevent memory leaks
                    entitiesToShow.clear();
                    entitiesToHide.clear();
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
        List<Entity> nearbyEntitiesList = player.getNearbyEntities(searchRadius, searchRadius, searchRadius);
        Set<Entity> nearbyEntitiesSet = new HashSet<>(nearbyEntitiesList);

        for (Entity entity : nearbyEntitiesSet) {
            if (world != player.getWorld()) {
                return;
            }
            if (entity instanceof Player && !cullPlayers) {
                continue;
            }
            if (entity instanceof Player p && sneakCull && !p.isSneaking()) {
                player.showEntity(plugin, entity);
                continue;
            }
            double distance = player.getLocation().distance(entity.getLocation());
            if (distance <= alwaysShowRadius) {
                // Always show entities within this radius
                player.showEntity(plugin, entity);
                continue;
            }
            if (distance > raycastRadius) {
                // Hide entities outside the raycast radius
                player.hideEntity(plugin, entity);
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

    ConcurrentLinkedQueue<Location> particleRed = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Location> particleBlue = new ConcurrentLinkedQueue<>();
    public void renderParticles(){
        //Loop through all locations in particles and show a particle there
        for (Location loc : particleRed) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1.0F);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 1, dustOptions);
        }
        for (Location loc : particleBlue) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.BLUE, 1.0F);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 1, dustOptions);
        }
        particleRed.clear();
        particleBlue.clear();
    }

/*
    public void addEntityToShow(Player player, Entity entity) {
        entitiesToShow.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(entity);
        entitiesToHide.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).remove(entity);
    }

    public void addEntityToHide(Player player, Entity entity) {
        entitiesToHide.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(entity);
        entitiesToShow.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).remove(entity);
    }


    private void applyVisibilityChanges() {
        plugin.getLogger().info("Applying visibility changes");

        // Apply show changes
        for (Map.Entry<Player, Set<Entity>> entry : entitiesToShow.entrySet()) {
            Player player = entry.getKey();
            plugin.getLogger().info("player (showing): " + player.getName());

            Set<Entity> entities = entry.getValue();
            entities.removeIf(entity -> {
                if (entity instanceof Player p) {
                    player.showPlayer(plugin, p);
                } else {
                    player.showEntity(plugin, entity);
                    plugin.getLogger().info("showing entity: " + entity.getName());
                }
                return true; // Remove from set after processing
            });
        }

        // Apply hide changes
        for (Map.Entry<Player, Set<Entity>> entry : entitiesToHide.entrySet()) {
            Player player = entry.getKey();
            plugin.getLogger().info("player (hiding): " + player.getName());

            Set<Entity> entities = entry.getValue();
            entities.removeIf(entity -> {
                if (entity instanceof Player p) {
                    player.hidePlayer(plugin, p);
                } else {
                    player.hideEntity(plugin, entity);
                    plugin.getLogger().info("hiding entity: " + entity.getName());
                }
                return true; // Remove from set after processing
            });
        }
    }
*/

    public void runCheck(int engineMode, Player player, Entity entity, RaycastedEntityOcclusion plugin){
        //plugin.getLogger().info("Running check"+engineMode);
        engineModeOne(player, entity, plugin);
        /*
        switch (engineMode){
            case 1:
                //plugin.getLogger().info("Engine mode 1");
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
        }*/
    }

    public void engineModeOne /*Basic eye-to-eye check*/(Player player, Entity entity, RaycastedEntityOcclusion plugin) {
        //plugin.getLogger().info("Engine mode 1");
        Location entityLocation = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        if (Raycast.raycast(entityLocation, player.getEyeLocation(), 3, true)) {
            player.showEntity(plugin, entity);
        }
        else {
            player.hideEntity(plugin, entity);
        }
        /*
        Location entityLocation = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        Raycast.asyncRaycast(player.getEyeLocation(), entityLocation, plugin, canSeePlayer -> {
            if (canSeePlayer) {
                plugin.getLogger().info("Can see player");
                //plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.showEntity(plugin, entity);
                });
            } else {
                plugin.getLogger().info("Can't see player");
                //plugin.getCheckEntityVisibility().addEntityToHide(player, entity);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.hideEntity(plugin, entity);
                });
            }
        });*/
    }

    public void engineModeTwo(Player player, Entity entity, RaycastedEntityOcclusion plugin) {
        plugin.getLogger().warning("Engine mode 2 not active");
    }
    public void engineModeThree(Player player, Entity entity, RaycastedEntityOcclusion plugin) {
        plugin.getLogger().warning("Engine mode 3 not active");
    }
    public void engineModeFour(Player player, Entity entity, RaycastedEntityOcclusion plugin) {
        plugin.getLogger().warning("Engine mode 4 not active");
    }

/*    public void engineModeTwo /*Intensive four corners check/ (Player player, Entity entity, RaycastedEntityOcclusion plugin) {
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

    public void engineModeThree /*Doubled eye-to-eye check with one block penetration/(Player player, Entity entity, RaycastedEntityOcclusion plugin) {
        Location entityLocation = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        Raycast.asyncRaycastWithLocationReturn(entityLocation, player.getEyeLocation(), plugin, location -> {
            if (location == null) {
                plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
            } else {
                plugin.getLogger().info("One block"+ location);
                particleRed.add(location);
                Location newLocation = location.clone();
                // Run a new raycast from 1.5 blocks closer to player than location currently is
                Location playerEye = player.getEyeLocation();
                Vector direction = playerEye.toVector().subtract(newLocation.toVector());

                // Avoid normalizing a zero-length vector
                if (direction.lengthSquared() > 0.01) { // Small threshold to prevent precision errors
                    direction.normalize().multiply(1.5); // Scale to 1.5 blocks
                    newLocation.add(direction); // Move the location
                }
                particleBlue.add(newLocation);
                plugin.getLogger().info("New location"+ newLocation);
                Raycast.asyncRaycast(newLocation, playerEye, plugin, canSeePlayer -> {
                    if (canSeePlayer) {
                        plugin.getLogger().info("Can see");
                        plugin.getCheckEntityVisibility().addEntityToShow(player, entity);
                    } else {
                        plugin.getLogger().info("Can't see");
                        plugin.getCheckEntityVisibility().addEntityToHide(player, entity);
                    }
                });

            }
        });
    }



    public interface ModeThreeCallback {
        void onResult(boolean canSeePlayer);
    }


    public void engineModeThreeAsyncReturn /*Doubled eye-to-eye check with one block penetration/(Player player, Entity entity, RaycastedEntityOcclusion plugin, Location entityLocation, ModeThreeCallback callback) {
        Raycast.asyncRaycastWithLocationReturn(player.getEyeLocation(), entityLocation, plugin, location -> {
            if (location == null) {
                callback.onResult(true);
            } else {
                plugin.getLogger().info("One block"+ location);
                particleRed.add(location);
                Location newLocation = location.clone();
                // Run a new raycast from 1.5 blocks closer to player than location currently is
                Location playerEye = player.getEyeLocation();
                Vector direction = playerEye.toVector().subtract(newLocation.toVector());

                // Avoid normalizing a zero-length vector
                if (direction.lengthSquared() > 0.01) { // Small threshold to prevent precision errors
                    direction.normalize().multiply(1.5); // Scale to 1.5 blocks
                    newLocation.add(direction); // Move the location
                }
                particleBlue.add(newLocation);
                plugin.getLogger().info("New location"+ newLocation);
                Raycast.asyncRaycast(newLocation, playerEye, plugin, canSeePlayer -> {
                    Raycast.asyncRaycast(location, playerEye, plugin, callback::onResult);
                });

            }
        });


    }
    public void engineModeFour /*Doubled four point check with one block penetration/(Player player, Entity entity, RaycastedEntityOcclusion plugin){
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
    }*/
}