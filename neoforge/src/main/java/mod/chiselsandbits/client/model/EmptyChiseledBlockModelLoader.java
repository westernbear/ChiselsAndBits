package mod.chiselsandbits.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import mod.chiselsandbits.core.ChiselsAndBits;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/** Loads {@code "loader": "chiselsandbits:chiseled_block"} model files as empty placeholders. */
public final class EmptyChiseledBlockModelLoader implements UnbakedModelLoader<EmptyChiseledBlockModel> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ChiselsAndBits.MODID, "chiseled_block");

    @Override
    public EmptyChiseledBlockModel read(final JsonObject json, final JsonDeserializationContext context) {
        return EmptyChiseledBlockModel.INSTANCE;
    }
}
