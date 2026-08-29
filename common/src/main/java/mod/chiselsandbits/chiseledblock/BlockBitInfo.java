package mod.chiselsandbits.chiseledblock;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import java.util.HashMap;
import mod.chiselsandbits.api.IgnoreBlockLogic;
import mod.chiselsandbits.chiseledblock.data.VoxelType;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModTags;
import mod.chiselsandbits.render.helpers.ModelUtil;
import mod.chiselsandbits.utils.ClassUtils;
import mod.chiselsandbits.utils.FluidUtil;
import mod.chiselsandbits.utils.SingleBlockBlockReader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockBitInfo {
    // imc api...
    private static final HashMap<Block, Boolean> ignoreLogicBlocks = new HashMap<>();
    // cache data..
    private static final HashMap<BlockState, BlockBitInfo> stateBitInfo = new HashMap<>();
    private static final HashMap<Block, SupportsAnalysisResult> supportedBlocks = new HashMap<>();
    private static final HashMap<Block, Boolean> forcedBlocks = new HashMap<>();
    private static final HashMap<Block, Fluid> fluidBlocks = new HashMap<>();
    private static final IntObjectMap<Fluid> fluidStates = new IntObjectHashMap<>();
    private static final HashMap<BlockState, Integer> bitColor = new HashMap<>();

    static {
        ignoreLogicBlocks.put(Blocks.ACACIA_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.AZALEA_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.CHERRY_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.MANGROVE_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.FLOWERING_AZALEA_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.BIRCH_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.DARK_OAK_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.JUNGLE_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.OAK_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.SPRUCE_LEAVES, true);
        ignoreLogicBlocks.put(Blocks.SNOW, true);
    }

    public final boolean isCompatible;
    public final float hardness;
    public final float explosionResistance;

    private BlockBitInfo(final boolean isCompatible, final float hardness, final float explosionResistance) {
        this.isCompatible = isCompatible;
        this.hardness = hardness;
        this.explosionResistance = explosionResistance;
    }

    public static int getColorFor(final BlockState state, final int tint) {
        Integer out = bitColor.get(state);

        if (out == null) {
            final Block blk = state.getBlock();

            final Fluid fluid = BlockBitInfo.getFluidFromBlock(blk);
            if (fluid != null) {
                out = FluidUtil.getColor(fluid);
            } else {
                final ItemStack target = ModUtil.getItemStackFromBlockState(state);

                if (ModUtil.isEmpty(target)) {
                    out = 0xffffff;
                } else {
                    out = ModelUtil.getBlockStateColor(state, tint);
                }
            }

            bitColor.put(state, out);
        }

        return out;
    }

    public static void recalculate() {
        recalculateFluids();
    }

    public static void recalculateFluids() {
        fluidBlocks.clear();

        for (final Fluid o : BuiltInRegistries.FLUID) {
            if (o.defaultFluidState().isSource()) {
                BlockBitInfo.addFluidBlock(o);
            }
        }
    }

    public static void addFluidBlock(final Fluid fluid) {
        fluidBlocks.put(fluid.defaultFluidState().createLegacyBlock().getBlock(), fluid);

        for (final BlockState state : fluid.defaultFluidState()
                .createLegacyBlock()
                .getBlock()
                .getStateDefinition()
                .getPossibleStates()) {
            try {
                fluidStates.put(ModUtil.getStateId(state), fluid);
            } catch (final Throwable t) {
                Log.logError("Error while determining fluid state.", t);
            }
        }

        stateBitInfo.clear();
        supportedBlocks.clear();
    }

    public static Fluid getFluidFromBlock(final Block blk) {
        return fluidBlocks.get(blk);
    }

    public static VoxelType getTypeFromStateID(final int bit) {
        if (bit == 0) {
            return VoxelType.AIR;
        }

        return fluidStates.containsKey(bit) ? VoxelType.FLUID : VoxelType.SOLID;
    }

    public static void ignoreBlockLogic(final Block which) {
        ignoreLogicBlocks.put(which, true);
        reset();
    }

    public static void forceStateCompatibility(final Block which, final boolean forceStatus) {
        forcedBlocks.put(which, forceStatus);
        reset();
    }

    public static void reset() {
        stateBitInfo.clear();
        supportedBlocks.clear();
    }

    public static BlockBitInfo getBlockInfo(final BlockState state) {
        BlockBitInfo bit = stateBitInfo.get(state);

        if (bit == null) {
            bit = BlockBitInfo.createFromState(state);
            stateBitInfo.put(state, bit);
        }

        return bit;
    }

    @SuppressWarnings("deprecation")
    public static SupportsAnalysisResult doSupportAnalysis(final BlockState state) {
        if (state.getBlock() instanceof BlockChiseled) {
            return new SupportsAnalysisResult(
                    true, LocalStrings.ChiselSupportGenericNotSupported, LocalStrings.ChiselSupportIsAlreadyChiseled);
        }

        if (forcedBlocks.containsKey(state.getBlock())) {
            final boolean forcing = forcedBlocks.get(state.getBlock());
            return new SupportsAnalysisResult(
                    forcing, LocalStrings.ChiselSupportForcedUnsupported, LocalStrings.ChiselSupportForcedSupported);
        }

        final Block blk = state.getBlock();
        if (supportedBlocks.containsKey(blk)) {
            return supportedBlocks.get(blk);
        }

        if (state.is(ModTags.Blocks.BLOCKED_CHISELABLE)) {
            final SupportsAnalysisResult result = new SupportsAnalysisResult(
                    false, LocalStrings.ChiselSupportTagBlackListed, LocalStrings.ChiselSupportTagWhitelisted);
            supportedBlocks.put(blk, result);
            return result;
        }

        if (state.is(ModTags.Blocks.FORCED_CHISELABLE)) {
            final SupportsAnalysisResult result = new SupportsAnalysisResult(
                    true, LocalStrings.ChiselSupportTagBlackListed, LocalStrings.ChiselSupportTagWhitelisted);
            supportedBlocks.put(blk, result);

            final BlockBitInfo info = BlockBitInfo.createFromState(state);
            stateBitInfo.put(state, info);

            return result;
        }

        try {
            // require basic hardness behavior...
            final ReflectionHelperBlock pb = ModBlocks.REFLECTION_HELPER_BLOCK.get();
            final Class<? extends Block> blkClass = blk.getClass();

            // custom dropping behavior?
            pb.getDrops(state, null);
            final Class<?> wc = ClassUtils.getDeclaringClass(
                    blkClass, pb.getLastInvokedThreadLocalMethodName(), BlockState.class, LootParams.Builder.class);
            final boolean quantityDroppedTest =
                    wc == Block.class || wc == BlockBehaviour.class || wc == LiquidBlock.class;

            final boolean isNotSlab = Item.byBlock(blk) != Items.AIR || state.getBlock() instanceof LiquidBlock;
            boolean itemExistsOrNotSpecialDrops = quantityDroppedTest || isNotSlab;

            // ignore blocks with custom collision.
            pb.getShape(null, null, null, null);
            Class<?> collisionClass = ClassUtils.getDeclaringClass(
                    blkClass,
                    pb.getLastInvokedThreadLocalMethodName(),
                    BlockState.class,
                    BlockGetter.class,
                    BlockPos.class,
                    CollisionContext.class);
            boolean noCustomCollision = collisionClass == Block.class
                    || collisionClass == BlockBehaviour.class
                    || blk.getClass() == SlimeBlock.class
                    || collisionClass == LiquidBlock.class;

            // full cube specifically is tied to lighting... so for glass
            // Compatibility use isFullBlock which can be true for glass.
            boolean isFullBlock = state.canOcclude() || blk instanceof TransparentBlock || blk instanceof LiquidBlock;
            final BlockBitInfo info = BlockBitInfo.createFromState(state);

            final boolean tickingBehavior = state.isRandomlyTicking()
                    && ChiselsAndBits.getConfig()
                            .getServer()
                            .blackListRandomTickingBlocks
                            .get();
            boolean hasBehavior = (blk instanceof EntityBlock || tickingBehavior);

            final Boolean IgnoredLogic = ignoreLogicBlocks.get(blk);
            if (blkClass.isAnnotationPresent(IgnoreBlockLogic.class) || IgnoredLogic != null && IgnoredLogic) {
                isFullBlock = true;
                noCustomCollision = true;
                hasBehavior = false;
                itemExistsOrNotSpecialDrops = true;
            }

            if (info.isCompatible
                    && noCustomCollision
                    && info.hardness >= -0.01f
                    && isFullBlock
                    && !hasBehavior
                    && itemExistsOrNotSpecialDrops) {
                final SupportsAnalysisResult result = new SupportsAnalysisResult(
                        true,
                        LocalStrings.ChiselSupportGenericNotSupported,
                        (blkClass.isAnnotationPresent(IgnoreBlockLogic.class) || IgnoredLogic != null && IgnoredLogic)
                                ? LocalStrings.ChiselSupportLogicIgnored
                                : LocalStrings.ChiselSupportGenericSupported);

                supportedBlocks.put(blk, result);
                stateBitInfo.put(state, info);
                return result;
            }

            if (fluidBlocks.containsKey(blk)) {
                stateBitInfo.put(state, info);

                final SupportsAnalysisResult result = new SupportsAnalysisResult(
                        true,
                        LocalStrings.ChiselSupportGenericNotSupported,
                        LocalStrings.ChiselSupportGenericFluidSupport);

                supportedBlocks.put(blk, result);
                return result;
            }

            SupportsAnalysisResult result = null;
            if (!info.isCompatible) {
                result = new SupportsAnalysisResult(
                        false, LocalStrings.ChiselSupportCompatDeactivated, LocalStrings.ChiselSupportGenericSupported);
            } else if (!noCustomCollision) {
                result = new SupportsAnalysisResult(
                        false, LocalStrings.ChiselSupportCustomCollision, LocalStrings.ChiselSupportGenericSupported);
            } else if (info.hardness < -0.01f) {
                result = new SupportsAnalysisResult(
                        false, LocalStrings.ChiselSupportNoHardness, LocalStrings.ChiselSupportGenericSupported);
            } else if (!isNotSlab) {
                result = new SupportsAnalysisResult(
                        false, LocalStrings.ChiselSupportIsSlab, LocalStrings.ChiselSupportGenericSupported);
            } else if (!isFullBlock) {
                result = new SupportsAnalysisResult(
                        false, LocalStrings.ChiselSupportNotFullBlock, LocalStrings.ChiselSupportGenericSupported);
            } else if (hasBehavior) {
                result = new SupportsAnalysisResult(
                        false, LocalStrings.ChiselSupportHasBehaviour, LocalStrings.ChiselSupportGenericSupported);
            } else if (!quantityDroppedTest) {
                result = new SupportsAnalysisResult(
                        false, LocalStrings.ChiselSupportHasCustomDrops, LocalStrings.ChiselSupportGenericSupported);
            }

            supportedBlocks.put(blk, result);
            return result;
        } catch (final Throwable t) {
            final SupportsAnalysisResult result = new SupportsAnalysisResult(
                    false, LocalStrings.ChiselSupportFailureToAnalyze, LocalStrings.ChiselSupportGenericSupported);
            // if the above test fails for any reason, then the block cannot be
            // supported.
            supportedBlocks.put(blk, result);
            return result;
        }
    }

    public static boolean isSupported(final BlockState state) {
        return doSupportAnalysis(state).isSupported();
    }

    public static BlockBitInfo createFromState(final BlockState state) {
        try {
            final ReflectionHelperBlock reflectBlock = ModBlocks.REFLECTION_HELPER_BLOCK.get();
            final Block blk = state.getBlock();
            final Class<? extends Block> blkClass = blk.getClass();

            reflectBlock.getDestroyProgress(null, null, null, null);
            final Class<?> b_Class = ClassUtils.getDeclaringClass(
                    blkClass,
                    reflectBlock.getLastInvokedThreadLocalMethodName(),
                    BlockState.class,
                    Player.class,
                    BlockGetter.class,
                    BlockPos.class);
            final boolean test_b = b_Class == Block.class || b_Class == BlockBehaviour.class;

            reflectBlock.getExplosionResistance();
            Class<?> exploResistanceClz =
                    ClassUtils.getDeclaringClass(blkClass, reflectBlock.getLastInvokedThreadLocalMethodName());
            final boolean test_c = exploResistanceClz == Block.class || exploResistanceClz == BlockBehaviour.class;

            final boolean isFluid = fluidStates.containsKey(ModUtil.getStateId(state));

            // is it perfect?
            if (test_b && test_c && !isFluid) {
                final float blockHardness =
                        state.getDestroySpeed(new SingleBlockBlockReader(state, state.getBlock()), BlockPos.ZERO);
                final float resistance = blk.getExplosionResistance();

                return new BlockBitInfo(true, blockHardness, resistance);
            } else {
                // less accurate, we can just pretend they are some fixed
                // hardness... say like stone?

                final Block stone = Blocks.STONE;
                return new BlockBitInfo(
                        ChiselsAndBits.getConfig().getServer().compatabilityMode.get(), 2f, 6f);
            }
        } catch (final Exception err) {
            return new BlockBitInfo(false, -1, -1);
        }
    }

    public static boolean canChisel(final BlockState state) {
        return state.getBlock() instanceof BlockChiseled || isSupported(state);
    }

    public static boolean canChisel(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof ItemBlockChiseled) {
            return true;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            final Block block = blockItem.getBlock();
            final BlockState blockState = block.defaultBlockState();
            final BlockBitInfo.SupportsAnalysisResult result = BlockBitInfo.doSupportAnalysis(blockState);

            return result.supported;
        }

        return false;
    }

    public static boolean isChiseled(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.getItem() instanceof ItemBlockChiseled;
    }

    public static class SupportsAnalysisResult {
        private final boolean supported;
        private final LocalStrings unsupportedReason;
        private final LocalStrings supportedReason;

        public SupportsAnalysisResult(
                final boolean supported, final LocalStrings unsupportedReason, final LocalStrings supportedReason) {
            this.supported = supported;
            this.unsupportedReason = unsupportedReason;
            this.supportedReason = supportedReason;
        }

        public boolean isSupported() {
            return supported;
        }

        public LocalStrings getUnsupportedReason() {
            return unsupportedReason;
        }

        public LocalStrings getSupportedReason() {
            return supportedReason;
        }
    }
}
