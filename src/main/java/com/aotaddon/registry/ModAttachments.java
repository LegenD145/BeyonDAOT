package com.aotaddon.registry;

import com.aotaddon.AotAddon;
import com.aotaddon.combat.CombatTagData;
import com.aotaddon.horse.HorseBondData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AotAddon.MOD_ID);

    public static final Supplier<AttachmentType<CombatTagData>> COMBAT_TAG =
            ATTACHMENT_TYPES.register("combat_tag",
                    () -> AttachmentType.builder(CombatTagData::new).build());

    public static final Supplier<AttachmentType<HorseBondData>> HORSE_BOND =
            ATTACHMENT_TYPES.register("horse_bond",
                    () -> AttachmentType.builder(HorseBondData::new)
                            .serialize(HorseBondData.CODEC)
                            .build());
}