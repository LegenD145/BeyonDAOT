package com.aotaddon.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * daot's BladeAttackTracker resolves swing targets via an AABB+cone sweep
 * (see BladeAttackTracker#tick), not a single-entity raycast. That means a
 * swing near a titan's head can independently "hit" both TitanNapeEntity and
 * TitanEyeEntity in the same tick, since both fall inside the same swept
 * volume regardless of aim. Left unfixed, the same problem would multiply
 * across every limb hitbox entity we add (thigh/knee/shin/ankle/foot all
 * clustered close together).
 *
 * This class filters a raw sweep result down to at most one hit per parent
 * titan for any entity that is itself a "sub-hitbox" of a titan (daot's nape
 * and eye entities, plus our own limb hitbox entity once it exists). All
 * other entities in the sweep (players, mobs) pass through untouched.
 *
 * daot is never a compile-time dependency, so titan sub-hitbox classes are
 * identified by simple name and their parent is resolved reflectively,
 * matching the same style daot itself uses internally
 * (BladeAttackTracker#isTitanWeakpoint does an equivalent simple-name check).
 */
public final class HitboxDeduper {

    // Every titan type has its own dedicated Nape/Eye subclass rather than
    // instantiating TitanNapeEntity/TitanEyeEntity directly (e.g.
    // FemaleTitanNapeEntity, ArmoredTitanEyeEntity), so match by suffix
    // instead of exact name.
    private static final Set<String> HITBOX_ENTITY_SUFFIXES = Set.of(
            "NapeEntity",
            "EyeEntity",
            "LimbHitboxEntity" // aotaddon's own, once built
    );

    // Cached reflective lookup of getParentTitan() per entity class, so we
    // only pay the reflection cost once per class rather than per hit.
    private static final Map<Class<?>, Method> PARENT_LOOKUP_CACHE = new HashMap<>();

    private HitboxDeduper() {
    }

    /**
     * @param raw      the entities returned by the AABB/cone sweep
     * @param attacker the swinging player (used as the reference point for
     *                 "closest")
     * @return a filtered list containing at most one titan-sub-hitbox entity
     * per distinct parent titan, plus every non-hitbox entity unchanged
     */
    public static List<Entity> collapseToClosestPerTitan(List<Entity> raw, Entity attacker) {
        if (raw.size() <= 1) {
            return raw;
        }

        Map<Integer, Entity> closestPerParent = new HashMap<>();
        List<Entity> passthrough = new ArrayList<>(raw.size());

        for (Entity e : raw) {
            Entity parent = resolveParentTitan(e);
            if (parent == null) {
                passthrough.add(e);
                continue;
            }
            int parentId = parent.getId();
            Entity current = closestPerParent.get(parentId);
            if (current == null || isCloser(attacker, e, current)) {
                closestPerParent.put(parentId, e);
            }
        }

        if (closestPerParent.isEmpty()) {
            return passthrough;
        }

        List<Entity> result = new ArrayList<>(passthrough.size() + closestPerParent.size());
        result.addAll(passthrough);
        result.addAll(closestPerParent.values());
        return result;
    }

    private static boolean isCloser(Entity attacker, Entity candidate, Entity current) {
        Vec3 origin = attacker.position();
        return origin.distanceToSqr(candidate.position()) < origin.distanceToSqr(current.position());
    }

    /**
     * Public entry point so other systems (e.g. the Female Titan
     * decapitation check) can reuse the same reflective parent-titan
     * lookup instead of duplicating it.
     */
    public static Entity resolveParentTitan(Entity e) {
        Class<?> clazz = e.getClass();
        String simpleName = clazz.getSimpleName();
        boolean isHitboxEntity = HITBOX_ENTITY_SUFFIXES.stream().anyMatch(simpleName::endsWith);
        if (!isHitboxEntity) {
            return null;
        }
        Method method = PARENT_LOOKUP_CACHE.computeIfAbsent(clazz, HitboxDeduper::lookupGetParentTitan);
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(e);
            return result instanceof Entity ? (Entity) result : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static Method lookupGetParentTitan(Class<?> clazz) {
        try {
            Method m = clazz.getMethod("getParentTitan");
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}