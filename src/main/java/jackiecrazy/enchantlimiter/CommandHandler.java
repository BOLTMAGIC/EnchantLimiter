package jackiecrazy.enchantlimiter;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
import jackiecrazy.enchantlimiter.exempt.Exemptions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

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
                        // /enchantlimiter exempt <players>            -> show status
                        // /enchantlimiter exempt <players> <true|false> -> set status
                        // GameProfileArgument also accepts offline players known to the server.
                        .then(Commands.literal("exempt")
                                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                        .executes(ctx -> showExempt(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "players")))
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setExempt(ctx.getSource(),
                                                        GameProfileArgument.getGameProfiles(ctx, "players"),
                                                        BoolArgumentType.getBool(ctx, "value"))))))
        );
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        LimiterConfig.setModEnabled(enabled);
        source.sendSuccess(() -> Component.literal("EnchantLimiter has been " + (enabled ? "enabled" : "disabled") + "."), true);
        return enabled ? 1 : 0;
    }

    private static int showExempt(CommandSourceStack source, Collection<GameProfile> players) {
        int count = 0;
        for (GameProfile p : players) {
            boolean exempt = Exemptions.isExempt(source.getServer(), p.getId());
            if (exempt) count++;
            source.sendSuccess(() -> Component.literal(p.getName() + " is " + (exempt ? "exempt from" : "limited by") + " EnchantLimiter."), false);
        }
        return count;
    }

    private static int setExempt(CommandSourceStack source, Collection<GameProfile> players, boolean value) {
        for (GameProfile p : players) {
            Exemptions.setExempt(source.getServer(), p.getId(), value);
            source.sendSuccess(() -> Component.literal(p.getName() + " is " + (value ? "now exempt from" : "no longer exempt from") + " EnchantLimiter."), true);
        }
        return players.size();
    }
}
