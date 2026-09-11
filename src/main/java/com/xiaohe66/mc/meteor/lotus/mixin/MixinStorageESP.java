/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.renderer.Renderer3D
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.systems.modules.render.StorageESP
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  net.minecraft.block.entity.BlockEntity
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.StorageEspLineEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.render.StorageESP;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={StorageESP.class}, remap=false)
public abstract class MixinStorageESP {
    @Shadow
    @Final
    private Setting<Boolean> tracers;
    @Unique
    private BlockEntity blockEntity;

    @Redirect(method={"onRender(Lmeteordevelopment/meteorclient/events/render/Render3DEvent;)V"}, at=@At(value="INVOKE", target="Lmeteordevelopment/meteorclient/renderer/Renderer3D;line(DDDDDDLmeteordevelopment/meteorclient/utils/render/color/Color;)V"))
    private void injectInvokeLine(Renderer3D renderer, double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        if (this.tracers.get() && this.blockEntity != null) {
            StorageEspLineEvent event = new StorageEspLineEvent(this.blockEntity);
            MeteorClient.EVENT_BUS.post(event);
            if (event.isCancel()) {
                return;
            }
        }
        renderer.line(x1, y1, z1, x2, y2, z2, color, color);
    }

    @Inject(method={"getBlockEntityColor"}, at={@At(value="HEAD")})
    private void injectGetBlockEntityColor(BlockEntity blockEntity, CallbackInfo info) {
        this.blockEntity = blockEntity;
    }
}

