package jackiecrazy.enchantlimiter;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = EnchantLimiter.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("enchantlimiter")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("enable")
                                .executes(ctx -> setEnabled(ctx.getSource(), true)))
                        .then(Commands.literal("disable")
                                .executes(ctx -> setEnabled(ctx.getSource(), false)))
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    boolean enabled = LimiterConfig.isModEnabled();
                                    ctx.getSource().sendSuccess(() -> Component.literal("EnchantLimiter is currently " + (enabled ? "enabled" : "disabled") + "."), false);
                                    return enabled ? 1 : 0;
                                }))
        );
    }

    private static int setEnabled(net.minecraft.commands.CommandSourceStack source, boolean enabled) {
        LimiterConfig.setModEnabled(enabled);
        source.sendSuccess(() -> Component.literal("EnchantLimiter has been " + (enabled ? "enabled" : "disabled") + "."), true);
        return enabled ? 1 : 0;
    }
}

