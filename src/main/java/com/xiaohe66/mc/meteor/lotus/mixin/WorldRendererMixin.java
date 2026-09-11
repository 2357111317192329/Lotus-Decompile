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

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaohe66.mc.meteor.lotus.event.WorldEntityRenderEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LevelRenderer.class})
public abstract class WorldRendererMixin {
    @Inject(method={"submitEntities"}, at={@At(value="TAIL")})
    private void onPushEntityRenders(PoseStack matrices, LevelRenderState worldRenderState, SubmitNodeCollector renderQueue, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(WorldEntityRenderEvent.get(matrices, renderQueue, worldRenderState.cameraRenderState.pos));
    }
}

