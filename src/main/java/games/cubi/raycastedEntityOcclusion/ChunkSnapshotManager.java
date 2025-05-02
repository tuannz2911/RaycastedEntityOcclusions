package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkSnapshotManager {
    public static class Data {
        public final ChunkSnapshot snapshot;
        public final Map<Location, Material> delta = new ConcurrentHashMap<>();
        public long lastRefresh;

        public Data(ChunkSnapshot snapshot, long time) {
            this.snapshot = snapshot;
            this.lastRefresh = time;
        }
    }

    private final Map<String, Data> dataMap = new ConcurrentHashMap<>();

    public ChunkSnapshotManager(RaycastedEntityOcclusion plugin, int refreshIntervalSecs) {
        ConfigManager cfg = plugin.getConfigManager();
        //get loaded chunks and add them to dataMap
        for (World w : plugin.getServer().getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                dataMap.put(key(c), takeSnapshot(c, System.currentTimeMillis()));
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                int chunksRefreshed = 0;
                for (Map.Entry<String, Data> e : dataMap.entrySet()) {
                    if (now - e.getValue().lastRefresh >= refreshIntervalSecs * 1000L) {
                        if (cfg.debugMode) {
                            chunksRefreshed++;
                        }
                        String key = e.getKey();
                        String[] parts = key.split(":");
                        World w = plugin.getServer().getWorld(parts[0]);
                        if (w == null) {
                            plugin.getLogger().warning("ChunkSnapshotManager: World " + parts[0] + " not found. Please report this on our discord (discord.cubi.games)'");
                            continue;
                        }
                        Chunk c = w.getChunkAt(
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2])
                        );
                        e.setValue(takeSnapshot(c, now));
                    }
                }
                if (cfg.debugMode) {
                    plugin.getLogger().info("ChunkSnapshotManager: Refreshed " + chunksRefreshed + " chunks.");
                }
            }
        }.runTaskTimerAsynchronously(plugin, refreshIntervalSecs * 5L, refreshIntervalSecs * 5L /* This runs 4 times per refreshInterval, spreading out the refreshes */);
    }

    public void onChunkLoad(Chunk c) {
        dataMap.put(key(c), takeSnapshot(c, System.currentTimeMillis()));
    }

    public void onChunkUnload(Chunk c) {
        dataMap.remove(key(c));
    }

    // Used by SnapshotListener to update the delta map when a block is placed or broken
    public void onBlockChange(Location loc, Material m) {
        Data d = dataMap.get(key(loc.getChunk()));
        if (d != null) {
            d.delta.put(loc, m);
        }
    }

    private Data takeSnapshot(Chunk c, long now) {
        return new Data(c.getChunkSnapshot(), now);
    }

    private String key(Chunk c) {
        return c.getWorld().getName() + ":" + c.getX() + ":" + c.getZ();
    }

    public Material getMaterialAt(Location loc) {
        Data d = dataMap.get(key(loc.getChunk()));
        if (d == null) {
            Chunk c = loc.getChunk();
            dataMap.put(key(c), takeSnapshot(c, System.currentTimeMillis()));
            System.err.println("ChunkSnapshotManager: No snapshot for " + loc.getChunk()+ " Please report this on our discord (discord.cubi.games)'");
            return loc.getBlock().getType();
        }
        Material dm = d.delta.get(loc);
        if (dm != null) {
            return dm;
        }
        int x = loc.getBlockX() & 0xF;
        int y = loc.getBlockY();
        int z = loc.getBlockZ() & 0xF;
        return d.snapshot.getBlockType(x, y, z);
    }
}
