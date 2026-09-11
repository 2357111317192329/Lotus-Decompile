/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 *  net.minecraft.util.Hand
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.client.MinecraftClient
 */
package com.xiaohe66.mc.meteor.lotus.modules.spiral;

import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.modules.MosquitoCoilScan;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.MinecraftClient;

public class Pitch40Controller {
    private static final float INIT_PITCH = 37.72f;
    private static final float MIN_PITCH = -54.77f;
    private static final float ASCEND_STEP = 0.9f;
    private static final float DESCEND_STEP = 5.45f;
    private static final float ASCEND_JITTER = 0.5f;
    private static final float DESCEND_JITTER = 1.0f;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final MosquitoCoilScan scan;
    private float currentPitch = 37.72f;
    private volatile boolean ascending = true;
    private volatile double heightLimit;
    private long lastFireworkTime;
    private boolean needClimb;

    public Pitch40Controller(MosquitoCoilScan scan) {
        this.scan = scan;
    }

    public void start() {
        this.currentPitch = 37.72f;
        this.heightLimit = this.mc.player.getY();
        this.needClimb = true;
        this.ascending = false;
    }

    public void tick() {
        double playerY = this.mc.player.getY();
        if (this.ascending && playerY < this.getDiveThreshold()) {
            this.ascending = false;
        }
        if (playerY < (double)this.scan.minHeight.get()) {
            this.needClimb = true;
        }
        if (this.ascending) {
            if (this.currentPitch < 37.72f) {
                this.currentPitch += this.withJitter(0.9f, 0.5f);
            }
        } else {
            if (this.currentPitch > -54.77f) {
                this.currentPitch -= this.withJitter(5.45f, 1.0f);
                if (this.currentPitch < -54.77f) {
                    this.currentPitch = -54.77f;
                }
            }
            if (this.currentPitch <= -54.77f) {
                if (this.needClimb) {
                    boolean falling = this.mc.player.getVelocity().y < 1.0;
                    if (falling) {
                        if (playerY > (double)(this.scan.minHeight.get() + this.scan.heightRange.get())) {
                            this.needClimb = false;
                            this.heightLimit = playerY;
                            this.ascending = true;
                            ChatUtils.warning("调整高度上限 : %.1f", (Object[])new Object[]{this.heightLimit});
                        } else {
                            this.launchFirework();
                        }
                    }
                } else {
                    this.ascending = true;
                }
            }
        }
        this.mc.player.setPitch(this.currentPitch);
    }

    public void launchFirework() {
        long now = System.currentTimeMillis();
        if (now - this.lastFireworkTime <= 2000L) {
            return;
        }
        boolean hasFirework = this.hasFirework();
        if (!hasFirework) {
            return;
        }
        ChatUtils.warning("使用烟花拉升高度 : %.1f", (Object[])new Object[]{this.mc.player.getY()});
        this.lastFireworkTime = now;
        this.mc.interactionManager.interactItem((PlayerEntity)this.mc.player, Hand.MAIN_HAND);
    }

    private boolean hasFirework() {
        ItemStack mainHandStack = this.mc.player.getMainHandStack();
        if (mainHandStack.getItem() == Items.FIREWORK_ROCKET) {
            return true;
        }
        int slot = this.findFireworkSlot();
        if (slot == -1) {
            ChatUtils.warning("缺少烟花火箭", (Object[])new Object[0]);
        } else if (HeInvUtils.isHotbar(slot)) {
            HeInvUtils.swapToSlot(slot);
        } else {
            HeInvUtils.swapMainHand(slot);
        }
        return false;
    }

    private int findFireworkSlot() {
        ItemStack stack;
        int slot;
        for (slot = 0; slot < 9; ++slot) {
            stack = this.mc.player.getInventory().getStack(slot);
            if (stack.getItem() != Items.FIREWORK_ROCKET) continue;
            return slot;
        }
        for (slot = 9; slot < 36; ++slot) {
            stack = this.mc.player.getInventory().getStack(slot);
            if (stack.getItem() != Items.FIREWORK_ROCKET) continue;
            return slot;
        }
        return -1;
    }

    public double getDiveThreshold() {
        return this.heightLimit - (double)this.scan.heightRange.get();
    }

    private float withJitter(float base, float jitter) {
        return (float)((double)base + (double)jitter * (Math.random() - 0.5));
    }

    public double getHeightLimit() {
        return this.heightLimit;
    }
}
