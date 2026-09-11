/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.render.command.OrderedRenderCommandQueue
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.client.util.math.MatrixStack
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;

public class WorldEntityRenderEvent extends Event {
    private static final WorldEntityRenderEvent INSTANCE = new WorldEntityRenderEvent();
    private MatrixStack matrixStack;
    private OrderedRenderCommandQueue commandQueue;
    private Vec3d pos;

    public WorldEntityRenderEvent() {
        super(Stage.Post);
    }

    public static WorldEntityRenderEvent get(MatrixStack matrixStack, OrderedRenderCommandQueue commandQueue, Vec3d pos) {
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.commandQueue = commandQueue;
        INSTANCE.pos = pos;
        return INSTANCE;
    }

    public MatrixStack getMatrixStack() {
        return this.matrixStack;
    }

    public OrderedRenderCommandQueue getCommandQueue() {
        return this.commandQueue;
    }

    public Vec3d getPos() {
        return this.pos;
    }
}

