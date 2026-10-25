package mod.chiselsandbits.gametest;

import mod.chiselsandbits.api.APIExceptions.CannotBeChiseled;
import mod.chiselsandbits.api.APIExceptions.SpaceOccupied;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.events.PickBlockHandler;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Edge cases for hit-aware pick block on chiseled blocks. */
public final class PickBlockGameTestsBodies {

    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    public static void picksStoredBitUnderCursorAndRejectsEmptyBit(final GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.STONE);
        final BlockPos absolute = helper.absolutePos(TARGET);
        final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();

        try {
            final IBitAccess access = api.getBitAccess(helper.getLevel(), absolute);
            access.setBitAt(0, 0, 0, null);
            access.commitChanges(true);
        } catch (final CannotBeChiseled | SpaceOccupied error) {
            throw new AssertionError(error);
        }

        final TileEntityBlockChiseled entity =
                (TileEntityBlockChiseled) helper.getLevel().getBlockEntity(absolute);
        if (entity == null) {
            throw new AssertionError("chiseled block entity missing");
        }

        final BlockChiseled block = ModBlocks.CHISELED_BLOCK.get();
        final ItemStack picked = block.getPickedBit(hitAt(absolute, 0.99), entity);
        helper.assertTrue(picked.getItem() == ModItems.ITEM_BLOCK_BIT.get(), "picked the stored bit");
        helper.assertValueEqual(picked.getCount(), 1, "pick block yields a single bit");

        final ItemStack emptyBit = block.getPickedBit(hitAt(absolute, 0.01), entity);
        helper.assertTrue(emptyBit.isEmpty(), "empty bit yields no item");

        helper.succeed();
    }

    public static void returnsEmptyStackForFullyEmptiedBlock(final GameTestHelper helper) {
        helper.setBlock(TARGET, ModBlocks.CHISELED_BLOCK.get());
        final BlockPos absolute = helper.absolutePos(TARGET);
        final TileEntityBlockChiseled entity =
                (TileEntityBlockChiseled) helper.getLevel().getBlockEntity(absolute);
        if (entity == null) {
            throw new AssertionError("chiseled block entity missing");
        }

        helper.assertTrue(
                ModBlocks.CHISELED_BLOCK
                        .get()
                        .getPickedBit(hitAt(absolute, 0.01), entity)
                        .isEmpty(),
                "empty blob has no bit to pick");

        helper.succeed();
    }

    public static void cloneStackConsumesRememberedHit(final GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.STONE);
        final BlockPos absolute = helper.absolutePos(TARGET);
        final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();

        try {
            final IBitAccess access = api.getBitAccess(helper.getLevel(), absolute);
            access.setBitAt(0, 0, 0, null);
            access.commitChanges(true);
        } catch (final CannotBeChiseled | SpaceOccupied error) {
            throw new AssertionError(error);
        }

        final ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ITEM_CHISEL_IRON.get()));
        // Remember-hit validates reach, so stand next to the block.
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);

        PickBlockHandler.rememberHit(player, hitAt(absolute, 0.99));
        final ItemStack picked = ModBlocks.CHISELED_BLOCK
                .get()
                .getCloneItemStack(helper.getLevel(), absolute, helper.getBlockState(TARGET), false);

        helper.assertTrue(picked.getItem() == ModItems.ITEM_BLOCK_BIT.get(), "remembered hit yields the bit");
        helper.succeed();
    }

    private static BlockHitResult hitAt(final BlockPos absolute, final double offset) {
        return new BlockHitResult(
                new Vec3(absolute.getX() + offset, absolute.getY() + offset, absolute.getZ() + offset),
                Direction.UP,
                absolute,
                false);
    }
}
