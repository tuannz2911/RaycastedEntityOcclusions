package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkSnapshotManager {
    public static class Data {
        public final Material[][][] snapshot;
        public final Map<Location, Material> delta = new ConcurrentHashMap<>();
        public long lastRefresh;
        public Data(Material[][][] snap, long time) {
            this.snapshot = snap;
            this.lastRefresh = time;
        }
    }

    private final Map<String, Data> map = new ConcurrentHashMap<>();

    public ChunkSnapshotManager(Plugin plugin, int refreshIntervalSecs) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<String, Data> e : map.entrySet()) {
                    if (now - e.getValue().lastRefresh >= refreshIntervalSecs*1000L) {
                        String key = e.getKey();
                        String[] parts = key.split(":");
                        World w = plugin.getServer().getWorld(parts[0]);
                        Chunk c = w.getChunkAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        e.setValue(takeSnapshot(c, now));
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, refreshIntervalSecs*20L, refreshIntervalSecs*20L);
    }

    public void onChunkLoad(Chunk c) {
        map.put(key(c), takeSnapshot(c, System.currentTimeMillis()));
    }

    public void onChunkUnload(Chunk c) {
        map.remove(key(c));
    }

    public void onBlockChange(Location loc, Material m) {
        Data d = map.get(key(loc.getChunk()));
        if (d != null) d.delta.put(loc, m);
    }

    private Data takeSnapshot(Chunk c, long now) {
        World w = c.getWorld();
        int height = w.getMaxHeight();
        Material[][][] arr = new Material[16][height][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < height; y++) {
                    arr[x][y][z] = c.getBlock(x, y, z).getType();
                }
            }
        }
        return new Data(arr, now);
    }


    private String key(Chunk c) {
        return c.getWorld().getName()+":"+c.getX()+":"+c.getZ();
    }

    public Material getMaterialAt(Location loc) {
        Data d = map.get(key(loc.getChunk()));
        if (d == null) return loc.getBlock().getType();
        Material dm = d.delta.get(loc);
        if (dm != null) return dm;
        int x = loc.getBlockX() & 0xF;
        int y = loc.getBlockY();
        int z = loc.getBlockZ() & 0xF;
        return d.snapshot[x][y][z];
    }

}