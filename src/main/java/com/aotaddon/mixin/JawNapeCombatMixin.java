package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.access.JawLatchReflection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jaw Titan nape-execution combat: pounce onto a titan -> cling to its body
 * (instead of only walls) -> chomp/bite targets the nape specifically.
 *
 *  - Pure/mindless titan (daot.GrabbingTitan, not a shifter): 1 bite kills.
 *  - Any shifter titan except Armored/Colossal: 5 bites kills; both the titan
 *    body AND the player piloting it die.
 *  - Colossal Titan: 30 bites to kill (same double-kill on the finishing hit).
 *  - Armored Titan: biting its nape breaks your own jaw instead of doing
 *    anything to it - 40s (800 tick) chomp lockout, steam while it "heals".
 *
 * Ground-chomp (not clinging to anything) is untouched - chompBite() falls
 * straight through to daot's original flat-damage/grab behavior in that case.
 *
 * Targets daot.TestShifterTitanEntity via string (remap = false), matching
 * every other daot mixin in this addon - it's not on the compile classpath.
 * No @Shadow of daot-private fields; all daot-specific state is read/written
 * through Class.forName + reflection, same as CrystalShellGrabMixin /
 * WarhammerAbilityMixin / ShifterTitanUtil. Per-jaw clinging/hit-count state
 * lives in static maps keyed by the jaw entity's own UUID (same shape as
 * CrystalShellGrabMixin's `cooldowns` map), since we can't add real instance
 * fields to a class we don't extend.
 *
 * chompBite()/tryLatch()/unlatch() are plain no-arg instance methods declared
 * directly on TestShifterTitanEntity (not vanilla overrides), so - like
 * TitanEntityRandomRetargetMixin's method_5959 hook - a single injection
 * with the raw name is enough; no mojmap/intermediary dual-descriptor dance
 * needed since there are no vanilla-typed parameters to disambiguate.
 *
 * === WIRING NOTE ===
 * Add this class to your aotaddon mixins json next to your other daot mixins
 * (e.g. CrystalShellGrabMixin, WarhammerAbilityMixin).
 *
 * === VERIFY BEFORE SHIPPING ===
 * Field/method names here are copied from a decompiled reference jar, not
 * your live DAOT build - re-check against your actual jar, in particular:
 *   - tryLatch()/unlatch()/chompBite() still being private no-arg methods
 *   - isLatched()/isGrabbing() still public on TestShifterTitanEntity
 *   - GrabbingTitan/ShifterTitan/ArmoredTitanEntity/ColossalTitanEntity
 *     package paths (assumed "daot.X" throughout)
 */
@Mixin(targets = "daot.TestShifterTitanEntity", remap = false)
public class JawNapeCombatMixin {

    private static final double CLING_SCAN = 2.5;
    private static final int JAW_BROKEN_TICKS = 800; // 40s at 20tps
    private static final int SHIFTER_HITS_TO_KILL = 5;
    private static final int COLOSSAL_HITS_TO_KILL = 30;

    // jaw entity UUID -> id of the titan body it's clinging to (absent = not clinging)
    private static final Map<UUID, Integer> CLING_TARGET = new HashMap<>();
    // jaw entity UUID -> {dx, dy, dz} offset from the target's position to hold while clinging,
    // captured at the moment of latching so the jaw tracks a moving titan instead of a fixed point
    private static final Map<UUID, double[]> CLING_OFFSET = new HashMap<>();
    // jaw entity UUID -> ticks remaining on the "jaw broken" lockout
    private static final Map<UUID, Integer> JAW_BROKEN = new HashMap<>();
    // jaw entity UUID -> (target titan UUID -> nape-bite count so far)
    private static final Map<UUID, Map<UUID, Integer>> NAPE_HITS = new HashMap<>();

