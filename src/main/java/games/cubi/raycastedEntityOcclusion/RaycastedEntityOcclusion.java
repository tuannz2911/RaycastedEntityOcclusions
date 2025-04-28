package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RaycastedEntityOcclusion extends JavaPlugin implements CommandExecutor {
    private ConfigManager cfg;
    private ChunkSnapshotManager snapMgr;
    private MovementTracker tracker;

    @Override
    public void onEnable() {
        cfg = new ConfigManager(this);
        snapMgr = new ChunkSnapshotManager(this, cfg.snapshotRefreshInterval);
        tracker = new MovementTracker(this);
        getServer().getPluginManager().registerEvents(new SnapshotListener(snapMgr), this);
        getCommand("raycastedentityocclusion").setExecutor(this);

        //bStats
        int pluginId = 24553;
        new Metrics(this, pluginId);

        new BukkitRunnable() {
            @Override
            public void run() {
                runEngine();
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            cfg.load();
            p.sendMessage("[EntityOcclusion] Config reloaded.");
            return true;
        }
        p.sendMessage("RaycastOcclusion; engine-mode=" + cfg.engineMode + ", Debug=" + cfg.debugMode);
        return true;
    }

    // --- helper to hold one ray job ---
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

    // --- helper to hold one ray result ---
    private static class RayResult {
        final UUID playerId, entityId;
        final boolean visible;

        RayResult(UUID p, UUID e, boolean v) {
            playerId = p;
            entityId = e;
            visible = v;
        }
    }

    private void runEngine() {
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
                    p.showEntity(this, e);
                    continue;
                }

                Location target = e.getLocation().add(0, e.getHeight() / 2, 0).clone();
                double dist = eye.distance(target);
                if (dist <= cfg.alwaysShowRadius) {
                    p.showEntity(this, e);
                } else if (dist > cfg.raycastRadius) {
                    p.hideEntity(this, e);
                } else {
                    // schedule for async raycast (with or without predEye)
                    jobs.add(new RayJob(p.getUniqueId(), e.getUniqueId(), eye, predEye, target));
                }
            }
        }

        // ----- PHASE 2: ASYNC RAYCASTS -----
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            List<RayResult> results = new ArrayList<>(jobs.size());
            for (RayJob job : jobs) {
                // first cast from real eye
                boolean vis = RaycastUtil.raycast(
                        job.start, job.end,
                        cfg.maxOccludingCount, /*debug=*/false,
                        snapMgr, /*playerForParticles=*/null
                );

                // if that fails and we have a predEye, cast again from predicted
                if (!vis && job.predictedStart != null) {
                    vis = RaycastUtil.raycast(
                            job.predictedStart, job.end,
                            cfg.maxOccludingCount, /*debug=*/false,
                            snapMgr, /*playerForParticles=*/null
                    );
                }

                results.add(new RayResult(job.playerId, job.entityId, vis));
            }

            // ----- PHASE 3: SYNC APPLY -----
            Bukkit.getScheduler().runTask(this, () -> {
                for (RayResult r : results) {
                    Player p = Bukkit.getPlayer(r.playerId);
                    Entity ent = Bukkit.getEntity(r.entityId);
                    if (p != null && ent != null) {
                        if (r.visible) p.showEntity(this, ent);
                        else p.hideEntity(this, ent);
                    }
                }
            });
        });
    }
}