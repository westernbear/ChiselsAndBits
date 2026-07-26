package mod.chiselsandbits.events;

import mod.chiselsandbits.api.ChiselsAndBitsEvents;
import mod.chiselsandbits.api.EventFullBlockRestoration;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.Blocks;

public class VaporizeWater {

    public static void register() {
        ChiselsAndBitsEvents.FULL_BLOCK_RESTORATION.register(VaporizeWater::handle);
    }

    private static void handle(final EventFullBlockRestoration e) {
        if (e.getState().getBlock() == Blocks.WATER
                && e.getWorld().environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, e.getPos())) {
            double i = e.getPos().getX();
            double j = e.getPos().getY();
            double k = e.getPos().getZ();
            e.getWorld()
                    .playLocalSound(
                            i,
                            j,
                            k,
                            SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS,
                            0.5F,
                            2.6F
                                    + (e.getWorld().getRandom().nextFloat()
                                                    - e.getWorld().getRandom().nextFloat())
                                            * 0.8F,
                            true);

            for (int l = 0; l < 8; ++l) {
                e.getWorld()
                        .addParticle(
                                ParticleTypes.LARGE_SMOKE,
                                i + Math.random(),
                                j + Math.random(),
                                k + Math.random(),
                                0.0D,
                                0.0D,
                                0.0D);
            }

            e.getWorld().setBlockAndUpdate(e.getPos(), Blocks.AIR.defaultBlockState());
            e.setCancelled(true);
        }
    }
}
