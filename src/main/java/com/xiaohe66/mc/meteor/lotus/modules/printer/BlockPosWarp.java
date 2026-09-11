/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.math.BlockPos
 */
package com.xiaohe66.mc.meteor.lotus.modules.printer;

import net.minecraft.core.BlockPos;

public class BlockPosWarp {
    private final BlockPos blockPos;
    private final double distance;

    public BlockPosWarp(BlockPos blockPos, double distance) {
        this.blockPos = blockPos;
        this.distance = distance;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public double getDistance() {
        return this.distance;
    }
}