    // =========================================================
    // 1) Let tryLatch() grab onto a nearby titan, not just walls
    // =========================================================
    @Inject(method = "tryLatch", at = @At("HEAD"), cancellable = true, remap = false)
    private void aotaddon$tryLatchOntoTitan(CallbackInfo ci) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self.onGround()) return; // grounded - leave vanilla's own guard alone
            if (self.level().isClientSide()) return;

            AABB scan = self.getBoundingBox().inflate(CLING_SCAN);
            Entity best = null;
            double bestDistSq = Double.MAX_VALUE;

            for (Entity e : self.level().getEntities(self, scan)) {
                if (!aotaddon$isClingableTitanBody(e, self)) continue;
                double dx = e.getX() - self.getX();
                double dy = e.getY() - self.getY();
                double dz = e.getZ() - self.getZ();
                double d = dx * dx + dy * dy + dz * dz;
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = e;
                }
            }

            if (best == null) return; // nothing to cling to - fall through to vanilla wall-latch

            UUID jawUUID = self.getUUID();
            double offX = self.getX() - best.getX();
            double offY = self.getY() - best.getY();
            double offZ = self.getZ() - best.getZ();

            CLING_TARGET.put(jawUUID, best.getId());
            CLING_OFFSET.put(jawUUID, new double[]{offX, offY, offZ});

            self.setDeltaMovement(0.0, 0.0, 0.0);

            // flip DAOT's own real latch state - without this, isLatched() keeps
            // returning false, so daot's tick loop never stops re-calling tryLatch()
            // every tick and the chomp ability's own gating likely refuses to fire
            // while "airborne and unlatched"
            double len = Math.max(0.01, Math.sqrt(offX * offX + offZ * offZ));
            JawLatchReflection.setLatchNormal(self, (float) (offX / len), (float) (offZ / len));
            JawLatchReflection.setAnchor(self, self.getX(), self.getY(), self.getZ());
            JawLatchReflection.setLatched(self, true);

            ci.cancel();
        } catch (Exception e) {
            AotAddon.LOGGER.error("[JawNapeCombat] tryLatch hook failed: {}", e.toString());
        }
    }

    // =========================================================
    // 1b) keep the anchor tracking the target each tick - a titan walks
    //     around, unlike a wall, so a one-time anchor snap isn't enough
    // =========================================================
    @Inject(method = "method_5773", at = @At("HEAD"), remap = false, require = 0)
    private void aotaddon$trackClingTargetIntermediary(CallbackInfo ci) {
        aotaddon$trackClingTarget();
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false, require = 0)
    private void aotaddon$trackClingTargetMojmap(CallbackInfo ci) {
        aotaddon$trackClingTarget();
    }

    private void aotaddon$trackClingTarget() {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            UUID jawUUID = self.getUUID();
            Integer targetId = CLING_TARGET.get(jawUUID);
            if (targetId == null) return;
            if (!(self.level() instanceof ServerLevel serverLevel)) return;

            Entity target = serverLevel.getEntity(targetId);
            double[] offset = CLING_OFFSET.get(jawUUID);
            if (target == null || !target.isAlive() || offset == null) {
                CLING_TARGET.remove(jawUUID);
                CLING_OFFSET.remove(jawUUID);
                JawLatchReflection.setLatched(self, false);
                return;
            }

            JawLatchReflection.setAnchor(self,
                    target.getX() + offset[0], target.getY() + offset[1], target.getZ() + offset[2]);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[JawNapeCombat] cling tracking failed: {}", e.toString());
        }
    }

    private boolean aotaddon$isClingableTitanBody(Entity e, LivingEntity self) {
        if (e == self) return false;
        if (e == self.getControllingPassenger()) return false;
        if (!(e instanceof LivingEntity living) || !living.isAlive()) return false;
        try {
            boolean isGrabbing = Class.forName("daot.GrabbingTitan").isInstance(e);
            boolean isShifter = Class.forName("daot.ShifterTitan").isInstance(e);
            if (!isGrabbing && !isShifter) return false;
        } catch (ClassNotFoundException ex) {
            return false;
        }
        // skip sub-hitboxes (nape/eye/leg/hand/grab) - only latch the main body,
        // same className-suffix convention daot itself uses internally
        String simpleName = e.getClass().getSimpleName();
        return !(simpleName.endsWith("NapeEntity") || simpleName.endsWith("EyeEntity")
                || simpleName.endsWith("LegEntity") || simpleName.endsWith("HandEntity")
                || simpleName.endsWith("GrabEntity"));
    }

    @Inject(method = "unlatch", at = @At("HEAD"), remap = false)
    private void aotaddon$clearClingOnUnlatch(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        UUID jawUUID = self.getUUID();
        CLING_TARGET.remove(jawUUID);
        CLING_OFFSET.remove(jawUUID);
        // don't force-set DATA_LATCHED false here - unlatch() itself already does
        // that internally (see decompiled source, line ~1119); this hook only
        // needs to clean up our own state
    }

    // =========================================================
    // 2) chompBite(): nape-execution logic while clinging
    // =========================================================
    @Inject(method = "chompBite", at = @At("HEAD"), cancellable = true, remap = false)
    private void aotaddon$onChompBite(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        UUID jawUUID = self.getUUID();
        Integer targetId = CLING_TARGET.get(jawUUID);
        if (targetId == null) {
            return; // not clinging to a titan - let daot's normal chomp run untouched
        }
        ci.cancel();

        try {
            Class<?> testShifterClass = Class.forName("daot.TestShifterTitanEntity");
            boolean isGrabbing = (boolean) testShifterClass.getMethod("isGrabbing").invoke(self);
            if (isGrabbing) return;

            if (!(self.level() instanceof ServerLevel serverLevel)) return;

            Entity target = serverLevel.getEntity(targetId);
            if (target == null || !target.isAlive() || self.distanceToSqr(target) > CLING_SCAN * CLING_SCAN * 4) {
                CLING_TARGET.remove(jawUUID);
                CLING_OFFSET.remove(jawUUID);
                JawLatchReflection.setLatched(self, false);
                return;
            }

            Entity riderEntity = self.getControllingPassenger();
            Player rider = (riderEntity instanceof Player p) ? p : null;

            int brokenTicks = JAW_BROKEN.getOrDefault(jawUUID, 0);
            if (brokenTicks > 0) {
                aotaddon$tellRider(rider, "Your jaw hasn't healed yet!");
                return;
            }

            boolean isArmored = Class.forName("daot.ArmoredTitanEntity").isInstance(target);
            boolean isColossal = Class.forName("daot.ColossalTitanEntity").isInstance(target);
            boolean isShifter = Class.forName("daot.ShifterTitan").isInstance(target);
            boolean isPure = Class.forName("daot.GrabbingTitan").isInstance(target);

            if (isArmored) {
                aotaddon$biteArmoredPunish(serverLevel, self, jawUUID, rider);
            } else if (isColossal) {
                aotaddon$biteShifterNape(serverLevel, self, jawUUID, (LivingEntity) target, rider, COLOSSAL_HITS_TO_KILL);
            } else if (isShifter) {
                aotaddon$biteShifterNape(serverLevel, self, jawUUID, (LivingEntity) target, rider, SHIFTER_HITS_TO_KILL);
            } else if (isPure) {
                DamageSource source = self.damageSources().mobAttack(self);
                ((LivingEntity) target).hurt(source, Float.MAX_VALUE);
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 3.0f, 0.8f);
                aotaddon$tellRider(rider, "One bite, and it's over.");
            }
        } catch (Exception e) {
            AotAddon.LOGGER.error("[JawNapeCombat] chompBite hook failed: {}", e.toString());
        }
    }

    private void aotaddon$biteShifterNape(ServerLevel serverLevel, LivingEntity self, UUID jawUUID,
                                          LivingEntity titanBody, Player rider, int hitsToKill) {
        UUID targetUUID = titanBody.getUUID();
        Map<UUID, Integer> perJawHits = NAPE_HITS.computeIfAbsent(jawUUID, k -> new HashMap<>());
        int hits = perJawHits.merge(targetUUID, 1, Integer::sum);

        serverLevel.playSound(null, titanBody.getX(), titanBody.getY(), titanBody.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 3.0f,
                0.7f + self.getRandom().nextFloat() * 0.2f);

        if (hits < hitsToKill) {
            aotaddon$tellRider(rider, "Nape hit! (" + hits + "/" + hitsToKill + ")");
            return;
        }

        // finishing bite: kill the titan body AND the person piloting it
        perJawHits.remove(targetUUID);
        DamageSource source = self.damageSources().mobAttack(self);
        titanBody.hurt(source, Float.MAX_VALUE);

        Entity pilotEntity = titanBody.getControllingPassenger();
        if (pilotEntity instanceof LivingEntity pilot && pilot.isAlive()) {
            // genericKill() (not outOfWorld() - that field has no public getter
            // in NeoForge 1.21.x's DamageSources) bypasses most iframes/armor so
            // the pilot reliably dies alongside the titan body on the finishing bite
            pilot.hurt(pilot.damageSources().genericKill(), Float.MAX_VALUE);
        }
        aotaddon$tellRider(rider, "Nape torn out! (" + hits + "/" + hitsToKill + ")");
    }

    private void aotaddon$biteArmoredPunish(ServerLevel serverLevel, LivingEntity self, UUID jawUUID, Player rider) {
        JAW_BROKEN.put(jawUUID, JAW_BROKEN_TICKS);
        serverLevel.playSound(null, self.getX(), self.getY(), self.getZ(),
                SoundEvents.PLAYER_HURT, SoundSource.HOSTILE, 4.0f, 0.6f); // TODO: swap for a bone-crunch sound if you have one

        double[] origin = aotaddon$mouthOrigin(self);
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, origin[0], origin[1], origin[2], 20, 0.3, 0.2, 0.3, 0.02);

        aotaddon$tellRider(rider, "Your jaw shatters against its hardened plating!");
    }

    private void aotaddon$tellRider(Player rider, String msg) {
        if (rider != null) {
            rider.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), true);
        }
    }

    private double[] aotaddon$mouthOrigin(LivingEntity self) {
        float yawRad = (float) Math.toRadians(self.getYRot());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        return new double[]{
                self.getX() + fx * 2.0,
                self.getY() + self.getBbHeight() * 0.6,
                self.getZ() + fz * 2.0
        };
    }

    // =========================================================
    // 3) tick down the jaw-broken lockout + steam-regen effect
    // =========================================================
    @Inject(method = "method_5773", at = @At("TAIL"), remap = false, require = 0)
    private void aotaddon$tickJawBroken(CallbackInfo ci) {
        aotaddon$doTick();
    }

    // Fallback in case this build's vanilla tick override compiled under a
    // different intermediary name than method_5773 - harmless no-op via
    // require = 0 if daot's LivingEntity#tick keeps its usual name instead.
    @Inject(method = "tick", at = @At("TAIL"), remap = false, require = 0)
    private void aotaddon$tickJawBrokenMojmap(CallbackInfo ci) {
        aotaddon$doTick();
    }

    private void aotaddon$doTick() {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            UUID jawUUID = self.getUUID();
            Integer ticks = JAW_BROKEN.get(jawUUID);
            if (ticks == null || ticks <= 0) return;

            ticks--;
            if (ticks <= 0) {
                JAW_BROKEN.remove(jawUUID);
            } else {
                JAW_BROKEN.put(jawUUID, ticks);
            }

            if (self.level() instanceof ServerLevel serverLevel && self.tickCount % 5 == 0) {
                double[] o = aotaddon$mouthOrigin(self);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, o[0], o[1], o[2], 3, 0.15, 0.1, 0.15, 0.01);
            }
        } catch (Exception e) {
            AotAddon.LOGGER.error("[JawNapeCombat] tick hook failed: {}", e.toString());
        }
    }
}