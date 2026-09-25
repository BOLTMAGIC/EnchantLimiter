package jackiecrazy.enchantlimiter.mixin.compat;

import jackiecrazy.enchantlimiter.exempt.PlayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Apotheosis compat: the Library of Alexandria extracts books through Placebo's own button packet
 * (not the vanilla one) and onButtonClick has no player. The menu is per-player, so the player is
 * remembered from the constructor and marked for the duration of onButtonClick, whose
 * EnchLibraryTile.extractEnchant ends in EnchantmentHelper.setEnchantments.
 * <p>
 * Apotheosis is not a compile dependency, hence @Pseudo, string targets, remap = false and
 * require = 0 (a changed Apotheosis just loses the exemption there, it never breaks loading).
 */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.ench.library.EnchLibraryContainer", remap = false)
public abstract class EnchLibraryContainerMixin {
    @Unique
    private Player enchantlimiter$player;
    @Unique
    private Player enchantlimiter$previous;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"), require = 0, remap = false)
    private void enchantlimiter$rememberPlayer(int id, Inventory inv, BlockPos pos, CallbackInfo ci) {
        enchantlimiter$player = inv.player;
    }

    @Inject(method = "onButtonClick(I)V", at = @At("HEAD"), require = 0, remap = false)
    private void enchantlimiter$pushPlayer(int id, CallbackInfo ci) {
        enchantlimiter$previous = PlayerContext.push(enchantlimiter$player);
    }

    @Inject(method = "onButtonClick(I)V", at = @At("RETURN"), require = 0, remap = false)
    private void enchantlimiter$popPlayer(int id, CallbackInfo ci) {
        PlayerContext.pop(enchantlimiter$previous);
        enchantlimiter$previous = null;
    }
}
