package jackiecrazy.enchantlimiter;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EnchantLimiter.MODID);

    // Crystal items: these are small items intended to be combined with tools/armors via an anvil
    // to increase their enchantment point pool. Design notes:
    // - Each crystal is single-use (consumed when applied) and cannot be applied multiple times.
    // - Crystals are NOT cumulative: when multiple crystals would apply, only the highest-value
    //   crystal counts. The anvil logic enforces this and will prevent applying a smaller or equal
    //   crystal on top of a larger/equal one.
    // - Max stack size is 64 as configured here.
    // - Textures/models are already present in resources; these registrations only expose the items.
    public static final RegistryObject<Item> COMMON_CRYSTAL = ITEMS.register("common_crystal",
            () -> new CrystalItem(1, new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> UNCOMMON_CRYSTAL = ITEMS.register("uncommon_crystal",
            () -> new CrystalItem(3, new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> RARE_CRYSTAL = ITEMS.register("rare_crystal",
            () -> new CrystalItem(5, new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> LEGENDARY_CRYSTAL = ITEMS.register("legendary_crystal",
            () -> new CrystalItem(10, new Item.Properties().stacksTo(64)));

    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
