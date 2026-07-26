package mod.chiselsandbits.mixin.compat.client;

import mod.chiselsandbits.compat.client.OverlayRenderCallback;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Hud.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private float partialTicks;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void extractRenderState(
            GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
    }

    // This might be the wrong method to inject to
    @Inject(
            method = "extractPlayerHealth",
            at =
                    @At(
                            value = "INVOKE",
                            shift = At.Shift.AFTER,
                            target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V"),
            cancellable = true)
    private void renderStatusBars(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
        if (OverlayRenderCallback.EVENT
                .invoker()
                .onOverlayRender(guiGraphics, partialTicks, minecraft.getWindow(), OverlayRenderCallback.Types.AIR)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractHearts", at = @At(value = "HEAD"), cancellable = true)
    private void renderHealth(
            GuiGraphicsExtractor guiGraphics,
            Player player,
            int i,
            int j,
            int k,
            int l,
            float f,
            int m,
            int n,
            int o,
            boolean bl,
            CallbackInfo ci) {
        if (OverlayRenderCallback.EVENT
                .invoker()
                .onOverlayRender(
                        guiGraphics, partialTicks, minecraft.getWindow(), OverlayRenderCallback.Types.PLAYER_HEALTH)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void renderCrosshair(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (OverlayRenderCallback.EVENT
                .invoker()
                .onOverlayRender(
                        guiGraphics, partialTicks, minecraft.getWindow(), OverlayRenderCallback.Types.CROSSHAIRS)) {
            ci.cancel();
        }
    }
}
