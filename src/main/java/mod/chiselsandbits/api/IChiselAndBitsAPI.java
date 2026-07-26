package mod.chiselsandbits.api;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.APIExceptions.CannotBeChiseled;
import mod.chiselsandbits.api.APIExceptions.InvalidBitItem;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Public Chisels & Bits API. */
public interface IChiselAndBitsAPI {

    void registerBlockProvider(BlockProvider provider);

    void registerItemStackHandler(Block block, @NotNull ItemStackHandler provider);

    /**
     * Determine the Item Type of the item in an ItemStack and return it.
     *
     * @param stack
     * @return ItemType of the item, or null if it is not any of them.
     */
    @Nullable
    ItemType getItemType(ItemStack stack);

    /**
     * Check if a block can support {@link IBitAccess}
     *
     * @param world
     * @param pos
     * @return true if the block can be chiseled, this is true for air,
     * multi-parts, and blocks which can be chiseled, false otherwise.
     */
    boolean canBeChiseled(Level world, BlockPos pos);

    /**
     * is this block already chiseled?
     *
     * @param world
     * @param pos
     * @return true if the block contains chiseled bits, false otherwise.
     */
    boolean isBlockChiseled(Level world, BlockPos pos);

    /**
     * Get Access to the bits for a given block.
     *
     * @param world
     * @param pos
     * @return A {@link IBitAccess} for the specified location.
     * @throws CannotBeChiseled when the location cannot support bits, or if the parameters
     *                          are invalid.
     */
    IBitAccess getBitAccess(Level world, BlockPos pos) throws CannotBeChiseled;

    /**
     * Create a bit access from an ItemStack, passing an empty ItemStack creates
     * an empty bit access, passing an invalid item returns null.
     *
     * @return a {@link IBitAccess} for an ItemStack.
     */
    @Nullable
    IBitAccess createBitItem(ItemStack stack);

    /**
     * Create a brush from an ItemStack, once created you can use it many times.
     *
     * @param stack the stack.
     * @return A brush for the specified ItemStack, if passed an empty ItemStack
     * an air brush is created.
     * @throws InvalidBitItem
     */
    IBitBrush createBrush(ItemStack stack) throws InvalidBitItem;

    /**
     * Create a brush from a state, once created you can use it many times.
     *
     * @param state
     * @return A brush for the specified state, if null is passed for the item
     * an air brush is created.
     * @throws InvalidBitItem
     */
    IBitBrush createBrushFromState(@Nullable BlockState state) throws InvalidBitItem;

    /**
     * Convert ray trace information into bit location information, note that
     * the block position can change, be aware.
     *
     * @param hitX
     * @param hitY
     * @param hitZ
     * @param side
     * @param pos
     * @param placement
     * @return details about the target bit, no arguments should be null.
     */
    IBitLocation getBitPos(double hitX, double hitY, double hitZ, Direction side, BlockPos pos, boolean placement);

    /**
     * Get an ItemStack for the bit type of the state...
     * <p>
     * VERY IMPORTANT: C&B lets you disable bits, if this happens the Item in
     * this ItemStack WILL BE empty.
     *
     * @param defaultState
     * @return the bit.
     */
    ItemStack getBitItem(BlockState defaultState) throws InvalidBitItem;

    /**
     * Give a bit to a player, it will end up in their inventory, a bag, or if
     * there is no where to put it, on the ground.
     * <p>
     * CLIENT: destroys the item.
     * <p>
     * SERVER: adds ItemStack to inv/bag/spawns entity.
     *
     * @param player   player to give bits to.
     * @param stack    bits to store.
     * @param spawnPos if null defaults to the players position, absolute position of
     *                 where to spawn bits, should be in the block near where they
     *                 are being extracted from.
     */
    void giveBitToPlayer(Player player, ItemStack stack, Vec3 spawnPos);

    /**
     * Access the contents of a bitbag as if it was a normal
     * {@link IBitBag}.
     *
     * @return internal object to manipulate bag.
     */
    @Nullable
    IBitBag getBitbag(ItemStack stack);

    /**
     * Example: int stackSize =
     * api.getParameter(IntegerParam.BIT_BAG_MAX_STACK_SIZE );
     *
     * @param which - refer to ParameterType for list of possible values.
     * @return value of specified parameter.
     */
    <T extends Object> T getParameter(ParameterType<T> which);

    /**
     * Begins an undo group, starting two operations without ending the previous
     * operation will throw a runtime exception.
     *
     * This is used to merge multiple blocks into a single operation, undo steps
     * will be recorded regardless of usage of this method, however its
     * suggested to use groups in any case where a change well affect more then
     * one block.
     *
     * @formatter:off
     *
     * 				Example:
     *
     *                try { api.beginUndoGroup(); this.manipulateAllTheBlocks();
     *                } finally { api.endUndoGroup(); }
     *
     */
    void beginUndoGroup(Player player);

    /**
     * Ends a previously running undo group, must be called after starting an
     * undo group, closing a group without opening one will result in a runtime
     * exception.
     */
    void endUndoGroup(Player player);

    /**
     * Get a C&B key binding.
     *
     * @param modKeyBinding
     *            the {@link ModKeyBinding} value that denotes the C&B key
     *            binding to return.
     * @return a C&B {@link KeyMapping}.
     */
    @Environment(EnvType.CLIENT)
    KeyMapping getKeyBinding(ModKeyBinding modKeyBinding);

    /**
     * Renders a model with transparency.
     *
     * @param model
     * @param world
     * @param pos
     * @param alpha
     *            Should be 0-255.
     */
    @Environment(EnvType.CLIENT)
    void renderModel(
            final PoseStack stack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos pos,
            final int alpha,
            final int combinedLight,
            final int combinedOverlay);

    /**
     * Renders a ghost model in the same way that chiseled item blocks are rendered for placement.
     *
     * @param model
     * @param world
     * @param pos
     * @param isUnplaceable
     *            if true, the block will be rendered with a decreased alpha value.
     */
    @Environment(EnvType.CLIENT)
    void renderGhostModel(
            final PoseStack stack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos pos,
            final boolean isUnplaceable,
            final int combinedLight,
            final int combinedOverlay);
}
