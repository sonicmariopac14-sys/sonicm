package com.example.stalker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Hace aparecer al Stalker detras del jugador de vez en cuando. */
@Mod.EventBusSubscriber(modid = StalkerMod.MODID)
public class StalkerEvents {
    private static final int CHECK_INTERVAL = 200; // cada 10 s
    private static final int CHANCE = 6;           // 1 de 6 => ~1 vez por minuto

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || player.isSpectator()) return;
        if (player.tickCount % CHECK_INTERVAL != 0) return;
        if (level.random.nextInt(CHANCE) != 0) return;
        if (!level.getEntitiesOfClass(StalkerEntity.class, player.getBoundingBox().inflate(96.0D)).isEmpty()) return;

        // Direccion contraria a donde mira el jugador, con un poco de desvio
        Vec3 look = player.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z);
        if (back.lengthSqr() < 1.0E-4) return;
        back = back.normalize().yRot((float) Math.toRadians(level.random.nextInt(81) - 40));

        double dist = 18.0D + level.random.nextDouble() * 10.0D;
        double x = player.getX() + back.x * dist;
        double z = player.getZ() + back.z * dist;

        BlockPos probe = BlockPos.containing(x, 0, z);
        if (!level.hasChunkAt(probe)) return;
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
        if (!level.getBlockState(pos.below()).isSolid() || !level.getFluidState(pos).isEmpty()) return;

        StalkerEntity stalker = ModEntities.STALKER.get().create(level);
        if (stalker == null) return;
        stalker.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(stalker);
    }
}
