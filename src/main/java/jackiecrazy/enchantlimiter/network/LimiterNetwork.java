package jackiecrazy.enchantlimiter.network;

import jackiecrazy.enchantlimiter.EnchantLimiter;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class LimiterNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EnchantLimiter.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private LimiterNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ExemptSyncPacket.class, ExemptSyncPacket::encode, ExemptSyncPacket::decode,
                ExemptSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
