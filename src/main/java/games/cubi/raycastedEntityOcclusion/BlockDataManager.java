package games.cubi.raycastedEntityOcclusion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockDataManager {

    private static class CachedBlockData {
        private final Material material;
        private final AtomicInteger lastAccessTick;

        public CachedBlockData(Material material, int currentTick) {
            this.material = material;
            this.lastAccessTick = new AtomicInteger(currentTick);
        }

        public Material getMaterial() {
            return material;
        }

        public int getLastAccessTick() {
            return lastAccessTick.get();
        }

        public void updateLastAccessTick(int tick) {
            lastAccessTick.set(tick);
        }
    }

    /**
     * Thread-safe map of location -> cached block data
     */
    private final Map<Location, CachedBlockData> cache = new ConcurrentHashMap<>();

    /**
     * The plugin reference for scheduling tasks.
     */
    private final Plugin plugin;

    /**
     * Number of ticks after which a block is considered stale if not accessed.
     */
    private volatile int expireAfterTicks = 2400;

    /**
     * Number of ticks between each cleanup run.
     */
    private volatile int cleanupInterval = 1200;

    /**
     * Tracks the current server tick (incremented on main thread), read by async tasks.
     */
    private final AtomicInteger serverTickCounter = new AtomicInteger(0);

    /**
     * The sync task that increments serverTickCounter every tick.
     */
    private BukkitTask tickSyncTask;

    /**
     * The async task that periodically calls cleanupCache().
     */
    private BukkitTask cleanupAsyncTask;

    /**
     * Constructor to initialize the manager with desired expiration and cleanup intervals.
     *
     * @param plugin The plugin instance
     */
    public BlockDataManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the tasks that:
     *   1) increment serverTickCounter every tick (sync),
     *   2) run cleanupCache() periodically (async).
     */
    public void start() {
        // 1) A sync task that increments our tick counter each tick:
        tickSyncTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            serverTickCounter.incrementAndGet();
        }, 1L, 1L);

        // 2) Schedule the cleanup task at the current cleanupInterval.
        scheduleCleanupTask();
    }

    /**
     * Stop both the tick increment task and the cleanup task.
     */
    public void stop() {
        if (tickSyncTask != null) {
            tickSyncTask.cancel();
        }
        if (cleanupAsyncTask != null) {
            cleanupAsyncTask.cancel();
        }
    }

    /**
     * Dynamically update the expireAfterTicks and cleanupInterval at runtime,
     * e.g., if the plugin config is reloaded.
     *
     * @param newExpireAfterTicks New expiration (in ticks)
     * @param newCleanupInterval  New cleanup interval (in ticks)
     */
    public void reloadConfigOptions(int newExpireAfterTicks, int newCleanupInterval) {
        this.expireAfterTicks = newExpireAfterTicks;
        this.cleanupInterval = newCleanupInterval;
        // Reschedule the cleanup task with the new interval
        scheduleCleanupTask();
    }

    /**
     * Internal helper: schedules/cancels the cleanup task with the updated cleanupInterval.
     */
    private void scheduleCleanupTask() {
        // Cancel any existing cleanup task first
        if (cleanupAsyncTask != null) {
            cleanupAsyncTask.cancel();
        }
        // Then schedule a new asynchronous repeating task
        cleanupAsyncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            cleanupCache();
        }, cleanupInterval, cleanupInterval);
    }

    /**
     * Gets a block's cached material (if present), updating its access time.
     * Returns null if not found in cache.
     */
    public Material getCachedBlockMaterial(Location location) {
        CachedBlockData data = cache.get(location);
        if (data != null) {
            data.updateLastAccessTick(serverTickCounter.get());
            return data.getMaterial();
        }
        return null;
    }

    /**
     * Cache a block's material, associating it with the current server tick.
     */
    public void putBlock(Location location, Material material) {
        // Clone location to avoid potential mutation issues
        Location key = location.clone();
        cache.put(key, new CachedBlockData(material, serverTickCounter.get()));
    }

    /**
     * Explicitly remove a block from the cache if needed.
     */
    public void removeBlock(Location location) {
        cache.remove(location);
    }

    /**
     * Removes entries that have not been accessed within expireAfterTicks.
     * This runs async, which is safe due to ConcurrentHashMap.
     */
    private void cleanupCache() {
        int currentTick = serverTickCounter.get();

        for (Map.Entry<Location, CachedBlockData> entry : cache.entrySet()) {
            int lastAccess = entry.getValue().getLastAccessTick();
            if (currentTick - lastAccess > expireAfterTicks) {
                cache.remove(entry.getKey());
            }
        }
    }

    /**
     * Fetch a block's Material asynchronously, ensuring it is in cache:
     *   - If cached, returns immediately via callback on the *async* thread
     *   - Otherwise, fetch on main thread, then callback.
     */
    public void getBlockMaterialAsync(Location location, java.util.function.Consumer<Material> callback) {
        Material cached = getCachedBlockMaterial(location);
        if (cached != null) {
            // Cache hit, callback right away (still on async thread here)
            callback.accept(cached);
        } else {
            // Not cached: do a sync fetch
            Bukkit.getScheduler().runTask(plugin, () -> {
                Block block = location.getWorld().getBlockAt(location);
                Material material = block.getType();
                putBlock(location, material);
                // Then callback (now on main thread)
                callback.accept(material);
            });
        }
    }
}