package com.aotaddon.family;
// hi guys pls help me im DYING DUDE
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Registers /setfamily <player> <family>
 * Permission level 2 (op) required.
 *
 * IMPORTANT: No import of ServerPlayer — extends Player → Entity.
 * Referenced inline inside method bodies only.
 */
public class SetFamilyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setfamily")
                        .requires(src -> src.hasPermission(2))
                        .then(
                                Commands.argument("player", StringArgumentType.word())
                                        .then(Commands.literal("helos") .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), "helos")))
                                        .then(Commands.literal("fritz") .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), "fritz")))
                                        .then(Commands.literal("yeager").executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), "yeager")))
                                        .then(Commands.literal("reiss") .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), "reiss")))
                        )
        );
    }

    private static int execute(CommandSourceStack source, String targetName, String family) {
        net.minecraft.server.level.ServerPlayer target = null;
        for (net.minecraft.server.level.ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equals(targetName)) {
                target = p;
                break;
            }
        }

        if (target == null) {
            source.sendFailure(Component.literal("Player not found or not online: " + targetName)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        FamilyData.setFamily(target, family);
        FamilyEventHandler.sendFamilyMessage(target, family);

        try {
            net.minecraft.server.level.ServerPlayer sender = source.getPlayerOrException();
            sender.sendSystemMessage(Component.literal(
                    "§aSet §f" + targetName + "§a's family to §e" + family + "§a."
            ));
        } catch (Exception ignored) {
            source.getServer().sendSystemMessage(Component.literal(
                    "[TitanRequiem] Set " + targetName + "'s family to " + family
            ));
        }

        return 1;
    }
}