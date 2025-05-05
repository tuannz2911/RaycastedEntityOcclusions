package games.cubi.raycastedEntityOcclusion;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandExecutor;
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
    private CommandsManager commands;

    public int tick = 0;

    @Override
    public void onEnable() {
        cfg = new ConfigManager(this);
        snapMgr = new ChunkSnapshotManager(this);
        tracker = new MovementTracker(this);
        commands = new CommandsManager(this, cfg);
        getServer().getPluginManager().registerEvents(new SnapshotListener(snapMgr), this);
        getServer().getPluginManager().registerEvents(new UpdateChecker(this), this);

        //Brigadier API
        LiteralCommandNode<CommandSourceStack> buildCommand = commands.registerCommand();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(buildCommand);
            //alias "reo"
            commands.registrar().register(Commands.literal("reo")
                    .requires(sender -> sender.getSender().hasPermission("raycastedentityocclusions.command"))
                    .executes(context -> {
                        new CommandsManager(this, cfg).helpCommand(context);
                        return Command.SINGLE_SUCCESS;
                    })
                    .redirect(buildCommand).build());
        });

        //bStats
        int pluginId = 24553;
        new Metrics(this, pluginId);

        new BukkitRunnable() {
            @Override
            public void run() {
                tick++;
                Engine.runEngine(cfg, snapMgr, tracker, RaycastedEntityOcclusion.this);
                Engine.runTileEngine(cfg, snapMgr, tracker, RaycastedEntityOcclusion.this);
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    public ConfigManager getConfigManager() {
        return cfg;
    }
}