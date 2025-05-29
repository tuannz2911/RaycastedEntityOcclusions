package games.cubi.raycastedEntityOcclusion;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;


public class UpdateChecker implements org.bukkit.event.Listener {
    private final RaycastedEntityOcclusion plugin;

    public UpdateChecker(RaycastedEntityOcclusion plugin) {
        this.plugin = plugin;
        checkForUpdates(plugin, Bukkit.getConsoleSender());
    }

    public void checkForUpdates() {
    }

    public static CompletableFuture<String> fetchFeaturedVersion(RaycastedEntityOcclusion plugin) {
        CompletableFuture<String> future = new CompletableFuture<>();

        RaycastedEntityOcclusion.getScheduler().runTaskAsynchronously(plugin, () -> {

            final String url = "https://api.modrinth.com/v2/project/raycasted-entity-occlusions/version?featured=true";
            try (final InputStreamReader reader = new InputStreamReader(new URL(url).openConnection().getInputStream())) {
                final JsonArray array = new JsonArray();
                array.add(new BufferedReader(reader).readLine());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < array.size(); i++) {
                    /*
                    final JsonObject object = array.get(i).getAsJsonObject();
                    if (object.get("version_type").getAsString().equals("release")) {
                        future.complete(object.get("version_number").getAsString());
                    }*/
                    sb.append(array.get(i).getAsString());
                }
                String apiData = sb.toString();
                JsonArray jsonArray = JsonParser.parseString(apiData).getAsJsonArray();
                JsonObject firstObject = jsonArray.get(0).getAsJsonObject();
                String versionNumber = firstObject.get("version_number").getAsString();

                future.complete(versionNumber);

                //future.completeExceptionally(new IllegalStateException("No versions found"));
            } catch (IOException e) {
                future.completeExceptionally(new IllegalStateException("Unable to fetch latest version", e));
            }
        });
        return future;
    }

    //on join
    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("raycastedentityocclusions.updatecheck")) {
            Player sender = event.getPlayer();
            checkForUpdates(plugin, sender);
        }
    }
    public static void checkForUpdates(RaycastedEntityOcclusion plugin, CommandSender audience) {
        fetchFeaturedVersion(plugin).thenAccept(version -> {
            // This runs asynchronously when the version is fetched
            RaycastedEntityOcclusion.getScheduler().runTask(plugin, () -> {
                if (plugin.getDescription().getVersion().equals(version)) {
                    audience.sendRichMessage("<green>You are using the latest version of Raycasted Entity Occlusions.");
                } else {
                    audience.sendRichMessage("<red>You are not using the latest version of Raycasted Entity Occlusions. Please update to <green>" + version);
                }
            });
        }).exceptionally(ex -> {
            // Handle error (e.g., log the exception)
            RaycastedEntityOcclusion.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().warning("Failed to fetch version: " + ex.getMessage());
            });
            return null;
        });
    }
}
