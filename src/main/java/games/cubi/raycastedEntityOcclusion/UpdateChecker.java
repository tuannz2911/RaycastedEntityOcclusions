package games.cubi.raycastedEntityOcclusion;


import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;


public class UpdateChecker {
    /*private final RaycastedEntityOcclusion plugin;

    public UpdateChecker(RaycastedEntityOcclusion plugin) {
        this.plugin = plugin;
        checkForUpdates();
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (hasNewUpdate() != null) {
                plugin.getLogger().info("A new version of RaycastedEntityOcclusions is available! ");
                plugin.getLogger().info("Please update to the latest version for the best experience.");
            } else {
                plugin.getLogger().info("You are using the latest version of RaycastedEntityOcclusions.");
            }
        });
    }

    public String hasNewUpdate() {
        try {
            String versionUrl = "https://example.com/version.txt";
            URL url = new URI(versionUrl).toURL();
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(500); // 5s connect timeout
            conn.setReadTimeout(500);    // 5s read timeout

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
                String version = reader.readLine();
                if (version == null) {
                    plugin.getLogger().warning("Could not check for updates: version is null");
                    return null;
                }
                String currentVersion = plugin.getDescription().getVersion();
                if (!currentVersion.equals(version)) {
                    return version;
                }
                return null;

            }
            catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
                return null;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
            return null;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    //on join
    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("raycastedentityocclusions.updatecheck")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String newVersion = hasNewUpdate();
                if (newVersion != null) {
                    event.getPlayer().sendRichMessage("<#FF00FF>A new version of <green>RaycastedEntityOcclusions<#FF00FF> is available! \n" +
                            "\n" +
                            "<white>Please update to the latest version for the best experience.\n" +
                            "\n" +
                            "<blue><aqua><u><click:open_url:'https://modrinth.com/plugin/raycasted-entity-occlusions/versions'>Click here to download</click></u></aqua></blue>");
                } else {
                    event.getPlayer().sendMessage("<green>You are using the latest version of RaycastedEntityOcclusions.");
                }
            });
        }
    }
*/
}
