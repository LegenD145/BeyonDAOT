package com.aotaddon.command;

import com.aotaddon.combat.DecapitationHandler;
import com.aotaddon.combat.ShifterTitanHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Locale;

/**
 * /pur - debug/test command tree for decap mechanics.
 *
 * /pur impulse female       — nearest Female Titan (legacy alias)
 * /pur impulse shifter      — nearest any DAOT shifter titan
 * /pur impulse shifter &lt;type&gt; — nearest shifter matching type (e.g. armored, colossal)
 */
public class PurCommand {

    private static final double SEARCH_RADIUS = 50.0;

    private static final List<String> SHIFTER_TYPES = List.of(
            "female", "armored", "attack", "warhammer", "jaw", "cart", "beast", "colossal"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("pur")
                        .requires(src -> src.hasPermission(2))
                        .then(
                                Commands.literal("impulse")
                                        .then(
                                                Commands.literal("female")
                                                        .executes(ctx -> triggerDecap(ctx, "female"))
                                        )
                                        .then(
                                                Commands.literal("shifter")
                                                        .executes(ctx -> triggerDecap(ctx, null))
                                                        .then(
                                                                Commands.argument("type", StringArgumentType.word())
                                                                        .suggests((ctx, builder) -> {
                                                                            for (String type : SHIFTER_TYPES) {
                                                                                if (type.startsWith(builder.getRemainingLowerCase())) {
                                                                                    builder.suggest(type);
                                                                                }
                                                                            }
                                                                            return builder.buildFuture();
                                                                        })
                                                                        .executes(ctx -> triggerDecap(
                                                                                ctx,
                                                                                StringArgumentType.getString(ctx, "type")))
                                                        )
                                        )
                        )
        );
    }

    private static int triggerDecap(CommandContext<CommandSourceStack> ctx, String typeFilter) {
        CommandSourceStack source = ctx.getSource();

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be run by a player.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Could not resolve server level.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        LivingEntity target = findNearestShifterTitan(level, player, typeFilter);
        if (target == null) {
            String typeLabel = typeFilter == null ? "shifter titan" : typeFilter + " shifter titan";
            source.sendFailure(Component.literal(
                    "No " + typeLabel + " found within " + (int) SEARCH_RADIUS + " blocks."
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        DecapitationHandler.decapitate(target, player);

        String titanName = target.getClass().getSimpleName().replace("Entity", "");
        player.sendSystemMessage(Component.literal(
                "§a[pur] Manually triggered decapitation on " + titanName + " (" + target.getId() + ")."
        ));

        return 1;
    }

    private static LivingEntity findNearestShifterTitan(ServerLevel level, ServerPlayer player, String typeFilter) {
        AABB searchBox = player.getBoundingBox().inflate(SEARCH_RADIUS);
        String filter = typeFilter == null ? null : typeFilter.toLowerCase(Locale.ROOT);

        LivingEntity closest = null;
        double closestDistSqr = Double.MAX_VALUE;

        for (Entity entity : level.getEntities(player, searchBox, e -> e instanceof LivingEntity living
                && ShifterTitanHelper.isShifterTitan(living)
                && matchesTypeFilter(living, filter))) {
            double distSqr = player.distanceToSqr(entity);
            if (distSqr < closestDistSqr) {
                closestDistSqr = distSqr;
                closest = (LivingEntity) entity;
            }
        }

        return closest;
    }

    private static boolean matchesTypeFilter(LivingEntity titan, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        String className = titan.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String geoBase = ShifterTitanHelper.geoBaseName(titan.getClass().getSimpleName());
        return className.contains(filter) || geoBase.contains(filter);
    }
}
