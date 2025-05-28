package games.cubi.raycastedEntityOcclusion;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;

public class PacketEvents {
    public static void showTabName(Player p, Player other) {
        Bukkit.getLogger().info("Showing tab name for " + other.displayName() + " to " + p.getName());
        UserProfile profile = new UserProfile(other.getUniqueId(), other.getName());
        PlayerInfo data = new PlayerInfo(
                profile,
                true,
                999,
                GameMode.ADVENTURE,
                other.displayName(),
                null
        );

        WrapperPlayServerPlayerInfoUpdate addPacket = new WrapperPlayServerPlayerInfoUpdate(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, Collections.singletonList(data));

        com.github.retrooper.packetevents.PacketEvents.getAPI().getPlayerManager().sendPacket(p, addPacket);

    }
}
