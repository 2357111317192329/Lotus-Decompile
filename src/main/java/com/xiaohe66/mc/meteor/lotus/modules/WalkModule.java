/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  baritone.api.BaritoneAPI
 *  baritone.api.IBaritone
 *  baritone.api.Settings
 *  baritone.api.event.events.PathEvent
 *  baritone.api.event.listener.AbstractGameEventListener
 *  baritone.api.event.listener.IGameEventListener
 *  baritone.api.pathing.goals.Goal
 *  baritone.api.pathing.goals.GoalNear
 *  baritone.api.process.ICustomGoalProcess
 *  net.minecraft.util.math.BlockPos
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.event.listener.IGameEventListener;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.ICustomGoalProcess;
import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import net.minecraft.util.math.BlockPos;
import com.xiaohe66.mc.meteor.lotus.modules.StepModule;

import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;

public class WalkModule extends StepModule implements AbstractGameEventListener {
    protected static final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    protected static final ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
    protected static final Settings baritoneSettings = BaritoneAPI.getSettings();

    public WalkModule(String title, String description) {
        super(title, description);
        this.init();
    }

    public WalkModule(String title, String description, int range) {
        super(title, description, range);
        this.init();
    }

    public WalkModule(String title, String description, int range, int max) {
        super(title, description, range, max);
        this.init();
    }

    private void init() {
        baritone.getGameEventHandler().registerEventListener((IGameEventListener)this);
        this.addStep(Steps.WALKING, this::none);
    }

    public void onActivate() {
        super.onActivate();
        WalkModule.baritoneSettings.allowBreak.value = false;
        WalkModule.baritoneSettings.allowPlace.value = false;
    }

    public void onPathEvent(PathEvent pathEvent) {
        if (pathEvent == PathEvent.CANCELED && this.walkingNext != null) {
            this.step = this.walkingNext;
            this.walkingNext = null;
        }
    }

    protected void gotoTargetIfNeed(BlockPos targetPos, int range, Step walkingNext, String gotoTargetMsg) {
        double distance = this.mc.player.getEntityPos().distanceTo(targetPos.toCenterPos());
        if (distance > 1000.0) {
            this.warning("移动距离超过1000格, 功能关闭", new Object[0]);
            this.toggle();
            return;
        }
        if (distance > (double)range) {
            if (gotoTargetMsg != null) {
                this.info(gotoTargetMsg, new Object[0]);
            }
            this.walkingNext = walkingNext;
            this.gotoTarget(targetPos, range);
        } else {
            this.step = walkingNext;
        }
    }

    protected void gotoTarget(BlockPos targetPos, int range) {
        customGoalProcess.setGoalAndPath((Goal)new GoalNear(targetPos, range));
        this.step = Steps.WALKING;
    }

    protected void gotoTarget(BlockPos targetPos, int range, Step walkingNext) {
        this.walkingNext = walkingNext;
        this.gotoTarget(targetPos, range);
    }

    protected void gotoBtnPos(StoragePos pos, String message, Step walkingNext) {
        this.info(message, new Object[0]);
        this.gotoTarget(pos.getBtnPos(), 0);
        this.walkingNext = walkingNext;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        baritone.getCommandManager().execute("cancel");
        WalkModule.baritoneSettings.allowBreak.value = true;
        WalkModule.baritoneSettings.allowPlace.value = true;
    }
}

