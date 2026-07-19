package jackiecrazy.enchantlimiter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Files;
import java.util.*;

@Mod.EventBusSubscriber(modid = EnchantLimiter.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LimiterConfig {
    //that is, subtract damage by (d>40/(t+1))(d-40/(t+1))/2
    public static final LimiterConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;
    public static HashMap<Enchantment, EnchantInfo> map = new HashMap<>();
    public static HashMap<Item, EnchantInfo> customItems = new HashMap<>();
    public static List<Enchantment> blacklistedEnchantments = new ArrayList<>();
    public static boolean modEnabled = true;
    public static EnchantInfo DEFAULT = new EnchantInfo(0, 1);
    public static double pointsPerEnchantability, grain;
    public static double basePoint;

    // New crystal config data
    // These static fields were added to support "crystal" items that add enchantment points
    // when combined with an item in an anvil. The design decisions are:
    // - Crystals are optional and can be disabled globally via config.
    // - Each crystal item maps to a double value (how many enchantment points it provides).
    // - The blacklist supports three forms: exact item (modid:item), tag (tag:namespace:path),
    //   or wildcard namespace (modid:*). The parsing and checks are done in loadConfig and
    //   isCrystalBlacklisted respectively.
    // - Namespaced wildcards are stored as namespaces (strings) for quick membership checks.
    // - Tag entries are stored as ResourceLocations and checked against the Item's tags at runtime.
    public static boolean crystalsEnabled = true;
    public static Map<Item, Double> crystalValues = new HashMap<>();
    public static Set<ResourceLocation> crystalBlacklistItems = new HashSet<>();
    public static Set<String> crystalBlacklistNamespaces = new HashSet<>();
    public static Set<ResourceLocation> crystalBlacklistTags = new HashSet<>();
    // Tooltip configuration: whether to show only a single numeric value instead of the used/total bar,
    // and whether positive values should be colored red (otherwise white). Negative values are colored green.
    public static boolean tooltipOnlyNumber = false;
    public static boolean tooltipPositiveColorRed = true;

    static {
        final Pair<LimiterConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(LimiterConfig::new);
        CONFIG = specPair.getLeft();
        CONFIG_SPEC = specPair.getRight();
    }

    private final ForgeConfigSpec.DoubleValue ppe;
    private final ForgeConfigSpec.DoubleValue granularity;
    private final ForgeConfigSpec.DoubleValue basePoints, baseCost, incrementalCost;
    private final ForgeConfigSpec.BooleanValue modEnabledCfg;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> _customItems;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> _enchantDefinition;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> _blacklistedEnchantments;
    private final ForgeConfigSpec.BooleanValue crystalsEnabledCfg;
    private final ForgeConfigSpec.DoubleValue commonCrystalValueCfg;
    private final ForgeConfigSpec.DoubleValue uncommonCrystalValueCfg;
    private final ForgeConfigSpec.DoubleValue rareCrystalValueCfg;
    private final ForgeConfigSpec.DoubleValue legendaryCrystalValueCfg;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> _crystalBlacklist;
    // Tooltip config entries
    private final ForgeConfigSpec.BooleanValue tooltipOnlyNumberCfg;
    private final ForgeConfigSpec.BooleanValue tooltipPositiveColorRedCfg;

    public LimiterConfig(ForgeConfigSpec.Builder b) {
        b.push("general");
        modEnabledCfg = b.comment("Enable or disable all EnchantLimiter features globally. Default: true").define("enabled", true);
        b.pop();

        b.push("enchantability");
        ppe = b.comment("how much each point of enchantability in the item will add to its enchantment point pool. Default: 0.5").defineInRange("points per enchantability", 0.5, 0d, Double.MAX_VALUE);
        granularity = b.comment("if the number of enchantment points falls within this distance to a whole number, it will be rounded to the whole number instead. This allows you to have clean 1/3 or 1/7 for the incremental cost of enchantments.").defineInRange("granularity", 0.1, 0d, 1);
        basePoints = b.comment("how many enchantment points an item starts with. Default: 20").defineInRange("base points", 20, 0d, Double.MAX_VALUE);
        baseCost = b.comment("the default cost to add a new enchantment onto an item. Default: 0").defineInRange("base cost", 0, 0d, Double.MAX_VALUE);
        incrementalCost = b.comment("the default cost to add one level to any existing enchantment on the item. An enchantment with level 1 will apply both the base cost and the incremental cost, once each.").defineInRange("incremental cost", 1, 0, Double.MAX_VALUE);
        _customItems = b.comment("Items that have their own enchantment point cap. Format is name, base enchantability, increment per extra point of enchantability.").defineList("custom items", Collections.emptyList(), String.class::isInstance);
        _enchantDefinition = b.comment("Define enchantments here. Format is name, base cost, incremental cost.").defineList("enchantments", Arrays.asList("minecraft:mending, 5, 0", "minecraft:sharpness, 0, 1"), String.class::isInstance);
        _blacklistedEnchantments = b.comment("Enchantments that should be blacklisted. Format is name. Examples: [\"minecraft:mending\", \"minecraft:sharpness\"]").defineList("blacklisted enchantments", Collections.emptyList(), String.class::isInstance);
        b.pop();

        // Crystals config
        // These builder entries create the config options users will see in the server TOML.
        // Each crystal has its own configurable value so server admins can tune balance.
        // The crystal blacklist is a flexible list that may contain exact item ids (modid:item),
        // tags prefixed with "tag:", or wildcard namespaces (modid:*). The parsing of each
        // entry is handled in loadConfig().
        b.push("crystals");
        crystalsEnabledCfg = b.comment("Enable or disable crystal items entirely. Default: true").define("enable crystals", true);
        commonCrystalValueCfg = b.comment("Value for common crystal. Default: 1").defineInRange("common_crystal_value", 1.0, 0.0, Double.MAX_VALUE);
        uncommonCrystalValueCfg = b.comment("Value for uncommon crystal. Default: 3").defineInRange("uncommon_crystal_value", 3.0, 0.0, Double.MAX_VALUE);
        rareCrystalValueCfg = b.comment("Value for rare crystal. Default: 5").defineInRange("rare_crystal_value", 5.0, 0.0, Double.MAX_VALUE);
        legendaryCrystalValueCfg = b.comment("Value for legendary crystal. Default: 10").defineInRange("legendary_crystal_value", 10.0, 0.0, Double.MAX_VALUE);
        _crystalBlacklist = b.comment("Blacklist for crystals. Entries can be exact item (modid:item), tag (tag:namespace:path), or wildcard namespace (modid:*). Examples: [\"minecraft:diamond_sword\", \"YOUR_MOD_ID:custom_item\"]").defineList("crystal blacklist", Collections.emptyList(), String.class::isInstance);
        b.pop();

        // Tooltip options
        b.push("tooltip");
        tooltipOnlyNumberCfg = b.comment("Show only a single numeric amount in the tooltip instead of used/total. If true, enchanted items/books show the consumed points and non-enchanted items show the total. Default: true").define("show_only_number", true);
        tooltipPositiveColorRedCfg = b.comment("When true, positive point values are colored red; otherwise they are white. Negative values are colored green. Default: true").define("positive_color_red", true);
        b.pop();
        b.build();
    }

    @SubscribeEvent
    public static void loadConfig(ModConfigEvent e) {
        if (e.getConfig().getSpec() == CONFIG_SPEC) {
            try {
                // Create the config file only if it does not exist. This prevents overwriting existing files.
                java.nio.file.Path configFile = FMLPaths.CONFIGDIR.get().resolve(EnchantLimiter.MODID + "-server.toml");
                if (!Files.exists(configFile)) {
                    try {
                        e.getConfig().save();
                        EnchantLimiter.LOGGER.debug("enchantlimiter: default config created at {}", configFile);
                    } catch (Exception saveEx) {
                        EnchantLimiter.LOGGER.warn("Failed to write default config file: {}", saveEx.getMessage());
                    }
                }

                map.clear();
                customItems.clear();
                blacklistedEnchantments.clear();
                modEnabled = CONFIG.modEnabledCfg.get();
                pointsPerEnchantability = CONFIG.ppe.get();
                basePoint = CONFIG.basePoints.get();
                grain = CONFIG.granularity.get();
                DEFAULT = new EnchantInfo(basePoint, pointsPerEnchantability);

                // load enchant definitions
                List<? extends String> list = CONFIG._enchantDefinition.get();
                for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
                    double base = CONFIG.baseCost.get();
                    double incr = CONFIG.incrementalCost.get();
                    ResourceLocation enchKey = ForgeRegistries.ENCHANTMENTS.getKey(ench);
                    for (String element : list) {
                        if (enchKey != null && element.startsWith(enchKey.toString())) {
                            String[] split = element.split(",");
                            base = Double.parseDouble(split[1].trim());
                            incr = Double.parseDouble(split[2].trim());
                        }
                    }
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

                // crystals
                // Read the crystal section from the config and populate runtime structures.
                // We populate crystalValues map by looking up the crystal item registry keys and
                // mapping them to the configured double values. This lets other systems ask for
                // the numeric value for a given Item instance at runtime.
                crystalsEnabled = CONFIG.crystalsEnabledCfg.get();
                crystalValues.clear();
                crystalBlacklistItems.clear();
                crystalBlacklistNamespaces.clear();
                crystalBlacklistTags.clear();

                // Populate crystalValues from individual config entries
                try {
                    Item common = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enchantlimiter:common_crystal"));
                    if (common != null) crystalValues.put(common, CONFIG.commonCrystalValueCfg.get());
                    Item uncommon = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enchantlimiter:uncommon_crystal"));
                    if (uncommon != null) crystalValues.put(uncommon, CONFIG.uncommonCrystalValueCfg.get());
                    Item rare = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enchantlimiter:rare_crystal"));
                    if (rare != null) crystalValues.put(rare, CONFIG.rareCrystalValueCfg.get());
                    Item legendary = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enchantlimiter:legendary_crystal"));
                    if (legendary != null) crystalValues.put(legendary, CONFIG.legendaryCrystalValueCfg.get());
                } catch (Exception ex) {
                    EnchantLimiter.LOGGER.warn("Failed to populate crystal defaults: {}", ex.getMessage());
                }

                // Parse the blacklist entries. Supported formats:
                // - tag:namespace:path -> stored in crystalBlacklistTags as ResourceLocation
                // - modid:* -> stored as namespace in crystalBlacklistNamespaces
                // - modid:item -> stored in crystalBlacklistItems as ResourceLocation
                for (String s : CONFIG._crystalBlacklist.get()) {
                    String entry = s.trim();
                    try {
                        if (entry.startsWith("tag:")) {
                            String tagName = entry.substring(4);
                            crystalBlacklistTags.add(new ResourceLocation(tagName));
                        } else if (entry.endsWith(":*")) {
                            String ns = entry.substring(0, entry.length() - 2);
                            crystalBlacklistNamespaces.add(ns);
                        } else {
                            crystalBlacklistItems.add(new ResourceLocation(entry));
                        }
                    } catch (Exception ex) {
                        EnchantLimiter.LOGGER.warn("Invalid crystal blacklist entry: {}", entry);
                    }
                }

                // Tooltip runtime options
                tooltipOnlyNumber = CONFIG.tooltipOnlyNumberCfg.get();
                tooltipPositiveColorRed = CONFIG.tooltipPositiveColorRedCfg.get();

                EnchantLimiter.LOGGER.debug("enchantment limits loaded!");
            } catch (Exception validationException) {
                EnchantLimiter.LOGGER.fatal("Something broke while loading the enchantment config!.");
                EnchantLimiter.LOGGER.error("Exception while loading config", validationException);
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

    // helper for Anvil: check if this item stack is blacklisted for crystals
    // This method checks three sources of blacklist entries:
    // 1) exact item blacklist (stored as ResourceLocation)
    // 2) namespace wildcard blacklist (stored as Strings of namespaces)
    // 3) tag-based blacklist (stored as ResourceLocation tag ids)
    // Returns true if the provided ItemStack should not accept crystals.
    public static boolean isCrystalBlacklisted(ItemStack stack) {
        if (stack == null) return false;
        Item it = stack.getItem();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(it);
        if (key != null) {
            if (crystalBlacklistItems.contains(key)) return true;
            if (crystalBlacklistNamespaces.contains(key.getNamespace())) return true;
        }
        // check tags
        for (ResourceLocation rl : crystalBlacklistTags) {
            try {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, rl);
                if (stack.is(tagKey)) return true;
            } catch (Exception ex) {
                // ignore invalid tag entries
            }
        }
        return false;
    }

    // Return the configured numeric value for a crystal item, or the provided default if not configured.
    // This allows other systems to treat the crystal as a numeric modifier without needing to know
    // about registry names or config parsing details.
    public static double getCrystalValueForItem(Item item, double defaultVal) {
        return crystalValues.getOrDefault(item, defaultVal);
    }

    // Quick runtime check whether crystals are enabled globally. Used before showing UI/tooltips
    // or applying crystal logic.
    public static boolean areCrystalsEnabled() {
        return crystalsEnabled;
    }

    public static boolean isModEnabled() {
        return !modEnabled;
    }
}
