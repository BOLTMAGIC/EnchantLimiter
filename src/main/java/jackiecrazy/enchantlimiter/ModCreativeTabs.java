package jackiecrazy.enchantlimiter;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ModCreativeTabs {
    // Deferred register for creative tabs. This one creates a custom tab for the mod's crystals
    // and other related items so they always appear together in the creative inventory.
    // Design notes:
    // - The tab icon is set to an Enchantment Table to indicate the tab's theme.
    // - Items are added via displayItems; this list should include the crystal items so they
    //   are always visible in the creative tab even if they are obtainable via /give as well.
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnchantLimiter.MODID);

    public static final RegistryObject<CreativeModeTab> ENCHANT_LIMITER_TAB = CREATIVE_TABS.register("enchant_limiter",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.ENCHANTING_TABLE)) // Tab icon: Enchantment Table
                    .title(Component.translatable("itemGroup.enchantlimiter"))
                    .displayItems((params, output) -> {
                        // Add the mod crystal items so they always appear in this tab.
                        output.accept(ItemInit.COMMON_CRYSTAL.get());
                        output.accept(ItemInit.UNCOMMON_CRYSTAL.get());
                        output.accept(ItemInit.RARE_CRYSTAL.get());
                        output.accept(ItemInit.LEGENDARY_CRYSTAL.get());
                    })
                    .build());

    public static void register() {
        CREATIVE_TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
