package com.aotaddon.command;

import com.aotaddon.combat.DecapitationHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * /pur - debug/test command tree, bypasses real trigger conditions so
 * mechanics can be tested in isolation without needing to actually land
 * them in-game.
 *
 * /pur impulse female
 *   Manually triggers Female Titan decapitation (skips ImpulseWindowHandler
 *   and the real eye-hit event entirely) on the nearest FemaleTitanEntity
 *   within 50 blocks of the command sender. Calls DecapitationHandler
 *   directly, same as FemaleDecapitationHandler would once the real
 *   Impulse + eye-hit conditions are met.
 *
 * Permission level 2 (op) required, same as /setfamily.
 */
public class PurCommand {

    private static final double SEARCH_RADIUS = 50.0;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("pur")
                        .requires(src -> src.hasPermission(2))
                        .then(
                                Commands.literal("impulse")
                                        .then(
                                                Commands.literal("female")
                                                        .executes(PurCommand::triggerFemaleDecapitation)
                                        )
                        )
        );
    }

    private static int triggerFemaleDecapitation(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be run by a player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Could not resolve server level.").withStyle(ChatFormatting.RED));
            return 0;
        }

        LivingEntity target = findNearestFemaleTitan(level, player);
        if (target == null) {
            source.sendFailure(Component.literal(
                    "No Female Titan found within " + (int) SEARCH_RADIUS + " blocks."
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        DecapitationHandler.decapitate(target, player);

        player.sendSystemMessage(Component.literal(
                "§a[pur] Manually triggered decapitation on Female Titan (" + target.getId() + ")."
        ));

        return 1;
    }

    private static LivingEntity findNearestFemaleTitan(ServerLevel level, ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(SEARCH_RADIUS);

        LivingEntity closest = null;
        double closestDistSqr = Double.MAX_VALUE;

        for (Entity e : level.getEntities(player, searchBox, entity ->
                entity instanceof LivingEntity && entity.getClass().getSimpleName().equals("FemaleTitanEntity"))) {
            double distSqr = player.distanceToSqr(e);
            if (distSqr < closestDistSqr) {
                closestDistSqr = distSqr;
                closest = (LivingEntity) e;
            }
        }

        return closest;
    }
}