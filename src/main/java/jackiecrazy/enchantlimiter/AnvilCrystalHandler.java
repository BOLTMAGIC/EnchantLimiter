package jackiecrazy.enchantlimiter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = EnchantLimiter.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AnvilCrystalHandler {

    // Handles applying crystals on an anvil. Key design points:
    // - Input: left ItemStack (target item), right ItemStack (crystal)
    // - Output: modified ItemStack as result of anvil combination
    // - Crystals are single-use and not cumulative. If the target already has a crystal
    //   with the same or larger value, the handler rejects the application and sets
    //   a rejection tag so the UI can display a tooltip (uses lang key enchantlimiter.cannot_apply_smaller).
    // - Blacklist checks are performed via LimiterConfig.isCrystalBlacklisted before allowing
    //   application.
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent e) {
        if (!LimiterConfig.isModEnabled()) return;

        ItemStack left = e.getLeft();
        ItemStack right = e.getRight();
        if (left.isEmpty() || right.isEmpty()) return;

        boolean leftIsCrystal = left.getItem() instanceof CrystalItem;
        boolean rightIsCrystal = right.getItem() instanceof CrystalItem;

        // If both are crystals or both are non-crystals, do nothing here
        if (leftIsCrystal == rightIsCrystal) return;

        // Identify target (the item to receive points) and the crystal item
        ItemStack targetStack;
        CrystalItem crystalItem;
        Item crystalItemItem;
        if (rightIsCrystal) {
            targetStack = left.copy();
            crystalItem = (CrystalItem) right.getItem();
            crystalItemItem = right.getItem();
        } else {
            targetStack = right.copy();
            crystalItem = (CrystalItem) left.getItem();
            crystalItemItem = left.getItem();
        }

        // Check if crystals are enabled
        if (!LimiterConfig.areCrystalsEnabled()) {
            // Show a preview with rejection reason but make the anvil operation impossible
            // by setting an extremely high level and material cost. This prevents the
            // player from taking the output (and thus prevents any crystal from being consumed),
            // while still allowing the UI to display the rejection tooltip.
            ItemStack preview = targetStack.copy();
            CompoundTag t = preview.getOrCreateTag();
            t.putString("el_crystal_reject", "enchantlimiter.crystals_disabled");
            preview.setTag(t);
            e.setOutput(preview);
            e.setCost(100000); // prohibitively high cost
            e.setMaterialCost(64000); // require more materials than a normal stack
            return;
        }

        // Inline blacklist check (avoid signature mismatch)
        boolean targetBlacklisted = false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(targetStack.getItem());
        if (key != null) {
            if (LimiterConfig.crystalBlacklistItems.contains(key)) targetBlacklisted = true;
            if (LimiterConfig.crystalBlacklistNamespaces.contains(key.getNamespace())) targetBlacklisted = true;
        }
        if (!targetBlacklisted) {
            for (ResourceLocation rl : LimiterConfig.crystalBlacklistTags) {
                try {
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, rl);
                    if (targetStack.is(tagKey)) {
                        targetBlacklisted = true;
                        break;
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        if (targetBlacklisted) {
            // As above, show rejection in the preview but make the operation impossible
            ItemStack preview = targetStack.copy();
            CompoundTag t = preview.getOrCreateTag();
            t.putString("el_crystal_reject", "enchantlimiter.cannot_apply_blacklist");
            preview.setTag(t);
            e.setOutput(preview);
            e.setCost(100000);
            e.setMaterialCost(64000);
            return;
        }

        // Determine effective crystal value (config override allowed)
        double crystalValue = LimiterConfig.getCrystalValueForItem(crystalItemItem, crystalItem.getExtraPoints());

        // If the target already has extraEnchantPoints, do not apply another crystal (no cumulative application)
        CompoundTag existingTag = targetStack.getTag();
        if (existingTag != null && existingTag.contains("extraEnchantPoints")) {
            double existing = existingTag.getDouble("extraEnchantPoints");
            // If trying to apply a crystal that is less-than-or-equal to existing, show rejection tooltip in preview
            if (crystalValue <= existing) {
                // Show the cannot-apply message but make the anvil operation impossible so
                // crystals are not consumed when the player attempts to apply an equal or
                // smaller crystal.
                ItemStack preview = targetStack.copy();
                CompoundTag t = preview.getOrCreateTag();
                t.putString("el_crystal_reject", "enchantlimiter.cannot_apply_smaller");
                preview.setTag(t);
                e.setOutput(preview);
                e.setCost(100000);
                e.setMaterialCost(64000);
                return;
            }
            // crystalValue > existing: allow replacing existing with larger crystal
        }

        CompoundTag tag = targetStack.getOrCreateTag();
        tag.putDouble("extraEnchantPoints", crystalValue);
        targetStack.setTag(tag);

        e.setOutput(targetStack);
        // Consume only a single crystal regardless of stack size
        e.setCost(1);
        e.setMaterialCost(1);
    }
}
