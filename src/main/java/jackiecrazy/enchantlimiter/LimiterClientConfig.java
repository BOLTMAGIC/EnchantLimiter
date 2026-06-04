package jackiecrazy.enchantlimiter;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = EnchantLimiter.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LimiterClientConfig {
    public static final LimiterClientConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;

    private final ForgeConfigSpec.BooleanValue tooltipOnlyNumberCfg;
    private final ForgeConfigSpec.BooleanValue tooltipPositiveColorRedCfg;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CONFIG = new LimiterClientConfig(builder);
        CONFIG_SPEC = builder.build();
    }

    private LimiterClientConfig(ForgeConfigSpec.Builder b) {
        b.push("tooltip");
        tooltipOnlyNumberCfg = b.comment("Show only a single numeric amount in the tooltip for books instead of used/total. Default: false").define("show_only_number", false);
        tooltipPositiveColorRedCfg = b.comment("When true, positive point values are colored red; otherwise they are white. Negative values are colored green. Default: true").define("positive_color_red", true);
        b.pop();
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void loadConfig(ModConfigEvent e) {
        if (e.getConfig().getSpec() == CONFIG_SPEC) {
            try {
                LimiterConfig.tooltipOnlyNumber = CONFIG.tooltipOnlyNumberCfg.get();
                LimiterConfig.tooltipPositiveColorRed = CONFIG.tooltipPositiveColorRedCfg.get();
            } catch (Exception ex) {
                EnchantLimiter.LOGGER.warn("Failed to load client tooltip config: {}", ex.getMessage());
            }
        }
    }
}

