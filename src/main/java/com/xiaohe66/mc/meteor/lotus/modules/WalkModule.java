package com.xiaohe66.mc.meteor.lotus.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.ICustomGoalProcess;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import net.minecraft.core.BlockPos;

public class WalkModule extends StepModule implements AbstractGameEventListener {
   protected static final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
   protected static final ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
   protected static final Settings baritoneSettings = BaritoneAPI.getSettings();

   public WalkModule(String name, String description) {
      super(name, description);
      this.init();
   }

   public WalkModule(String name, String description, int defaultDelay) {
      super(name, description, defaultDelay);
      this.init();
   }

   public WalkModule(String name, String description, int defaultDelay, int max) {
      super(name, description, defaultDelay, max);
      this.init();
   }

   private void init() {
      baritone.getGameEventHandler().registerEventListener(this);
      this.addStep(Steps.WALKING, this::none);
   }

   public void onActivate() {
      super.onActivate();
      baritoneSettings.allowBreak.value = false;
      baritoneSettings.allowPlace.value = false;
   }

   public void onPathEvent(PathEvent event) {
      if (event == PathEvent.CANCELED && this.walkingNext != null) {
         this.step = this.walkingNext;
         this.walkingNext = null;
      }
   }

   protected void gotoTargetIfNeed(BlockPos targetPos, int range, Step walkingNext, String gotoTargetMsg) {
      double distance = this.mc.player.position().distanceTo(targetPos.getCenter());
      if (distance > 1000.0) {
         this.warning("移动距离超过1000格, 功能关闭", new Object[0]);
         this.toggle();
      } else {
         if (distance > range) {
            if (gotoTargetMsg != null) {
               this.info(gotoTargetMsg, new Object[0]);
            }

            this.walkingNext = walkingNext;
            this.gotoTarget(targetPos, range);
         } else {
            this.step = walkingNext;
         }
      }
   }

   protected void gotoTarget(BlockPos targetPos, int distance) {
      customGoalProcess.setGoalAndPath(new GoalNear(targetPos, distance));
      this.step = Steps.WALKING;
   }

   protected void gotoTarget(BlockPos targetPos, int distance, Step walkingNext) {
      this.walkingNext = walkingNext;
      this.gotoTarget(targetPos, distance);
   }

   protected void gotoBtnPos(StoragePos pos, String msg, Step nextStep) {
      this.info(msg, new Object[0]);
      this.gotoTarget(pos.getBtnPos(), 0);
      this.walkingNext = nextStep;
   }

   public void onDeactivate() {
      baritone.getCommandManager().execute("cancel");
      baritoneSettings.allowBreak.value = true;
      baritoneSettings.allowPlace.value = true;
   }
}
