/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.entity.BlockEntity
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StorageEspLineEvent extends Event {
    private BlockEntity blockEntity;

    public StorageEspLineEvent(BlockEntity blockEntity) {
        super(Stage.Pre);
        this.blockEntity = blockEntity;
    }

    public BlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
