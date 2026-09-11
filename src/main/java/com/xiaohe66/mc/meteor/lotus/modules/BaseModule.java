/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.utils.player.FindItemResult
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  net.minecraft.util.Hand
 *  net.minecraft.util.ActionResult
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.entity.player.PlayerInventory
 *  net.minecraft.entity.projectile.ProjectileUtil
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.GenericContainerScreenHandler
 *  net.minecraft.screen.PlayerScreenHandler
 *  net.minecraft.screen.ShulkerBoxScreenHandler
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.hit.EntityHitResult
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;

import java.util.function.Consumer;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.EntityHitResult;

public abstract class BaseModule
extends Module {
    protected final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Integer> delay;
    private int delayTimer;
    private int curPlayerSlot;
    private int curScreenSlot;
    private Step openStatus;
    private long lastOpenTime;
    private BlockPos lastBlockPos;

    public BaseModule(String name, String description) {
        this(name, description, 5);
    }

    public BaseModule(String name, String description, int defaultDelay) {
        this(name, description, defaultDelay, 40);
    }

    public BaseModule(String name, String description, int defaultDelay, int max) {
        super(Const.CATEGORY, name, description);
        this.openStatus = Steps.IDLE;
        this.lastOpenTime = 0L;
        boolean visible = defaultDelay >= 0;
        this.delay = sgGeneral.add(new IntSetting.Builder()
            .name("延迟")
            .min(0)
            .sliderMax(max)
            .defaultValue(defaultDelay)
            .visible(() -> visible)
            .build());
    }

    protected boolean checkAndDecrement() {
        if (this.delayTimer > 0) {
            --this.delayTimer;
            return false;
        }
        return true;
    }

    protected PlayerInventory getPlayerInventory() {
        return this.mc.player.getInventory();
    }

    protected ItemStack getItemStack(int slot) {
        return this.getPlayerInventory().getStack(slot);
    }

    protected ItemStack getItemStackBySlotId(int slotId) {
        ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
        return screenHandler.getSlot(slotId).getStack();
    }

    protected ItemStack nextPlayerStack(Predicate<ItemStack> predicate) {
        int startSlot = this.curPlayerSlot;
        int maxSlot = this.getPlayerMainSize() - 1;
        do {
            this.curPlayerSlot = this.curPlayerSlot >= maxSlot ? 0 : ++this.curPlayerSlot;
            ItemStack itemStack = this.getItemStack(this.curPlayerSlot);
            if (itemStack.isEmpty() || !predicate.test(itemStack)) continue;
            return itemStack;
        } while (this.curPlayerSlot != startSlot);
        return ItemStack.EMPTY;
    }

    protected ItemStack nextScreenStack(Predicate<ItemStack> predicate) {
        ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
        int maxSlot = this.getScreenMainSize() - 1;
        int startSlot = this.curScreenSlot;
        do {
            this.curScreenSlot = this.curScreenSlot >= maxSlot ? 0 : ++this.curScreenSlot;
            ItemStack itemStack = screenHandler.getSlot(this.curScreenSlot).getStack();
            if (itemStack.isEmpty() || !predicate.test(itemStack)) continue;
            return itemStack;
        } while (this.curScreenSlot != startSlot);
        return ItemStack.EMPTY;
    }

    protected boolean hasScreenFull() {
        ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
        int size = this.getScreenMainSize();
        for (int i = 0; i < size; ++i) {
            ItemStack stack = screenHandler.getSlot(i).getStack();
            if (!stack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    protected void openChest(BlockPos blockPos, Consumer<ScreenHandler> consumer) {
        this.openKit(blockPos, null, consumer);
    }

    protected void openKit(BlockPos blockPos, Direction direction, Consumer<ScreenHandler> consumer) {
        ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
        if (screenHandler instanceof PlayerScreenHandler || !blockPos.equals(this.lastBlockPos)) {
            this.tryRotateAndOpen(blockPos, direction);
        } else if (screenHandler instanceof GenericContainerScreenHandler || screenHandler instanceof ShulkerBoxScreenHandler) {
            this.openStatus = Steps.IDLE;
            consumer.accept(screenHandler);
        } else {
            this.openStatus = Steps.IDLE;
            HeInvUtils.closeCurScreen();
            this.warning("打开的容器错误", new Object[0]);
            this.setDelay();
        }
    }

    protected void rotateAndOpen(BlockPos blockPos) {
        this.rotateAndOpen(blockPos, (Direction)null);
    }

    protected void rotateAndOpen(BlockPos blockPos, Direction direction) {
        Direction finalDirection = direction == null ? BlockUtils.getDirection((BlockPos)blockPos) : direction;
        Vec3i vector = finalDirection.getVector();
        double offset = 0.45;
        Vec3d lookPos = new Vec3d((double)blockPos.getX() + 0.5 + (double)vector.getX() * offset, (double)blockPos.getY() + 0.5 + (double)vector.getY() * offset, (double)blockPos.getZ() + 0.5 + (double)vector.getZ() * offset);
        HeRotationUtils.rotate(lookPos, () -> HeBlockUtils.open(blockPos, finalDirection, lookPos));
    }

    private void tryRotateAndOpen(BlockPos blockPos, Direction direction) {
        long now = System.currentTimeMillis();
        if (this.openStatus == Steps.IDLE || now - this.lastOpenTime > 1000L) {
            if (direction == null) {
                HeBlockUtils.open(blockPos);
            } else {
                HeBlockUtils.open(blockPos, direction);
            }
            this.openStatus = Steps.OPENING;
            this.lastOpenTime = now;
            this.lastBlockPos = new BlockPos((Vec3i)blockPos);
        } else {
            this.warning("打开没有反应...", new Object[0]);
        }
        this.setDelay();
    }

    protected void rotateAndOpenWithOffset(BlockPos blockPos, Direction direction) {
        Vec3d lookPos;
        if (direction == Direction.UP) {
            double playerX = this.mc.player.getX();
            double playerZ = this.mc.player.getZ();
            double blockX = (double)blockPos.getX() + 0.5;
            double blockZ = (double)blockPos.getZ() + 0.5;
            double dx = Math.abs(playerX - blockX);
            double dz = Math.abs(playerZ - blockZ);
            double offset = 0.4;
            double xOffset = 0.5;
            double zOffset = 0.5;
            if (dx > dz) {
                xOffset = playerX > blockX ? 0.5 + offset : 0.5 - offset;
            } else {
                zOffset = playerZ > blockZ ? 0.5 + offset : 0.5 - offset;
            }
            double yPos = (double)blockPos.getY() + 0.95;
            lookPos = new Vec3d((double)blockPos.getX() + xOffset, yPos, (double)blockPos.getZ() + zOffset);
        } else {
            Vec3i vector = direction.getVector();
            double offset = 0.5;
            lookPos = new Vec3d((double)blockPos.getX() + 0.5 + (double)vector.getX() * offset, (double)blockPos.getY() + 0.5 + (double)vector.getY() * offset, (double)blockPos.getZ() + 0.5 + (double)vector.getZ() * offset);
        }
        HeRotationUtils.rotate(lookPos, () -> HeBlockUtils.open(blockPos, direction, lookPos));
    }

    protected void interactEntity(Entity entity) {
        Vec3d lookPos;
        Vec3d playerPos = this.mc.player.getEntityPos();
        EntityHitResult entityHitResult = ProjectileUtil.raycast((Entity)this.mc.player, (Vec3d)playerPos, (Vec3d)(lookPos = entity.getEntityPos()), (Box)entity.getBoundingBox(), Entity::canHit, (double)playerPos.squaredDistanceTo(lookPos));
        if (entityHitResult == null) {
            HeRotationUtils.keepRotation(entity.getEyePos());
            this.mc.interactionManager.interactEntity((PlayerEntity)this.mc.player, entity, Hand.MAIN_HAND);
        } else {
            HeRotationUtils.keepRotation(entity.getEyePos());
            ActionResult actionResult = this.mc.interactionManager.interactEntityAtLocation((PlayerEntity)this.mc.player, entity, entityHitResult, Hand.MAIN_HAND);
            if (!actionResult.isAccepted()) {
                this.mc.interactionManager.interactEntity((PlayerEntity)this.mc.player, entity, Hand.MAIN_HAND);
            }
        }
    }

    protected boolean ensureItemInMainHand(Predicate<ItemStack> predicate, String notFoundMessage) {
        FindItemResult findItemResult = InvUtils.find(predicate);
        if (!findItemResult.found()) {
            this.error(notFoundMessage, new Object[0]);
            this.toggle();
            return false;
        }
        if (!findItemResult.isMainHand()) {
            HeInvUtils.swap(findItemResult.slot(), this.getMainSlot());
            return false;
        }
        return true;
    }

    protected boolean swapToMainHand(int slot) {
        if (slot != this.getMainSlot()) {
            HeInvUtils.swap(slot, this.getMainSlot());
            this.setDelay();
            return true;
        }
        return false;
    }

    protected int getMainSlot() {
        return HeInvUtils.getMainSlot();
    }

    protected boolean isReady() {
        return this.mc.player != null && this.mc.world != null;
    }

    protected long mcTime() {
        return this.mc.world.getTimeOfDay();
    }

    protected long mcDay() {
        return this.mc.world.getTimeOfDay() / 24000L;
    }

    protected long mcTimeOfDay() {
        return this.mc.world.getTimeOfDay() % 24000L;
    }

    protected void setDelay() {
        this.delayTimer = this.delay.get();
    }

    protected void setDelay(int delay) {
        this.delayTimer = delay;
    }

    public int getDelayTimer() {
        return this.delayTimer;
    }

    public int getPlayerMainSize() {
        return this.mc.player.getInventory().getMainStacks().size();
    }

    public int getScreenMainSize() {
        if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            return this.mc.player.currentScreenHandler.slots.size() - this.getPlayerMainSize() - 1;
        }
        return this.mc.player.currentScreenHandler.slots.size() - this.getPlayerMainSize();
    }

    public boolean isContainer(int slotId) {
        return slotId < this.getScreenMainSize();
    }

    public int getFirstEmptySlot() {
        PlayerInventory playerInventory = this.getPlayerInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = playerInventory.getStack(i);
            if (!itemStack.isEmpty()) continue;
            return i;
        }
        return -1;
    }

    public boolean isContainerFull() {
        int size = this.getScreenMainSize();
        ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
        for (int i = 0; i < size; ++i) {
            ItemStack itemStack = screenHandler.getSlot(i).getStack();
            if (!itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public boolean isInvFull() {
        int size = this.getPlayerMainSize();
        PlayerInventory playerInventory = this.getPlayerInventory();
        for (int i = 0; i < size; ++i) {
            ItemStack itemStack = playerInventory.getStack(i);
            if (!itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public void initCurPlayerSlot() {
        this.curPlayerSlot = this.getPlayerMainSize() - 1;
    }

    public int getCurPlayerSlot() {
        return this.curPlayerSlot;
    }

    public void initCurScreenSlot() {
        this.curPlayerSlot = this.getScreenMainSize() - 1;
    }

    public int getCurScreenSlot() {
        return this.curScreenSlot;
    }
}
