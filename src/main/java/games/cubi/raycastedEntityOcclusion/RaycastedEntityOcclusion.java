package games.cubi.raycastedEntityOcclusion;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RaycastedEntityOcclusion extends JavaPlugin {

    public int alwaysShowRadius;
    public int raycastRadius;
    public int searchRadius;
    public boolean moreChecks;
    public boolean occludePlayers;
    public int recheckInterval;
    public int tickCounter = 0;
    public RaycastedEntityOcclusion plugin = this;
    public CheckEntityVisibility checkEntityVisibility;

    @Override
    public void onEnable() {
        loadConfig();

        int pluginId = 24553;
        new Metrics(this, pluginId);

        checkEntityVisibility = new CheckEntityVisibility(plugin);

        getCommand("raycastedentityocclusion").setExecutor(new CommandHandler(plugin));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration config = getConfig();

        alwaysShowRadius = config.getInt("AlwaysShowRadius", 4);
        raycastRadius = config.getInt("RaycastRadius", 64);
        searchRadius = config.getInt("SearchRadius", 72);
        moreChecks = config.getBoolean("MoreChecks", false);
        occludePlayers = config.getBoolean("OccludePlayers", false);
        recheckInterval = config.getInt("RecheckInterval", 10);


        getLogger().info("AlwaysShowRadius: " + alwaysShowRadius);
        getLogger().info("RaycastRadius: " + raycastRadius);
        getLogger().info("SearchRadius: " + searchRadius);
        getLogger().info("MoreChecks: " + moreChecks);
        getLogger().info("OccludePlayers: " + occludePlayers);
        getLogger().info("RecheckInterval: " + recheckInterval);
    }
    public CheckEntityVisibility getCheckEntityVisibility() {
        return checkEntityVisibility;
    }
}