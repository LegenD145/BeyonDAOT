package com.aotaddon.horse;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Per-player horse whistle bond state, stored as a NeoForge data attachment.
 * Not serialized — bonds do not survive a server restart (same tradeoff as
 * CombatTagData). If persistence is ever needed, add a serializer to the
 * AttachmentType in ModAttachments.
 */
public class HorseBondData {

    /** UUID of the bonded horse, or null if never bonded. */
    public UUID horseUUID = null;

    /** Dimension the horse was bonded in. Summon only works within this dimension. */
    public ResourceKey<Level> dimension = null;

    /** Game-time tick at which the summon cooldown expires. 0 = no cooldown active. */
    public long cooldownExpiryTick = 0L;
}