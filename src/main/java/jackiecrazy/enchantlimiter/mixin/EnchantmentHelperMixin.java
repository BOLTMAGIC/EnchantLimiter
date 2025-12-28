package jackiecrazy.enchantlimiter.mixin;

import jackiecrazy.enchantlimiter.EnchantLimiter;
import jackiecrazy.enchantlimiter.LimiterConfig;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.HashMap;
import java.util.Map;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    /**
     * @author Jackiecrazy
     * @reason yes
     */
    @Overwrite()
    public static void setEnchantments(Map<Enchantment, Integer> enchMap, ItemStack stack) {
        Map<Enchantment, Integer> filteredMap = new HashMap<>(enchMap);
        if (stack.getItem() instanceof EnchantedBookItem) {
            filteredMap.keySet().removeIf(LimiterConfig.blacklistedEnchantments::contains);
        }
        ListTag listnbt = new ListTag();
        double accumulated = 0;
        double max = EnchantLimiter.getTotalEnchantPoints(stack);
        //iterate over all negative values first to make space
        for (Map.Entry<Enchantment, Integer> entry : filteredMap.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment != null) {
                int i = entry.getValue();
                //only iterating negatives
                if (EnchantLimiter.getRequiredEnchantPoints(enchantment, i) >= 0) continue;
                listnbt.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(enchantment), i));
                if (stack.getItem() instanceof EnchantedBookItem) {
                    EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, i));
                }
                accumulated += EnchantLimiter.getRequiredEnchantPoints(enchantment, i);
            }
        }
        //iterate over all positive values, and add as much as the enchantability allows
        for (Map.Entry<Enchantment, Integer> entry : filteredMap.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment != null) {
                int i = entry.getValue();
                //negatives have already been iterated
                if (EnchantLimiter.getRequiredEnchantPoints(enchantment, i) < 0) continue;
                while (i >= 1) {
                    if (accumulated + EnchantLimiter.getRequiredEnchantPoints(enchantment, i) > max) {
                        i--;
                    } else break;
                }
                if (i <= 0) continue;
                listnbt.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(enchantment), i));
                if (stack.getItem() instanceof EnchantedBookItem) {
                    EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, i));
                }
                accumulated += EnchantLimiter.getRequiredEnchantPoints(enchantment, i);
            }
        }

        if (listnbt.isEmpty()) {
            stack.removeTagKey("Enchantments");
        } else if (stack.getItem() != Items.ENCHANTED_BOOK) {
            stack.addTagElement("Enchantments", listnbt);
        }
    }
}
