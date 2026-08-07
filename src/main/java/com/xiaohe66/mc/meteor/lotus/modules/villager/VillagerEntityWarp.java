package com.xiaohe66.mc.meteor.lotus.modules.villager;

import java.util.UUID;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;

public class VillagerEntityWarp {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final VillagerType villagerType;
   private final UUID uuid;
   private final BlockPos operatePos;
   private final Vec3d operatePosCenter;
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
      this.operatePosCenter = operatePos.toCenterPos();
      this.facing = facing;
      this.workPos = workPos;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public VillagerEntity getVillager() {
      try {
         return (VillagerEntity)mc.world.getEntityLookup().get(this.uuid);
      } catch (Exception e) {
         ChatUtils.error("获取村民状态异常 : " + e.getMessage(), new Object[0]);
         e.printStackTrace();
         return null;
      }
   }

   public VillagerType getVillagerType() {
      return this.villagerType;
   }

   public BlockPos getOperatePos() {
      return this.operatePos;
   }

   public Vec3d getOperatePosCenter() {
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
