package jackiecrazy.enchantlimiter.mixin;

import jackiecrazy.enchantlimiter.EnchantLimiter;
import jackiecrazy.enchantlimiter.LimiterConfig;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minimal ItemStack mixin that prevents enchant applications that would exceed
 * the configured enchantment point limit. It injects at the head of ItemStack.enchant(...)
 * and cancels applications that don't fit.
 *
 * SERVER-SIDE FIX: This mixin is critical for server-side validation. When a player
 * attempts to enchant an item via an enchanting table on a server, this mixin ensures
 * the enchantment respects the point limits configured in the server config.
 *
 * BUG FIX: On dedicated servers, the server config must be loaded before enchantments
 * are allowed. This prevents enchantments from being applied with default/incomplete values.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "enchant", at = @At("HEAD"), cancellable = true)
    private void validateEnchant(Enchantment ench, int level, CallbackInfo ci) {
        // Only validate if mod is enabled and level is positive
        if (!LimiterConfig.isModEnabled() || level <= 0) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        double total = EnchantLimiter.getTotalEnchantPoints(stack);
        double used = EnchantLimiter.getUsedEnchantPoints(stack);
        double available = total - used;
        double required = EnchantLimiter.getRequiredEnchantPoints(ench, level);

        // If the requested level doesn't fit, cancel the enchantment
        // This prevents the enchantment from being applied on both client and server
        if (available < required) {
            ci.cancel();
        }
    }
}
