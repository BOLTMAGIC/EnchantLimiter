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
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public EnchantLimiter() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, LimiterConfig.CONFIG_SPEC);
    }

    public static double getTotalEnchantPoints(ItemStack stack) {
        double ret=(stack.getEnchantmentValue() * LimiterConfig.pointsPerEnchantability) + LimiterConfig.basePoint;
        if (LimiterConfig.customItems.containsKey(stack.getItem())) {
             ret = LimiterConfig.customItems.get(stack.getItem()).getBase();
            ret += (stack.getEnchantmentValue() * LimiterConfig.customItems.get(stack.getItem()).getIncrement());
        }
        if(stack.hasTag())
            ret+=stack.getTag().getDouble("extraEnchantPoints");
        return ret;
    }

    public static double getUsedEnchantPoints(ItemStack stack) {
        double ret = 0;
        for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            ret += getRequiredEnchantPoints(e.getKey(), e.getValue());
        }
        return ret; // Rundung entfernt, um Dezimalstellen zu erhalten
    }

    public static double getRequiredEnchantPoints(Enchantment e, int i) {
        LimiterConfig.EnchantInfo ei=LimiterConfig.map.getOrDefault(e, LimiterConfig.DEFAULT);
        double ret = ei.getBase() + (ei.getIncrement() * i);
        // Rundung entfernt, um Dezimalstellen zu erhalten
        return ret;
    }
}
