package games.cubi.raycastedEntityOcclusion;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Raycast {
    public static void asyncRaycast(Location start, Location end, RaycastedEntityOcclusion plugin, RaycastCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Vector direction = end.toVector().subtract(start.toVector()).normalize();
                double distance = start.distance(end);


                // Perform the ray trace
                RayTraceResult result = start.getWorld().rayTraceBlocks(start, direction, distance,
                        FluidCollisionMode.NEVER, // Ignore fluids
                        true, // Pass through transparent blocks
                        // Check if the block is occluding
                        (block) -> isOccluding(block.getType())
                );

                // Check if the ray hit a solid block or reached the player
                if (result == null || result.getHitBlock() == null) {
                    // No solid block was hit, so the ray reached the player
                    callback.onResult(true);
                } else {
                    // A solid block was hit
                    callback.onResult(false);
                    //plugin.getLogger().info("Raycast hit a solid block: " + result.getHitBlock().getType() + " at " + result.getHitBlock().getLocation());

                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public interface RaycastCallback {
        void onResult(boolean canSeePlayer);
    }
    private static boolean isOccluding(Material material) {
        return material.isOccluding();
    }

    public static boolean syncRaycast(Location start, Location end, RaycastedEntityOcclusion plugin) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);


        // Perform the ray trace
        RayTraceResult result = start.getWorld().rayTraceBlocks(start, direction, distance,
                FluidCollisionMode.NEVER, // Ignore fluids
                true, // Pass through transparent blocks
                // Check if the block is occluding
                (block) -> isOccluding(block.getType())
        );

        // Check if the ray hit a solid block or reached the player
        if (result == null || result.getHitBlock() == null) {
            // No solid block was hit, so the ray reached the player
            return true;
        } else {
            // A solid block was hit
            return false;
            //plugin.getLogger().info("Raycast hit a solid block: " + result.getHitBlock().getType() + " at " + result.getHitBlock().getLocation());

        }
    }
}
