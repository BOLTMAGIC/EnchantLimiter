package jackiecrazy.enchantlimiter;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


@Mod(EnchantLimiter.MODID)
public class EnchantLimiter {
    public static final String MODID = "enchantlimiter";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public EnchantLimiter() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, LimiterConfig.CONFIG_SPEC);
        // Client-side config for tooltip display (so clients can toggle the visual behavior locally)
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LimiterClientConfig.CONFIG_SPEC);
        // Register items and other registries
        ItemInit.register();
        ModCreativeTabs.register();
    }

    // Compute total enchant points for display/limits.
    // Design notes:
    // - If an item has an NBT tag "extraEnchantPoints", it overrides/adds to the base calculation
    //   and represents points granted by a crystal. The tooltip code uses this to always display
    //   the crystal-sourced value. This field is not cumulative across multiple crystals; the
    //   Anvil handler will ensure only the highest crystal value is stored here.
    public static double getTotalEnchantPoints(ItemStack stack) {
        double ret=(stack.getEnchantmentValue() * LimiterConfig.pointsPerEnchantability) + LimiterConfig.basePoint;
        ret=(stack.getEnchantmentValue() * LimiterConfig.pointsPerEnchantability) + LimiterConfig.basePoint;
        if (LimiterConfig.customItems.containsKey(stack.getItem())) {
             ret = LimiterConfig.customItems.get(stack.getItem()).getBase();
            ret += (stack.getEnchantmentValue() * LimiterConfig.customItems.get(stack.getItem()).getIncrement());
        }
        if(stack.hasTag()) {
            assert stack.getTag() != null;
            ret+=stack.getTag().getDouble("extraEnchantPoints");
        }
        return ret;
    }

    public static double getUsedEnchantPoints(ItemStack stack) {
        double ret = 0;
        for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            ret += getRequiredEnchantPoints(e.getKey(), e.getValue());
        }
        return ret;
    }

    public static double getRequiredEnchantPoints(Enchantment e, int i) {
        LimiterConfig.EnchantInfo ei=LimiterConfig.map.getOrDefault(e, LimiterConfig.DEFAULT);
        return ei.getBase() + (ei.getIncrement() * i);
    }
}
