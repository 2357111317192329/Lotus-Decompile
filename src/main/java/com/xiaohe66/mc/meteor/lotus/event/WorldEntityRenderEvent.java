/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.client.util.math.MatrixStack
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class WorldEntityRenderEvent extends Event {
    private static final WorldEntityRenderEvent INSTANCE = new WorldEntityRenderEvent();
    private MatrixStack matrixStack;
    private Vec3d pos;

    public WorldEntityRenderEvent() {
        super(Stage.Post);
    }

    public static WorldEntityRenderEvent get(MatrixStack matrixStack, Vec3d pos) {
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.pos = pos;
        return INSTANCE;
    }

    public MatrixStack getMatrixStack() {
        return this.matrixStack;
    }

    public Vec3d getPos() {
        return this.pos;
    }
}
