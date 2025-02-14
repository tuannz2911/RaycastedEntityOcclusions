package games.cubi.raycastedEntityOcclusion;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RaycastedEntityOcclusion extends JavaPlugin {

    public int alwaysShowRadius;
    public int raycastRadius;
    public int searchRadius;
    public int engineMode;
    public boolean cullPlayers;
    public int recheckInterval;
    public boolean sneakCull;
    public RaycastedEntityOcclusion plugin = this;
    public CheckEntityVisibility checkEntityVisibility;
    public SneakListener sneakListener;

    @Override
    public void onEnable() {
        loadConfig();

        int pluginId = 24553;
        new Metrics(this, pluginId);

        checkEntityVisibility = new CheckEntityVisibility(plugin);
        sneakListener = new SneakListener(plugin);

        getCommand("raycastedentityocclusion").setExecutor(new CommandHandler(plugin));
        getServer().getPluginManager().registerEvents(sneakListener, plugin);
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
        raycastRadius = config.getInt("RaycastRadius", 48);
        searchRadius = config.getInt("SearchRadius", 48);
        engineMode = config.getInt("EngineMode", 3);
        cullPlayers = config.getBoolean("CullPlayers", false);
        sneakCull = config.getBoolean("OnlyCullSneakingPlayers", false);

        recheckInterval = config.getInt("RecheckInterval", 20);

        // This ensures that if the config is not set, it will be set to the default value
        if (alwaysShowRadius == 4) config.set("AlwaysShowRadius", 4);
        if (raycastRadius == 48) config.set("RaycastRadius", 48);
        if (searchRadius == 48) config.set("SearchRadius", 48);
        if (engineMode == 3) config.set("EngineMode", 3);
        if (!cullPlayers) config.set("CullPlayers", false);
        if (!sneakCull) config.set("OnlyCullSneakingPlayers", false);
        if (recheckInterval == 20) config.set("RecheckInterval", 20);


        if (config.contains("OccludePlayers")) {
            getLogger().warning("The config option 'OccludePlayers' is outdated and has been replaced with 'CullPlayers'. The value has been automatically converted.");
            cullPlayers = config.getBoolean("OccludePlayers");
            config.set("CullPlayers", cullPlayers);
            config.set("OccludePlayers", null);
        }
        if (config.contains("moreChecks")) {
            getLogger().warning("The config option 'MoreChecks' is outdated and has been replaced with 'EngineMode'. Two new engine modes were also added. The engine mode has been automatically set to 3.");
            config.set("EngineMode", 3);
            config.set("MoreChecks", null);
        }

        getLogger().info("AlwaysShowRadius: " + alwaysShowRadius);
        getLogger().info("RaycastRadius: " + raycastRadius);
        getLogger().info("SearchRadius: " + searchRadius);
        getLogger().info("EngineMode: " + engineMode);
        getLogger().info("CullPlayers: " + cullPlayers);
        getLogger().info("OnlyCullSneakingPlayers: " + sneakCull);
        getLogger().info("RecheckInterval: " + recheckInterval);

        saveConfig();
    }
    public CheckEntityVisibility getCheckEntityVisibility() {
        return checkEntityVisibility;
    }
    public SneakListener getSneakListener() {
        return sneakListener;
    }
}