/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.math.BlockPos
 */
package com.xiaohe66.mc.meteor.lotus.modules.redstone;

import net.minecraft.core.BlockPos;

public class Marker {
    public double x;
    public double y;
    public double z;
    public boolean safe;
    public int lightLevel;

    public Marker set(BlockPos blockPos, boolean safe, int lightLevel) {
        this.x = blockPos.getX();
        this.y = blockPos.getY();
        this.z = blockPos.getZ();
        this.safe = safe;
        this.lightLevel = lightLevel;
        return this;
    }
}

