package jackiecrazy.enchantlimiter.mixin;

import jackiecrazy.enchantlimiter.exempt.PlayerContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the anvil's player while the result is computed. This covers vanilla's own
 * EnchantmentHelper.setEnchantments call and every AnvilUpdateEvent listener (fired from inside
 * createResult), on both the server and the client preview.
 * Extends ItemCombinerMenu only to reach its protected {@code player} field.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Unique
    private Player enchantlimiter$previous;

    private AnvilMenuMixin(MenuType<?> type, int id, Inventory inv, ContainerLevelAccess access) {
        super(type, id, inv, access);
    }

    @Inject(method = "createResult", at = @At("HEAD"))
    private void enchantlimiter$pushPlayer(CallbackInfo ci) {
        enchantlimiter$previous = PlayerContext.push(this.player);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void enchantlimiter$popPlayer(CallbackInfo ci) {
        PlayerContext.pop(enchantlimiter$previous);
        enchantlimiter$previous = null;
    }
}
