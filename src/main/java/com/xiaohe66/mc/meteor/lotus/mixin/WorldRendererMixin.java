/*
 * 1.21.8-compatible port of the WorldRenderer mixin.
 * 
 * The 1.21.11 version hooked WorldRenderer.pushEntityRenders(MatrixStack,
 * WorldRenderState, OrderedRenderCommandQueue), which does not exist in 1.21.8.
 * In 1.21.8 the world is rendered through a frame graph inside
 * WorldRenderer.render(ObjectAllocator, RenderTickCounter, boolean, Camera,
 * Matrix4f, Matrix4f, GpuBufferSlice, Vector4f, boolean); world-space vertices
 * are written camera-relative (world pos - camera pos) and the camera rotation
 * is applied through the model view matrix (positionMatrix) at draw time.
 * 
 * We post the event at the tail of render(), with the positionMatrix pushed
 * onto the model view stack so that handlers drawing into the entity vertex
 * consumers are transformed exactly like vanilla entities. The entity
 * consumers are already empty at this point (drawn inside the frame graph),
 * so flushing them draws only the mod's own marks.
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xiaohe66.mc.meteor.lotus.event.WorldEntityRenderEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={WorldRenderer.class})
public abstract class WorldRendererMixin {
    @Inject(method={"render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"}, at={@At(value="TAIL")})
    private void onRenderTail(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world == null) {
            return;
        }
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(positionMatrix);
        MeteorClient.EVENT_BUS.post(WorldEntityRenderEvent.get(new MatrixStack(), camera.getPos()));
        MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
        RenderSystem.getModelViewStack().popMatrix();
    }
}
