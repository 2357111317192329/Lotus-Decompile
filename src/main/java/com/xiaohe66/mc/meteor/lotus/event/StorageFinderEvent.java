/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.entity.BlockEntity
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.block.entity.BlockEntity;

public class StorageFinderEvent extends Event {
    private static final StorageFinderEvent INSTANCE = new StorageFinderEvent();
    private BlockEntity blockEntity;

    private StorageFinderEvent() {
        super(Stage.Post);
    }

    public static StorageFinderEvent get(BlockEntity blockEntity) {
        INSTANCE.blockEntity = blockEntity;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }

    public BlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
