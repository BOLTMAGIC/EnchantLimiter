package jackiecrazy.enchantlimiter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = EnchantLimiter.MODID, value = Dist.CLIENT)
public class ItemTooltipHandler {
    // Adds enchantment point information to item tooltips when appropriate.
    // Design notes:
    // - Only certain item types should show the enchantment points: swords, all tools (TieredItem),
    //   armor, shields, bows, crossbows, and tridents. This avoids showing the tooltip on random items.
    // - If an item has extraEnchantPoints in NBT (from a crystal), that value is shown.
    // - If an item is blacklisted for crystals, a blacklist message is shown instead.
    @SubscribeEvent(priority = EventPriority.HIGH)//by marking it high, this will appear AFTER ench desc
    public static void tooltip(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();

        String rejectKey = null;
        if (stack.hasTag()) {
            net.minecraft.nbt.CompoundTag t = stack.getTag();
            if (t != null && t.contains("el_crystal_reject")) {
                rejectKey = t.getString("el_crystal_reject");
            }
        }

        boolean hasExtraTag = false;
        if (stack.hasTag()) {
            net.minecraft.nbt.CompoundTag t = stack.getTag();
            hasExtraTag = (t != null && t.contains("extraEnchantPoints"));
        }

        boolean isEnchanted = stack.isEnchanted() || stack.getItem() instanceof EnchantedBookItem;
        // Show only for swords, tiered tools (pick/axe/shovel/hoe/etc.), armor, shields, bows, crossbows, tridents or items with the crystal tag or already enchanted
        boolean isSword = stack.getItem() instanceof net.minecraft.world.item.SwordItem;
        boolean isTieredTool = stack.getItem() instanceof net.minecraft.world.item.TieredItem;
        boolean isArmor = stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
        boolean isShield = stack.getItem() instanceof net.minecraft.world.item.ShieldItem;
        boolean isBow = stack.getItem() instanceof net.minecraft.world.item.BowItem;
        boolean isCrossbow = stack.getItem() instanceof net.minecraft.world.item.CrossbowItem;
        boolean isTrident = stack.getItem() instanceof net.minecraft.world.item.TridentItem;

        boolean shouldShowPoints = hasExtraTag || isEnchanted || isSword || isTieredTool || isArmor || isShield || isBow || isCrossbow || isTrident;

        // Always show enchantment points for allowed items
        if (shouldShowPoints) {
            double used = EnchantLimiter.getUsedEnchantPoints(stack);
            double total = EnchantLimiter.getTotalEnchantPoints(stack);
            MutableComponent amountComp;
            if (isEnchanted || stack.getItem() instanceof EnchantedBookItem) {
                amountComp = Component.literal(formatDouble(used) + "/" + formatDouble(total)).withStyle(used > total ? ChatFormatting.RED : ChatFormatting.RESET);
            } else {
                amountComp = Component.literal(formatDouble(total));
            }
            e.getToolTip().add(Component.translatable("enchantlimiter.points", amountComp));
        }

        // If there was a rejection reason, show it (but don't suppress the points)
        if (rejectKey != null) {
            e.getToolTip().add(Component.translatable(rejectKey).withStyle(ChatFormatting.RED));
            // do not return; keep showing other info (points already shown above)
        }

        // If the item has extraEnchantPoints (from a crystal), don't show additional cost/shift lines
        if (hasExtraTag) {
            return;
        }

        // For enchanted books show the description on SHIFT, otherwise show the SHIFT hint only for books
        if (stack.getItem() instanceof EnchantedBookItem) {
            if (Screen.hasShiftDown())
                insertDescriptionTooltips(e.getToolTip(), stack);
            else e.getToolTip().add(Component.translatable("enchantlimiter.shift"));
        }
    }

    private static String formatDouble(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }

    private static void insertDescriptionTooltips(List<Component> tips, ItemStack stack) {

        final Iterator<Map.Entry<Enchantment, Integer>> enchants = EnchantmentHelper.getEnchantments(stack).entrySet().iterator();
        while (enchants.hasNext()) {
            final Map.Entry<Enchantment, Integer> entry = enchants.next();
            Enchantment enchant = entry.getKey();
            final ListIterator<Component> tooltips = tips.listIterator();
            while (tooltips.hasNext()) {
                final Component component = tooltips.next();
                if (component instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc && tc.getKey().equals(enchant.getDescriptionId())) {
                    final LimiterConfig.EnchantInfo info = LimiterConfig.map.getOrDefault(enchant, LimiterConfig.DEFAULT);
                    tooltips.add(Component.translatable("enchantlimiter.tip", info.getBase(), info.getIncrement(), entry.getValue(), EnchantLimiter.getRequiredEnchantPoints(enchant, entry.getValue())));
                    if (enchants.hasNext()) {
                        tooltips.add(Component.empty());
                    }
                    break;
                }
            }
        }
    }
}
