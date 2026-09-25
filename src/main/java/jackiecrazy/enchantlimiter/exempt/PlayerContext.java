package jackiecrazy.enchantlimiter.exempt;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Remembers which player is currently performing an enchanting operation.
 * <p>
 * EnchantmentHelper.setEnchantments and ItemStack.enchant have no player parameter, so the
 * menus that know the player (enchanting table, anvil) push it here for the duration of the
 * operation. Everything without a player (loot, villager trades, commands) sees null and
 * stays limited.
 */
public final class PlayerContext {
    private static final ThreadLocal<Player> CURRENT = new ThreadLocal<>();

    private PlayerContext() {
    }

    /**
     * Sets the acting player and returns the previous one, which must be passed to {@link #pop}.
     */
    @Nullable
    public static Player push(Player player) {
        Player previous = CURRENT.get();
        CURRENT.set(player);
        return previous;
    }

    public static void pop(@Nullable Player previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    @Nullable
    public static Player current() {
        return CURRENT.get();
    }

    /**
     * Safety net, called at the start of every server tick: if an exception ever skipped a pop,
     * the stale player must not keep exempting unrelated operations.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
