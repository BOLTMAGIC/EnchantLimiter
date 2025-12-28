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
    @SubscribeEvent(priority = EventPriority.HIGH)//by marking it high, this will appear AFTER ench desc
    public static void tooltip(ItemTooltipEvent e) {
        if (e.getItemStack().isEnchanted() || e.getItemStack().getItem() instanceof EnchantedBookItem) {
            final double used = EnchantLimiter.getUsedEnchantPoints(e.getItemStack());
            final double total = EnchantLimiter.getTotalEnchantPoints(e.getItemStack());
            e.getToolTip().add(Component.translatable("enchantlimiter.points", (used > total ? ChatFormatting.RED : "") + "" + used + "" + ChatFormatting.RESET + "/" + total));
        }
        if (e.getItemStack().getItem() instanceof EnchantedBookItem) {
            if (Screen.hasShiftDown())
                insertDescriptionTooltips(e.getToolTip(), e.getItemStack());
            else e.getToolTip().add(Component.translatable("enchantlimiter.shift"));
        }
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
