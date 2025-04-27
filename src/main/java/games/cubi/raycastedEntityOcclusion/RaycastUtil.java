package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RaycastUtil {
    public static boolean raycast(
            Location start, Location end,
            int maxOccluding, boolean debug,
            ChunkSnapshotManager snap,
            Player p) {
        double total = start.distance(end);
        double traveled = 0;
        Location curr = start.clone();
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        while (traveled < total) {
            curr.add(dir);
            traveled += 1;
            Material mat = snap.getMaterialAt(curr);
            //System.out.println(curr + " " + mat);
            if (mat.isOccluding()) {
                maxOccluding--;
                if (debug) {
                    p.spawnParticle(Particle.DUST, curr, 1, new Particle.DustOptions(org.bukkit.Color.RED,1f));
                }
                if (maxOccluding < 1) return false;
            }
            else if (debug) {
                p.spawnParticle(Particle.DUST, curr, 1, new Particle.DustOptions(org.bukkit.Color.GREEN,1f));
            }
        }
        return true;
    }
}