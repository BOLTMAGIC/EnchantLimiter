package jackiecrazy.enchantlimiter;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = EnchantLimiter.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AnvilEnchantHandler {

    // When combining an item with an enchanted book on an anvil, allow partial application:
    // - If the book would add more enchantment points than available, reduce the added enchant
    //   levels so the resulting item fits within the cap. The "extra" from the book is lost.
    // - Preference: reduce levels that came from the book first. If no added levels exist,
    //   fall back to greedily reducing any enchantments in the result until it fits.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilUpdate(AnvilUpdateEvent e) {
        if (LimiterConfig.isModEnabled()) return;

        ItemStack left = e.getLeft();
        ItemStack output = e.getOutput();
        if (left == null || left.isEmpty() || output == null || output.isEmpty()) return;

        Map<Enchantment, Integer> leftMap = EnchantmentHelper.getEnchantments(left);
        Map<Enchantment, Integer> outMap = EnchantmentHelper.getEnchantments(output);
        if (outMap.isEmpty()) return;

        double totalAllowed = EnchantLimiter.getTotalEnchantPoints(left);
        // compute used for output as starting point
        double used = 0;
        for (Map.Entry<Enchantment, Integer> entry : outMap.entrySet()) {
            used += EnchantLimiter.getRequiredEnchantPoints(entry.getKey(), entry.getValue());
        }

        if (used <= totalAllowed) return; // already fits

        // compute added levels (those above what's in left)
        Map<Enchantment, Integer> added = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : outMap.entrySet()) {
            Enchantment ench = entry.getKey();
            int outLvl = entry.getValue();
            int leftLvl = leftMap.getOrDefault(ench, 0);
            int add = outLvl - leftLvl;
            if (add > 0) added.put(ench, add);
        }

        // Work on a mutable result map
        Map<Enchantment, Integer> resultMap = new HashMap<>(outMap);

        // Helper to compute delta when decreasing an enchant from lvl -> lvl-1
        final java.util.function.BiFunction<Enchantment, Integer, Double> decreaseDelta = (ench, lvl) -> {
            double before = EnchantLimiter.getRequiredEnchantPoints(ench, lvl);
            double after = (lvl - 1) > 0 ? EnchantLimiter.getRequiredEnchantPoints(ench, lvl - 1) : 0;
            return before - after;
        };

        // Try to reduce added enchantments first, sorted by per-level cost descending
        List<Enchantment> addedList = new ArrayList<>(added.keySet());
        addedList.sort((a, b) -> Double.compare(EnchantLimiter.getRequiredEnchantPoints(b, 1), EnchantLimiter.getRequiredEnchantPoints(a, 1)));

        while (used > totalAllowed) {
            boolean reduced = false;
            // loop over added enchants and reduce one level where possible
            for (Enchantment ench : addedList) {
                Integer cur = resultMap.getOrDefault(ench, 0);
                int base = leftMap.getOrDefault(ench, 0);
                if (cur > base) {
                    double delta = decreaseDelta.apply(ench, cur);
                    // decrement
                    int newlvl = cur - 1;
                    if (newlvl > 0) resultMap.put(ench, newlvl);
                    else resultMap.remove(ench);
                    used -= delta;
                    reduced = true;
                    if (used <= totalAllowed) break;
                }
            }
            if (reduced) continue;

            // If nothing added could be reduced further, fall back to reducing any enchantment in result
            List<Enchantment> allEnchs = new ArrayList<>(resultMap.keySet());
            // sort by per-level cost descending
            allEnchs.sort((a, b) -> Double.compare(EnchantLimiter.getRequiredEnchantPoints(b, 1), EnchantLimiter.getRequiredEnchantPoints(a, 1)));
            boolean any = false;
            for (Enchantment ench : allEnchs) {
                int cur = resultMap.getOrDefault(ench, 0);
                if (cur <= 0) continue;
                double delta = decreaseDelta.apply(ench, cur);
                int newlvl = cur - 1;
                if (newlvl > 0) resultMap.put(ench, newlvl);
                else resultMap.remove(ench);
                used -= delta;
                any = true;
                if (used <= totalAllowed) break;
            }
            if (!any) break; // cannot reduce further
        }

        // Build adjusted ItemStack result
        ItemStack adjusted = output.copy();
        EnchantmentHelper.setEnchantments(resultMap, adjusted);

        e.setOutput(adjusted);
        // leave cost/material as provided by other logic
    }
}
