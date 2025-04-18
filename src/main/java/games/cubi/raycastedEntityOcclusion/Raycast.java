package games.cubi.raycastedEntityOcclusion;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Raycast {
    public static boolean raycast(Location start, Location end, int maxOccludingCount, boolean debugMode) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        World world = start.getWorld();
        Location current = start.clone();
        double totalDistance = start.distance(end);
        double traveled = 0.0;
        int occludingCount = maxOccludingCount;

        while (traveled < totalDistance) {
            current.add(direction.clone().multiply(1.0)); //cloning every time seems dumb
            traveled += 1.0;
            Block block = world.getBlockAt(current);

            if (block.getType().isOccluding()) {
                occludingCount--;
                if (debugMode) {
                    double t = (double) (maxOccludingCount - occludingCount) / maxOccludingCount;
                    int dustColour = (int) (165 * (1 - t));
                    Particle.DustOptions options = new Particle.DustOptions(Color.fromRGB(255, dustColour, 0), 1.0F);
                    world.spawnParticle(Particle.DUST, current, 1, options);
                }
                if (occludingCount < 1) {
                    return false;
                }
            } else if (debugMode) {
                Particle.DustOptions options = new Particle.DustOptions(Color.GREEN, 1.0F);
                world.spawnParticle(Particle.DUST, current, 1, options);
            }
        }
        return true;
    }

}
