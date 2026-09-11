/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={VanillaTextRenderer.class}, remap=false)
public class VanillaTextRendererMixin {
    @Shadow
    public boolean scaleIndividually;

    @Inject(method={"render"}, at={@At(value="HEAD")})
    public void render(String text, double x, double y, Color color, boolean shadow, CallbackInfoReturnable<Double> cir) {
        this.scaleIndividually = true;
    }
}

