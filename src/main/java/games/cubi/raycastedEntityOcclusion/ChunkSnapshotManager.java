package games.cubi.raycastedEntityOcclusion;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import com.github.Anon8281.universalScheduler.UniversalRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkSnapshotManager {
    public static class Data {
        public final ChunkSnapshot snapshot;
        public final Map<Location, Material> delta = new ConcurrentHashMap<>();
        public final Set<Location> tileEntities = ConcurrentHashMap.newKeySet();
        public long lastRefresh;

        public Data(ChunkSnapshot snapshot, long time) {
            this.snapshot = snapshot;
            this.lastRefresh = time;
        }
    }

    private final Map<String, Data> dataMap = new ConcurrentHashMap<>();
    private final ConfigManager cfg;

    public ChunkSnapshotManager(RaycastedEntityOcclusion plugin) {
        cfg = plugin.getConfigManager();
        //get loaded chunks and add them to dataMap
        for (World w : plugin.getServer().getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                dataMap.put(key(c), takeSnapshot(c, System.currentTimeMillis()));
            }
        }

        new UniversalRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                int chunksRefreshed = 0;
                int chunksToRefreshMaximum = getNumberOfCachedChunks() / 3;
                for (Map.Entry<String, Data> e : dataMap.entrySet()) {
                    if (now - e.getValue().lastRefresh >= cfg.snapshotRefreshInterval * 1000L && chunksRefreshed < chunksToRefreshMaximum) {
                        chunksRefreshed++;
                        String key = e.getKey();
                        String[] parts = key.split(":");
                        World w = plugin.getServer().getWorld(parts[0]);
                        if (w == null) {
                            plugin.getLogger().warning("ChunkSnapshotManager: World " + parts[0] + " not found. Please report this on our discord (discord.cubi.games)'");
                            continue;
                        }
                        Location loc = new Location(w, Integer.parseInt(parts[1]), 0, Integer.parseInt(parts[2]));
                        RaycastedEntityOcclusion.getScheduler().runTask(loc, () -> {
                            Chunk c = w.getChunkAt(loc);
                            e.setValue(takeSnapshot(c, now));
                        });
                    }
                }
                if (cfg.debugMode) {
                    plugin.getLogger().info("ChunkSnapshotManager: Refreshed " + chunksRefreshed + " chunks out of " + chunksToRefreshMaximum + " maximum.");
                }
            }
        }.runTaskTimerAsynchronously(plugin, cfg.snapshotRefreshInterval * 2L, cfg.snapshotRefreshInterval * 2L /* This runs 10 times per refreshInterval, spreading out the refreshes */);
    }

    public void onChunkLoad(Chunk c) {
        dataMap.put(key(c), takeSnapshot(c, System.currentTimeMillis()));
    }

    public void onChunkUnload(Chunk c) {
        dataMap.remove(key(c));
    }

    // Used by SnapshotListener to update the delta map when a block is placed or broken
    public void onBlockChange(Location loc, Material m) {
        RaycastedEntityOcclusion.getScheduler().runTask(loc, () -> {
            Data d = dataMap.get(key(loc.getChunk()));
            if (d != null) {
                d.delta.put(loc, m);
                if (cfg.checkTileEntities) {
                    // Check if the block is a tile entity
                    BlockState data = loc.getBlock().getState();
                    Location centerLoc = loc.clone().add(0.5, 0, 0.5);
                    if (data instanceof TileState) {
                        if (cfg.debugMode){
                            Bukkit.getLogger().info("ChunkSnapshotManager: Tile entity at " + centerLoc);
                        }
                        d.tileEntities.add(centerLoc);
                    } else {
                        d.tileEntities.remove(centerLoc);
                    }
                }
            }
            if (cfg.debugMode) {
                Bukkit.getLogger().info("ChunkSnapshotManager: Block change at " + loc + " to " + m);
            }
        });
    }

    private Data takeSnapshot(Chunk c, long now) {
        World w = c.getWorld();
        Data data = new Data(c.getChunkSnapshot(), now);
        int chunkX = c.getX() * 16;
        int chunkZ = c.getZ() * 16;
        if (cfg.checkTileEntities) {
            for (int x = 0; x < 16; x++) {
                for (int y = w.getMinHeight(); y < w.getMaxHeight(); y++) {
                    for (int z = 0; z < 16; z++) {
                            BlockState bs = data.snapshot.getBlockData(x, y, z).createBlockState();

                            if (bs instanceof TileState) {
                                data.tileEntities.add(new Location(w, x+ chunkX +0.5, y, z + chunkZ+0.5));
                            }
                    }
                }
            }
        }
        return data;
    }

    private String key(Chunk c) {
        return c.getWorld().getName() + ":" + c.getX() + ":" + c.getZ();
    }

    public Material getMaterialAt(Location loc) {
        Data d = dataMap.get(key(loc.getChunk()));
        if (d == null) {
            Chunk c = loc.getChunk();
            //dataMap.put(key(c), takeSnapshot(c, System.currentTimeMillis())); infinite loop
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

    //get TileEntity Locations in chunk
    public Set<Location> getTileEntitiesInChunk(String worldName, int x, int z) {
        Data d = dataMap.get(worldName + ":" + x + ":" + z);
        if (d == null) {
            return Collections.emptySet();
        }
        return d.tileEntities;
    }

    public int getNumberOfCachedChunks() {
        return dataMap.size();
        //created to use in a debug command maybe
    }

}
