package jackiecrazy.enchantlimiter;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

// CrystalItem represents a small consumable item that grants extra enchantment points
// when combined with a tool/armor in an anvil. Design notes:
// - Each instance holds a numeric 'extraPoints' value exposed via getExtraPoints().
// - The item itself is ordinary otherwise; the anvil handler consumes one item and
//   writes the configured value into the target item's NBT under "extraEnchantPoints".
// - Crystals are intended to be single-use and not cumulative. The anvil logic will
//   reject applying a smaller or equal crystal if the target already has a crystal value.
public class CrystalItem extends Item {
    private final double extraPoints;

    public CrystalItem(double extraPoints, Properties properties) {
        super(properties);
        this.extraPoints = extraPoints;
    }

    public double getExtraPoints() {
        return extraPoints;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        // Show a crystal-specific tooltip line instead of generic SHIFT/Cost lines
        if (extraPoints == 1) {
            tooltip.add(Component.translatable("item.enchantlimiter.tooltip.common"));
        } else if (extraPoints == 3) {
            tooltip.add(Component.translatable("item.enchantlimiter.tooltip.uncommon"));
        } else if (extraPoints == 5) {
            tooltip.add(Component.translatable("item.enchantlimiter.tooltip.rare"));
        } else if (extraPoints == 10) {
            tooltip.add(Component.translatable("item.enchantlimiter.tooltip.legendary"));
        } else {
            tooltip.add(Component.translatable("enchantlimiter.points", String.valueOf((int)extraPoints)));
        }
    }

}
