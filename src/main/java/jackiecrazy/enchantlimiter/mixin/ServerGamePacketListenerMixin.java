package jackiecrazy.enchantlimiter.mixin;

import jackiecrazy.enchantlimiter.exempt.PlayerContext;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Marks the player while the server handles a menu button click. Every enchanting table that
 * uses the vanilla button packet goes through here: the vanilla table, Apotheosis' table,
 * Enchanting Infuser and any other mod that overrides clickMenuButton, whether or not it calls
 * super. A redirect (instead of HEAD/RETURN injects) lets us pop in a finally block, so an
 * exception in some mod's menu can never leak an exempt player into later operations.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Redirect(method = "handleContainerButtonClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z"))
    private boolean enchantlimiter$clickWithPlayer(AbstractContainerMenu menu, Player clicker, int id) {
        Player previous = PlayerContext.push(clicker);
        try {
            return menu.clickMenuButton(clicker, id);
        } finally {
            PlayerContext.pop(previous);
        }
    }
}
