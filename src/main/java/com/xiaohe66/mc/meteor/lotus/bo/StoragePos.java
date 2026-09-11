/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.math.BlockPos
 */
package com.xiaohe66.mc.meteor.lotus.bo;

import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import net.minecraft.core.BlockPos;

public class StoragePos {
    private final StorageItem item;
    private final BlockPos framePos;
    private final BlockPos putPos;
    private final BlockPos takePos;
    private final BlockPos kitPos;
    private final BlockPos btnPos;

    public StoragePos(StorageItem item, BlockPos framePos, BlockPos putPos, BlockPos takePos, BlockPos kitPos, BlockPos btnPos) {
        this.item = item;
        this.framePos = framePos;
        this.putPos = putPos;
        this.takePos = takePos;
        this.kitPos = kitPos;
        this.btnPos = btnPos;
    }

    public StorageItem getItem() {
        return this.item;
    }

    public BlockPos getFramePos() {
        return this.framePos;
    }

    public BlockPos getPutPos() {
        return this.putPos;
    }

    public BlockPos getTakePos() {
        return this.takePos;
    }

    public BlockPos getKitPos() {
        return this.kitPos;
    }

    public BlockPos getBtnPos() {
        return this.btnPos;
    }
}
