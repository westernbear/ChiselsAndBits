package mod.chiselsandbits.mixin.compat.client;

import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.compat.client.GameMouseEvents;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(
            method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
            cancellable = true)
    private void chiselsandbits$compat$beforeScroll(
            long window,
            double xOffset,
            double yOffset,
            CallbackInfo ci,
            @Local(ordinal = 1) double deltaX,
            @Local(ordinal = 2) double deltaY) {
        if (GameMouseEvents.BEFORE_SCROLL.invoker().wheelScroll(deltaX, deltaY)) {
            ci.cancel();
        }
    }
}
