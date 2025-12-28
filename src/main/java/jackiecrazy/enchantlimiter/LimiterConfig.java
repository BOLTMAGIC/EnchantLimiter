package jackiecrazy.enchantlimiter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Mod.EventBusSubscriber(modid = EnchantLimiter.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LimiterConfig {
    //that is, subtract damage by (d>40/(t+1))(d-40/(t+1))/2
    public static final LimiterConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;
    public static HashMap<Enchantment, EnchantInfo> map = new HashMap<>();
    public static HashMap<Item, EnchantInfo> customItems = new HashMap<>();
    public static List<Enchantment> blacklistedEnchantments = new ArrayList<>();
    public static EnchantInfo DEFAULT = new EnchantInfo(0, 1);
    public static double pointsPerEnchantability, grain;
    public static double basePoint;

    static {
        final Pair<LimiterConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(LimiterConfig::new);
        CONFIG = specPair.getLeft();
        CONFIG_SPEC = specPair.getRight();
    }

    private final ForgeConfigSpec.DoubleValue ppe;
    private final ForgeConfigSpec.DoubleValue granularity;
    private final ForgeConfigSpec.DoubleValue basePoints, baseCost, incrementalCost;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> _customItems;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> _enchantDefinition;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> _blacklistedEnchantments;

    public LimiterConfig(ForgeConfigSpec.Builder b) {
        b.push("enchantability");
        ppe = b.comment("how much each point of enchantability in the item will add to its enchantment point pool").defineInRange("points per enchantability", 0.5, 0d, Double.MAX_VALUE);
        granularity = b.comment("if the number of enchantment points falls within this distance to a whole number, it will be rounded to the whole number instead. This allows you to have clean 1/3 or 1/7 for the incremental cost of enchantments.").defineInRange("granularity", 0.1, 0d, 1);
        basePoints = b.comment("how many enchantment points an item starts with").defineInRange("base points", 10, 0d, Double.MAX_VALUE);
        baseCost = b.comment("the default cost to add a new enchantment onto an item.").defineInRange("base cost", 0, 0d, Double.MAX_VALUE);
        incrementalCost = b.comment("the default cost to add one level to any existing enchantment on the item. An enchantment with level 1 will apply both the base cost and the incremental cost, once each.").defineInRange("incremental cost", 1, 0, Double.MAX_VALUE);
        _customItems = b.comment("Items that have their own enchantment point cap. Format is name, base enchantability, increment per extra point of enchantability.").defineList("custom items", Arrays.asList("minecraft:fishing_rod, 20, 0.5", "minecraft:bow, 15, 1", "minecraft:enchanted_book, 30, 0"), String.class::isInstance);
        _enchantDefinition = b.comment("It's really irritating how nightconfig doesn't keep the entries in separate lines, but alas. Define enchantments here. Format is name, base cost, incremental cost.").defineList("enchantments", Arrays.asList("minecraft:mending, 5, 0", "minecraft:sharpness, 0, 1"), String.class::isInstance);
        _blacklistedEnchantments = b.comment("Enchantments that should be blacklisted. Format is name. Examples: [\"minecraft:mending\", \"minecraft:sharpness\"]").defineList("blacklisted enchantments", Arrays.asList(), String.class::isInstance);
        b.pop();
//        b.push("total enchantment cost is calculated as cost to apply+(additional cost per level*level), so for an enchantment that only has 1 level (eg mending) you can leave either of the two as 0");
//        b.pop();
//        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
//            b.define(ench.getRegistryName().toString() + ".cost to apply", 0);
//            b.define(ench.getRegistryName().toString() + ".additional cost per level", 1);
//        }
//        b.comment("define the number of points an enchantment will take per level. Enchantments not described here will default to 1 point per level.").push("enchantments");
//        b.define("minecraft:mending", 5);
//        b.pop();
        b.build();
    }

    private static String readAllBytesJava7(String filePath) {
        String content = "";

        try {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }

    @SubscribeEvent
    public static void loadConfig(ModConfigEvent e) {
        if (e.getConfig().getSpec() == CONFIG_SPEC) {
            try {
                map.clear();
                customItems.clear();
                blacklistedEnchantments.clear();
                pointsPerEnchantability = CONFIG.ppe.get();
                basePoint = CONFIG.basePoints.get();
                grain = CONFIG.granularity.get();
                DEFAULT = new EnchantInfo(basePoint, pointsPerEnchantability);
//                Gson gson=new Gson();
//                File f=new File(FMLPaths.CONFIGDIR+"/enchantlimiter.json");
//                String parse=readAllBytesJava7(f.getPath());
                List<? extends String> list = CONFIG._enchantDefinition.get();
                for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
                    double base = CONFIG.baseCost.get();
                    double incr = CONFIG.incrementalCost.get();
                    for (String element : list) {
                        if (element.startsWith(ForgeRegistries.ENCHANTMENTS.getKey(ench).toString())) {
                            String[] split = element.split(",");
                            base = Double.parseDouble(split[1].trim());
                            incr = Double.parseDouble(split[2].trim());
                        }
                    }
//                    final String name = ench.getRegistryName().toString();
//                    e.getConfig().getConfigData().add(name + ".cost to apply", e.getConfig().getConfigData().getIntOrElse(name + ".cost to apply", 0));
//                    int base = e.getConfig().getConfigData().getIntOrElse(name + ".cost to apply", 0);
//                    e.getConfig().getConfigData().add(name + ".additional cost per level", e.getConfig().getConfigData().getIntOrElse(name + ".additional cost per level", 1));
//                    int incr = e.getConfig().getConfigData().getIntOrElse(name + ".additional cost per level", 1);
                    map.put(ench, new EnchantInfo(base, incr));
                }
                for (String s : CONFIG._customItems.get()) {
                    String[] split = s.split(",");
                    customItems.put(ForgeRegistries.ITEMS.getValue(new ResourceLocation(split[0].trim())), new EnchantInfo(Integer.parseInt(split[1].trim()), Double.parseDouble(split[2].trim())));
                }
                for (String s : CONFIG._blacklistedEnchantments.get()) {
                    Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(s.trim()));
                    if (ench != null) {
                        blacklistedEnchantments.add(ench);
                    }
                }
                EnchantLimiter.LOGGER.debug("enchantment limits loaded!");
            } catch (Exception validationException) {
                EnchantLimiter.LOGGER.fatal("Something broke while loading the enchantment config!.");
                validationException.printStackTrace();
            }
        }
    }

    public static class EnchantInfo {
        private final double base;
        private final double increment;

        EnchantInfo(double base, double increment) {
            this.base = base;
            this.increment = increment;
        }

        public double getIncrement() {
            return increment;
        }

        public double getBase() {
            return base;
        }
    }
}
