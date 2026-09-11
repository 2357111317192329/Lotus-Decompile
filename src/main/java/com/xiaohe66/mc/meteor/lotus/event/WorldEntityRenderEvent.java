/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.render.command.OrderedRenderCommandQueue
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.client.util.math.MatrixStack
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

public class WorldEntityRenderEvent extends Event {
    private static final WorldEntityRenderEvent INSTANCE = new WorldEntityRenderEvent();
    private PoseStack matrixStack;
    private SubmitNodeCollector commandQueue;
    private Vec3 pos;

    public WorldEntityRenderEvent() {
        super(Stage.Post);
    }

    public static WorldEntityRenderEvent get(PoseStack matrixStack, SubmitNodeCollector commandQueue, Vec3 pos) {
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.commandQueue = commandQueue;
        INSTANCE.pos = pos;
        return INSTANCE;
    }

    public PoseStack getMatrixStack() {
        return this.matrixStack;
    }

    public SubmitNodeCollector getCommandQueue() {
        return this.commandQueue;
    }

    public Vec3 getPos() {
        return this.pos;
    }
}

