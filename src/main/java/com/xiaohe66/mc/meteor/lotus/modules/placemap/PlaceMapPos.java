/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.math.BlockPos
 */
package com.xiaohe66.mc.meteor.lotus.modules.placemap;

import net.minecraft.core.BlockPos;

public class PlaceMapPos {
    private final BlockPos blockPos;
    private String name;
    private boolean done;

    public PlaceMapPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public PlaceMapPos(int x, int y, int z) {
        this.blockPos = new BlockPos(x, y, z);
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDone() {
        return this.done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
