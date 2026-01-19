package jackiecrazy.enchantlimiter.mixin;

import jackiecrazy.enchantlimiter.EnchantLimiter;
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
 * and reduces the level if necessary.
 *
 * This mixin was simplified to avoid unstable LocalCapture uses and mapping errors.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract void enchant(Enchantment ench, int level);

    @Inject(method = "enchant", at = @At("HEAD"), cancellable = true)
    private void enchant(Enchantment ench, int level, CallbackInfo ci) {
        ItemStack i = (ItemStack) (Object) this;
        if (EnchantLimiter.getTotalEnchantPoints(i) - EnchantLimiter.getUsedEnchantPoints(i) < EnchantLimiter.getRequiredEnchantPoints(ench, level)) {
            if (level > 1)
                enchant(ench, level - 1);
            ci.cancel();
        }
    }
}
