package mod.chiselsandbits.render.patterns;

import mod.chiselsandbits.client.model.baked.BaseBakedItemModel;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.interfaces.IPatternItem;
import mod.chiselsandbits.render.NullBakedModel;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class PrintBaked extends BaseBakedItemModel {

    final String itemName;

    public PrintBaked(final String itname, final IPatternItem item, final ItemStack stack) {
        itemName = itname;

        final ItemStack blockItem = item.getPatternedItem(stack, false);
        final LegacyBakedModel model =
                new ChiseledBlockSmartModel().resolve(NullBakedModel.instance, blockItem, null, null);

        for (final Direction face : Direction.values()) {
            list.addAll(model.getQuads(null, face, RANDOM));
        }

        list.addAll(model.getQuads(null, null, RANDOM));
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(Identifier.fromNamespaceAndPath(ChiselsAndBits.MODID, "item/" + itemName));
    }
}
