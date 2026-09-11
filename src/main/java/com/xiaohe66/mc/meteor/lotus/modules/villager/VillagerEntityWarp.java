/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 *  net.minecraft.entity.passive.VillagerEntity
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.client.MinecraftClient
 */
package com.xiaohe66.mc.meteor.lotus.modules.villager;

import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerType;
import java.util.UUID;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

public class VillagerEntityWarp {
    private static final Minecraft mc = Minecraft.getInstance();
    private final VillagerType villagerType;
    private final UUID uuid;
    private final BlockPos operatePos;
    private final Vec3 operatePosCenter;
    private final Direction facing;
    private final BlockPos workPos;
    private long lastTradeTime;
    private long lastFailTime;

    public VillagerEntityWarp(VillagerType villagerType, UUID uuid, BlockPos operatePos) {
        this(villagerType, uuid, operatePos, null, null);
    }

    public VillagerEntityWarp(VillagerType villagerType, UUID uuid, BlockPos operatePos, Direction facing, BlockPos workPos) {
        this.villagerType = villagerType;
        this.uuid = uuid;
        this.operatePos = operatePos;
        this.operatePosCenter = operatePos.getCenter();
        this.facing = facing;
        this.workPos = workPos;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Villager getVillager() {
        try {
            return (Villager)VillagerEntityWarp.mc.level.getEntities().get(this.uuid);
        }
        catch (Exception exception) {
            ChatUtils.error((String)("获取村民状态异常 : " + exception.getMessage()), (Object[])new Object[0]);
            exception.printStackTrace();
            return null;
        }
    }

    public VillagerType getVillagerType() {
        return this.villagerType;
    }

    public BlockPos getOperatePos() {
        return this.operatePos;
    }

    public Vec3 getOperatePosCenter() {
        return this.operatePosCenter;
    }

    public long getLastTradeTime() {
        return this.lastTradeTime;
    }

    public void setLastTradeTime(long lastTradeTime) {
        this.lastTradeTime = lastTradeTime;
    }

    public long getLastFailTime() {
        return this.lastFailTime;
    }

    public void setLastFailTime(long lastFailTime) {
        this.lastFailTime = lastFailTime;
    }

    public long getTimeOfDay() {
        return this.lastTradeTime % 24000L;
    }

    public long getDay() {
        return this.lastTradeTime / 24000L;
    }

    public Direction getFacing() {
        return this.facing;
    }

    public BlockPos getWorkPos() {
        return this.workPos;
    }
}

