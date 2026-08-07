package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

public class StepModule extends BaseModule {
   protected Step step = Steps.NONE;
   protected Step walkingNext;
   protected Step closeScreenAfter = Steps.NONE;
   protected int closeScreenAfterDelay;
   protected BooleanSupplier closeCheck = () -> true;
   protected Map<Step, Runnable> stepDispatch = new HashMap<>();

   public StepModule(String name, String description) {
      super(name, description);
      this.init();
   }

   public StepModule(String name, String description, int defaultDelay) {
      super(name, description, defaultDelay);
      this.init();
   }

   public StepModule(String name, String description, int defaultDelay, int max) {
      super(name, description, defaultDelay, max);
      this.init();
   }

   private void init() {
      this.addStep(Steps.NONE, this::none);
      this.addStep(Steps.CLOSE_SCREEN, this::closeScreen);
   }

   @EventHandler
   protected void onTick(Pre event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (this.checkAndDecrement()) {
            Runnable runnable = this.stepDispatch.get(this.step);
            if (runnable != null) {
               try {
                  runnable.run();
               } catch (Exception e) {
                  e.printStackTrace();
                  this.error("发生未知异常 : " + e.getMessage(), new Object[0]);
                  this.toggle();
               }
            } else {
               this.warning("未处理状态 : " + this.step.getName(), new Object[0]);
               this.toggle();
            }
         }
      }
   }

   protected void closeNext(Step nextStep) {
      this.closeScreenAfter = nextStep;
      this.step = Steps.CLOSE_SCREEN;
   }

   protected void closeScreen() {
      if (!this.closeCheck.getAsBoolean()) {
         this.setDelay();
      } else {
         HeInvUtils.closeCurScreen();
         if (this.closeScreenAfter != null) {
            this.step = this.closeScreenAfter;
         }

         if (this.closeScreenAfterDelay > 0) {
            this.setDelay(this.closeScreenAfterDelay);
            this.closeScreenAfterDelay = 0;
         } else {
            this.setDelay();
         }
      }
   }

   protected void setCloseScreenAfterDelay(int closeScreenAfterDelay) {
      this.closeScreenAfterDelay = closeScreenAfterDelay;
   }

   protected void delayCloseNext(Step nextStep) {
      this.delayCloseNext(nextStep, () -> true);
   }

   protected void delayCloseNext(Step nextStep, BooleanSupplier closeCheck) {
      this.closeCheck = closeCheck;
      this.closeNext(nextStep);
      this.setDelay();
   }

   protected void delayNext(Step nextStep) {
      this.step = nextStep;
      this.setDelay();
   }

   protected void none() {
   }

   protected void breakStep(String msg) {
      this.warning(msg, new Object[0]);
      this.step = Steps.NONE;
      this.closeScreenAfter = Steps.NONE;
   }

   protected void addStep(Step step, Runnable runnable) {
      this.stepDispatch.put(step, runnable);
   }

   protected boolean notInOperationRange(StoragePos pos) {
      return this.mc.player.position().distanceTo(pos.getBtnPos().getCenter()) > 1.0;
   }

   protected StoragePos checkAndBuildStoragePos(ItemFrame frame, StorageItem storageItem) {
      BlockPos framePos = frame.blockPosition();
      BlockPos attachedBlockPos = frame.getPos();
      BlockPos putPos = attachedBlockPos.offset(0, 1, 0).relative(frame.getNearestViewDirection().getOpposite());
      if (this.mc.level.getBlockState(putPos).getBlock() != Blocks.CHEST) {
         return null;
      }

      BlockPos kitPos = framePos.offset(0, -2, 0);
      BlockState kitPosBlockState = this.mc.level.getBlockState(kitPos);
      if (!kitPosBlockState.isAir() && !(kitPosBlockState.getBlock() instanceof ShulkerBoxBlock)) {
         return null;
      }

      BlockPos pistonPos = kitPos.offset(0, -1, 0);
      if (this.mc.level.getBlockState(pistonPos).getBlock() != Blocks.PISTON) {
         return null;
      }

      if (kitPos.getY() != this.mc.player.blockPosition().getY()) {
         return null;
      }

      BlockPos btnPos = kitPos.relative(frame.getNearestViewDirection());
      BlockPos takePos = putPos.offset(0, -2, 0);
      return !(this.mc.level.getBlockState(btnPos).getBlock() instanceof ButtonBlock)
         ? null
         : new StoragePos(storageItem, framePos, putPos, takePos, kitPos, btnPos);
   }
}
