package com.example.stalker;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Un ser que te observa. Nunca ataca: se acerca cuando no lo miras,
 * se queda quieto cuando lo miras y desaparece si lo miras mucho rato
 * o si te acercas demasiado.
 */
public class StalkerEntity extends PathfinderMob {
    private static final int MAX_WATCH_TICKS = 50;          // ~2.5 s mirandolo
    private static final int MAX_LIFE_TICKS = 20 * 120;     // 2 minutos maximo
    private static final double VANISH_DISTANCE = 7.0;
    private static final double STOP_DISTANCE = 14.0;

    private int watchedTicks = 0;
    private int lifeTicks = 0;

    public StalkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        Player player = this.level().getNearestPlayer(this, 64.0D);
        if (player == null || player.isSpectator() || ++lifeTicks > MAX_LIFE_TICKS) {
            vanish(false);
            return;
        }

        this.getLookControl().setLookAt(player, 60.0F, 60.0F);
        double dist = this.distanceTo(player);

        if (dist < VANISH_DISTANCE) {
            vanish(true);
            return;
        }

        if (isSeenBy(player)) {
            this.getNavigation().stop();
            if (++watchedTicks >= MAX_WATCH_TICKS) {
                vanish(true);
            }
        } else {
            watchedTicks = 0;
            if (dist > STOP_DISTANCE) {
                if (this.tickCount % 10 == 0) {
                    this.getNavigation().moveTo(player, 0.7D);
                }
            } else {
                this.getNavigation().stop();
            }
        }
    }

    /** Misma logica que el Enderman: el jugador lo tiene en el centro de la mira. */
    private boolean isSeenBy(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toMe = new Vec3(this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ());
        double d = toMe.length();
        toMe = toMe.normalize();
        double dot = view.dot(toMe);
        return dot > 1.0D - 0.03D / d && player.hasLineOfSight(this);
    }

    private void vanish(boolean withEffects) {
        if (withEffects && this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDERMAN_STARE, SoundSource.AMBIENT, 1.0F, 0.5F);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(),
                    25, 0.3D, 0.7D, 0.3D, 0.01D);
        }
        this.discard();
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
