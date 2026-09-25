package jackiecrazy.enchantlimiter.network;

import jackiecrazy.enchantlimiter.exempt.Exemptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells the client whether the local player is exempt, so anvil previews and tooltips match the
 * server's result.
 */
public record ExemptSyncPacket(boolean exempt) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(exempt);
    }

    public static ExemptSyncPacket decode(FriendlyByteBuf buf) {
        return new ExemptSyncPacket(buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> Exemptions.clientExempt = exempt);
        ctx.get().setPacketHandled(true);
    }
}
