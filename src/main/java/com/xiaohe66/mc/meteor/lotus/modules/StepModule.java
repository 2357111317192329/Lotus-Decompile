/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.world.TickEvent$Pre
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.KeybindSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.utils.misc.Keybind
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.entity.decoration.ItemFrameEntity
 *  net.minecraft.world.GameMode
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.ButtonBlock
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.block.ShulkerBoxBlock
 *  net.minecraft.block.BlockState
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;
import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;

import com.xiaohe66.mc.meteor.lotus.util.TaskUtils;
import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.GameType;
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
    protected Map<Step, Runnable> stepDispatch = new HashMap<Step, Runnable>();
    protected final Setting<Keybind> quickStopKeybind;
    protected ScheduledFuture<?> delayTask;
    protected final Setting<Integer> restartDelay = sgGeneral.add(new IntSetting.Builder()
        .name("重启延时")
        .description("重启延时（秒）")
        .min(1)
        .sliderMax(200)
        .defaultValue(2)
        .build());

    public StepModule(String name, String description) {
        super(name, description);
        this.init();
        this.quickStopKeybind = this.createKeybindSetting();
    }

    public StepModule(String name, String description, int defaultDelay) {
        super(name, description, defaultDelay);
        this.init();
        this.quickStopKeybind = this.createKeybindSetting();
    }

    public StepModule(String name, String description, int defaultDelay, int max) {
        super(name, description, defaultDelay, max);
        this.init();
        this.quickStopKeybind = this.createKeybindSetting();
    }

    private void init() {
        this.addStep(Steps.NONE, this::none);
        this.addStep(Steps.CLOSE_SCREEN, this::closeScreen);
    }

    private Setting<Keybind> createKeybindSetting() {
        if (!this.useQuickStopKeybind()) {
            return null;
        }
        return sgGeneral.add(new KeybindSetting.Builder()
            .name("快速停止键")
            .description("按下后关闭当前模块")
            .defaultValue(Keybind.fromKey(256))
            .visible(this::useQuickStopKeybind)
            .action(this::onQuickStopKeybind)
            .build());
    }

    protected boolean useQuickStopKeybind() {
        return false;
    }

    protected boolean allowQuickStop() {
        return true;
    }

    private void onQuickStopKeybind() {
        if (this.isActive() && this.allowQuickStop()) {
            this.toggle();
            this.warning("已快速关闭", new Object[0]);
        }
    }

    @EventHandler
    protected void onTick(TickEvent.Pre event) {
        if (!this.isReady() || !this.isActive()) {
            return;
        }
        if (!this.checkAndDecrement()) {
            return;
        }
        Runnable runnable = this.stepDispatch.get(this.step);
        if (runnable != null) {
            try {
                runnable.run();
            }
            catch (Throwable throwable) {
                throwable.printStackTrace();
                this.error("发生未知异常 : " + throwable.getMessage(), new Object[0]);
                this.toggle();
            }
        } else {
            this.warning("未处理状态 : " + this.step.getName(), new Object[0]);
            this.toggle();
        }
    }

    protected void closeNext(Step nextStep) {
        this.closeScreenAfter = nextStep;
        this.step = Steps.CLOSE_SCREEN;
    }

    protected void closeScreen() {
        if (!this.closeCheck.getAsBoolean()) {
            this.setDelay();
            return;
        }
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

    protected void stopTask() {
        if (this.delayTask != null) {
            this.delayTask.cancel(false);
            this.delayTask = null;
        }
    }

    protected void delayStart() {
        this.delayStart(Steps.NEXT);
    }

    protected void delayStart(Step nextStep) {
        this.stopTask();
        GameType gameMode = this.mc.gameMode.getPlayerMode();
        if (gameMode == GameType.SPECTATOR || gameMode == GameType.ADVENTURE) {
            return;
        }
        this.delayTask = TaskUtils.run(() -> {
            this.info("启动", new Object[0]);
            this.step = nextStep;
            this.delayTask = null;
        }, this.restartDelay.get(), TimeUnit.SECONDS);
    }

    protected void stop() {
        this.step = Steps.NONE;
        this.stopTask();
    }

    public void onDeactivate() {
        this.stop();
    }

    protected boolean notInOperationRange(StoragePos pos) {
        return this.mc.player.position().distanceTo(pos.getBtnPos().getCenter()) > 1.0;
    }

    protected boolean isTooFar(BlockPos blockPos, double range) {
        return this.mc.player.position().distanceTo(blockPos.getCenter()) > range;
    }

    protected boolean isEyeTooFar(BlockPos blockPos, double range) {
        return this.mc.player.getEyePosition().distanceTo(blockPos.getCenter()) > range;
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
        if (!(this.mc.level.getBlockState(btnPos).getBlock() instanceof ButtonBlock)) {
            return null;
        }
        return new StoragePos(storageItem, framePos, putPos, takePos, kitPos, btnPos);
    }
}
