package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.StorageFinderEvent;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.world.StashFinder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;

@Mixin(value = StashFinder.class, remap = false)
public class MixinStashFinder {
   @Shadow
   @Final
   private Setting<List<BlockEntityType<?>>> storageBlocks;
   @Unique
   private BlockEntity blockEntity;

   @Redirect(method = "onChunkData", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"))
   private boolean isStorage(List<?> list, Object element) {
      boolean isStorage = ((List)this.storageBlocks.get()).contains(element);
      if (isStorage) {
         StorageFinderEvent event = StorageFinderEvent.get(this.blockEntity);
         MeteorClient.EVENT_BUS.post(event);
         if (event.isCancel()) {
            isStorage = false;
         }
      }

      return isStorage;
   }

   @ModifyVariable(method = "onChunkData", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", shift = Shift.BEFORE))
   private BlockEntity captureBlockEntity(BlockEntity blockEntity) {
      this.blockEntity = blockEntity;
      return blockEntity;
   }
}
