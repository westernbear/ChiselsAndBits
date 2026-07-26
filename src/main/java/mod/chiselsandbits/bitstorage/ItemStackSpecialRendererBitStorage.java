package mod.chiselsandbits.bitstorage;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class ItemStackSpecialRendererBitStorage extends BlockEntityWithoutLevelRenderer {
    public ItemStackSpecialRendererBitStorage() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext itemDisplayContext,
            PoseStack matrixStack,
            MultiBufferSource buffer,
            int combinedLight,
            int combinedOverlay) {
        final BakedModel model = Minecraft.getInstance()
                .getModelManager()
                .getModel(new ModelResourceLocation(
                        BuiltInRegistries.BLOCK.getKey(ModBlocks.BIT_STORAGE_BLOCK.get()), "facing=east"));

        Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        matrixStack.last(),
                        buffer.getBuffer(RenderType.translucent()),
                        ModBlocks.BIT_STORAGE_BLOCK.get().defaultBlockState(),
                        model,
                        1f,
                        1f,
                        1f,
                        combinedLight,
                        combinedOverlay);

        final TileEntityBitStorage tileEntity = new TileEntityBitStorage(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        tileEntity.setFluid(ItemBlockBitStorage.getFluidVariant(stack), ItemBlockBitStorage.getFluidAmount(stack));
        Minecraft.getInstance()
                .getBlockEntityRenderDispatcher()
                .renderItem(tileEntity, matrixStack, buffer, combinedLight, combinedOverlay);
    }
}
