package jackiecrazy.enchantlimiter.exempt;

import jackiecrazy.enchantlimiter.EnchantLimiter;
import jackiecrazy.enchantlimiter.LimiterConfig;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnchantLimiter.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ExemptionEvents {
    // Periodic resync picks up FTB team joins/leaves without hooking FTB's own events.
    private static final int RESYNC_INTERVAL = 40;

    private ExemptionEvents() {
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (LimiterConfig.exemptAdvancements.contains(e.getAdvancement().getId())) {
            Exemptions.setExempt(sp.server, sp.getUUID(), true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        // Catch advancements earned before this feature (or the config entry) existed.
        for (ResourceLocation id : LimiterConfig.exemptAdvancements) {
            Advancement adv = sp.server.getAdvancements().getAdvancement(id);
            if (adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone()) {
                Exemptions.setExempt(sp.server, sp.getUUID(), true);
                break;
            }
        }
        Exemptions.forget(sp.getUUID());
        Exemptions.sync(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Exemptions.forget(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) PlayerContext.clear();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % RESYNC_INTERVAL == 0) Exemptions.sync(sp);
    }
}
