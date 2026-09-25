package jackiecrazy.enchantlimiter.exempt;

import jackiecrazy.enchantlimiter.LimiterConfig;
import jackiecrazy.enchantlimiter.network.ExemptSyncPacket;
import jackiecrazy.enchantlimiter.network.LimiterNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Central place that decides whether the enchantment limit applies right now.
 */
public final class Exemptions {
    /**
     * Client-side copy of the local player's exemption, sent by the server. Plain field in a
     * common class so the dedicated server never loads client classes.
     */
    public static volatile boolean clientExempt = false;
    // last value sent to each client (server side), so resyncs only send changes
    private static final Map<UUID, Boolean> lastSent = new HashMap<>();

    private Exemptions() {
    }

    /**
     * True if the limit must be enforced for the operation currently running on this thread.
     * Used by the mixins and anvil handler instead of the old global LimiterConfig.isModEnabled().
     */
    public static boolean isLimitActive() {
        if (!LimiterConfig.isModEnabled()) return false;
        Player player = PlayerContext.current();
        return player == null || !isExempt(player);
    }

    public static boolean isExempt(Player player) {
        if (player.level().isClientSide) return clientExempt;
        if (!(player instanceof ServerPlayer sp)) return false;
        return isExempt(sp.server, sp.getUUID());
    }

    /**
     * Server-side exemption check, also valid for offline players.
     */
    public static boolean isExempt(MinecraftServer server, UUID player) {
        ExemptionData data = ExemptionData.get(server);
        if (data.contains(player)) return true;
        // Live team check: joining an exempt player's party grants it, leaving revokes it.
        for (UUID member : teamMembers(player)) {
            if (data.contains(member)) return true;
        }
        return false;
    }

    /**
     * Members of the player's FTB Teams party, or an empty set without FTB Teams.
     */
    static Set<UUID> teamMembers(UUID player) {
        if (!ModList.get().isLoaded("ftbteams")) return Set.of();
        try {
            return FtbTeamsCompat.teamMembers(player);
        } catch (LinkageError e) {
            // incompatible FTB Teams version: behave as if it were not installed
            return Set.of();
        }
    }

    public static boolean setExempt(MinecraftServer server, UUID player, boolean value) {
        boolean changed = ExemptionData.get(server).set(player, value);
        // Teammates' status may change too, so resync everyone online.
        if (changed) syncAll(server);
        return changed;
    }

    /**
     * Sends the player's exemption to their client if it differs from what was sent last.
     */
    public static void sync(ServerPlayer player) {
        boolean exempt = isExempt(player.server, player.getUUID());
        Boolean previous = lastSent.put(player.getUUID(), exempt);
        if (previous == null || previous != exempt) {
            LimiterNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ExemptSyncPacket(exempt));
        }
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }

    public static void forget(UUID player) {
        lastSent.remove(player);
    }
}
