/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  net.minecraft.client.render.state.WorldRenderState
 *  net.minecraft.client.render.command.OrderedRenderCommandQueue
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.client.render.WorldRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.WorldEntityRenderEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={WorldRenderer.class})
public abstract class WorldRendererMixin {
    @Inject(method={"pushEntityRenders"}, at={@At(value="TAIL")})
    private void onPushEntityRenders(MatrixStack matrices, WorldRenderState worldRenderState, OrderedRenderCommandQueue renderQueue, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(WorldEntityRenderEvent.get(matrices, renderQueue, worldRenderState.cameraRenderState.pos));
    }
}

