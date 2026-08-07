package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.block.entity.BlockEntity;

public class StorageEspLineEvent extends Event {
   private BlockEntity blockEntity;

   public StorageEspLineEvent(BlockEntity blockEntity) {
      super(Event.Stage.Pre);
      this.blockEntity = blockEntity;
   }

   public BlockEntity getBlockEntity() {
      return this.blockEntity;
   }
}
