package com.aotaddon.horse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class HorseBondData {

    public static final Codec<HorseBondData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.optionalFieldOf("horseUUID").forGetter(d -> Optional.ofNullable(d.horseUUID)),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(d -> Optional.ofNullable(d.dimension)),
            Codec.LONG.fieldOf("cooldownExpiryTick").forGetter(d -> d.cooldownExpiryTick)
    ).apply(inst, (uuid, dim, cd) -> {
        HorseBondData data = new HorseBondData();
        uuid.ifPresent(u -> data.horseUUID = u);
        dim.ifPresent(d -> data.dimension = d);
        data.cooldownExpiryTick = cd;
        return data;
    }));

    public UUID horseUUID = null;
    public ResourceKey<Level> dimension = null;
    public long cooldownExpiryTick = 0L;
}