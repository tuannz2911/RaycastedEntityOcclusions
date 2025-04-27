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

import java.util.List;

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
                for (Player p : Bukkit.getOnlinePlayers()) runEngine(p);
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
        p.sendMessage("RaycastOcclusion; engine-mode=" + cfg.engineMode);
        return true;
    }

    private void runEngine(Player p) {
        // Precompute tickCheck (0 only every recheckInterval ticks)
        boolean tickZero = (getServer().getCurrentTick() % cfg.recheckInterval == 0);

        List<Entity> ents = p.getNearbyEntities(cfg.searchRadius, cfg.searchRadius, cfg.searchRadius);
        for (Entity e : ents) {
            if (e == p) continue;
            // Cull players if disabled
            if (e instanceof Player && !cfg.cullPlayers) continue;
            // Only cull sneaking players if configured
            if (e instanceof Player pl && cfg.onlyCullSneakingPlayers && !pl.isSneaking()) {
                p.showEntity(this, e);
                continue;
            }

            Location eye = p.getEyeLocation();
            Location target = e.getLocation().add(0, e.getHeight()/2, 0);
            double dist = eye.distance(target);

            // Always-show and auto-hide radii
            if (dist <= cfg.alwaysShowRadius) {
                p.showEntity(this, e);
                continue;
            }
            if (dist > cfg.raycastRadius) {
                p.hideEntity(this, e);
                continue;
            }

            boolean currentlyVisible = p.canSee(e);

            // Only raycast visible entities at recheck intervals; hidden ones every tick
            boolean needRaycast = !currentlyVisible || tickZero;
            if (needRaycast) {
                boolean visible = RaycastUtil.raycast(eye, target, cfg.maxOccludingCount, cfg.debugMode, snapMgr, p);
                if (!visible && cfg.engineMode == 2) {
                    Location pred = tracker.getPredictedLocation(p);
                    if (pred != null) {
                        visible = RaycastUtil.raycast(pred, target, cfg.maxOccludingCount, cfg.debugMode, snapMgr, p);
                        //TODO: doesn't account for target moving, look into resolving later
                    }
                }
                if (visible) p.showEntity(this, e);
                else p.hideEntity(this, e);
            }
            // else: nothing (maintain current state)
        }
    }
}
