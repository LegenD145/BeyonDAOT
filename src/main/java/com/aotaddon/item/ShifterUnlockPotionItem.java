package com.aotaddon.item;

import com.aotaddon.AotAddon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * One reusable item class for all three permanent-unlock Female shifter potions
 * (Zero Hour Formula, Resilience Compound, Raptor Compound), mirroring DAOT's own
 * ArmorPotionItem behavior: drink once -> gain a permanent vanilla scoreboard tag ->
 * that tag gates the actual mechanic elsewhere (mixins / tick handlers).
 *
 * Reuses the Armor Potion's geo model + a per-item texture, same pattern already
 * agreed on for the placeholder assets.
 */
public class ShifterUnlockPotionItem extends Item implements GeoItem {

    /** Set by client bootstrap, same pattern as ArmorPotionItem.clientRendererConsumer. */
    public static Consumer<Consumer<software.bernie.geckolib.animatable.client.GeoRenderProvider>> clientRendererConsumer;

    private static final Map<UUID, Long> REJECTION_COOLDOWN_TICK = new ConcurrentHashMap<>();
    private static final long REJECTION_COOLDOWN_TICKS = 20L;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final String unlockTag;
    private final String requiredShifterTag;
    private final String acquiredMessage;
    private final String tooltip;
    private final ResourceLocation modelResource;
    private final ResourceLocation textureResource;

    public ShifterUnlockPotionItem(Item.Properties properties, String unlockTag, String requiredShifterTag,
                                    String acquiredMessage, String tooltip,
                                    ResourceLocation modelResource, ResourceLocation textureResource) {
        super(properties);
        this.unlockTag = unlockTag;
        this.requiredShifterTag = requiredShifterTag;
        this.acquiredMessage = acquiredMessage;
        this.tooltip = tooltip;
        this.modelResource = modelResource;
        this.textureResource = textureResource;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public ResourceLocation getModelResource() {
        return modelResource;
    }

    public ResourceLocation getTextureResource() {
        return textureResource;
    }

    public String getUnlockTag() {
        return unlockTag;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, net.minecraft.world.item.TooltipFlag flag) {
        tooltipComponents.add(Component.literal(tooltip).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player.getTags().contains(unlockTag)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!canUse(player)) {
            long now = level.getGameTime();
            Long last = REJECTION_COOLDOWN_TICK.get(player.getUUID());
            if (last == null || now - last >= REJECTION_COOLDOWN_TICKS) {
                REJECTION_COOLDOWN_TICK.put(player.getUUID(), now);
                player.displayClientMessage(
                        Component.literal("Requires the " + requiredShifterTag + " shifter power")
                                .withStyle(ChatFormatting.RED), false);
            }
            player.stopUsingItem();
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    private boolean canUse(Player player) {
        return player.getTags().contains(requiredShifterTag);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 0.5f);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (canUse(player) && !sp.getTags().contains(unlockTag)) {
                sp.addTag(unlockTag);
                sp.displayClientMessage(Component.literal(acquiredMessage).withStyle(ChatFormatting.GOLD), false);
                AotAddon.LOGGER.info("[ShifterUnlockPotion] {} acquired tag {}", sp.getName().getString(), unlockTag);
            }
            if (!sp.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void createGeoRenderer(Consumer consumer) {
        if (clientRendererConsumer != null) {
            clientRendererConsumer.accept(consumer);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
