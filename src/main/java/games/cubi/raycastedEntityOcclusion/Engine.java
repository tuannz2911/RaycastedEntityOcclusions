package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Engine {

    private static class RayJob {
        final UUID playerId, entityId;
        final Location start, predictedStart, end;

        RayJob(UUID p, UUID e, Location s, Location pred, Location t) {
            playerId = p;
            entityId = e;
            start = s;
            predictedStart = pred;
            end = t;
        }
    }

    private static class RayResult {
        final UUID playerId, entityId;
        final boolean visible;

        RayResult(UUID p, UUID e, boolean v) {
            playerId = p;
            entityId = e;
            visible = v;
        }
    }

    public static void runEngine(ConfigManager cfg, ChunkSnapshotManager snapMgr, MovementTracker tracker, RaycastedEntityOcclusion plugin) {
        // ----- PHASE 1: SYNC GATHER -----
        List<RayJob> jobs = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location eye = p.getEyeLocation().clone();
            Location predEye = null;
            if (cfg.engineMode == 2) {
                // getPredictedLocation returns null if insufficient data or too slow
                predEye = tracker.getPredictedLocation(p);
            }

            for (Entity e : p.getNearbyEntities(cfg.searchRadius, cfg.searchRadius, cfg.searchRadius)) {
                if (e == p) continue;
                // Cull-players logic
                if (e instanceof Player pl && (!cfg.cullPlayers || (cfg.onlyCullSneakingPlayers && !pl.isSneaking()))) {
                    p.showEntity(plugin, e);
                    continue;
                }

                Location target = e.getLocation().add(0, e.getHeight() / 2, 0).clone();
                double dist = eye.distance(target);
                if (dist <= cfg.alwaysShowRadius) {
                    p.showEntity(plugin, e);
                } else if (dist > cfg.raycastRadius) {
                    p.hideEntity(plugin, e);
                } else if (p.canSee(e) && plugin.tick % cfg.recheckInterval != 0) {
                    // player can see entity, no need to raycast
                }
                else {
                    // schedule for async raycast (with or without predEye)
                    jobs.add(new RayJob(p.getUniqueId(), e.getUniqueId(), eye, predEye, target));
                }
            }
        }

        // ----- PHASE 2: ASYNC RAYCASTS -----
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<RayResult> results = new ArrayList<>(jobs.size());
            for (RayJob job : jobs) {
                // first cast from real eye
                boolean vis = RaycastUtil.raycast(job.start, job.end, cfg.maxOccludingCount, cfg.debugMode, snapMgr);

                // if that fails and we have a predEye, cast again from predicted
                if (!vis && job.predictedStart != null) {
                    if (cfg.debugMode) {
                        job.predictedStart.getWorld().spawnParticle(Particle.DUST, job.predictedStart, 1, new Particle.DustOptions(org.bukkit.Color.BLUE,1f));
                    }
                    vis = RaycastUtil.raycast(job.predictedStart, job.end, cfg.maxOccludingCount, cfg.debugMode, snapMgr);
                }

                results.add(new RayResult(job.playerId, job.entityId, vis));
            }

            // ----- PHASE 3: SYNC APPLY -----
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (RayResult r : results) {
                    Player p = Bukkit.getPlayer(r.playerId);
                    Entity ent = Bukkit.getEntity(r.entityId);
                    if (p != null && ent != null) {
                        if (r.visible) p.showEntity(plugin, ent);
                        else p.hideEntity(plugin, ent);
                    }
                }
            });
        });
    }
}
