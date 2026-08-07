package com.xiaohe66.mc.meteor.lotus.util;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;

public class HeRotationUtils {
   public static void rotate(Vec3d vec3d) {
      double yaw = Rotations.getYaw(vec3d);
      double pitch = Rotations.getPitch(vec3d);
      Rotations.rotate(yaw, pitch);
   }

   public static void rotate(BlockPos blockPos) {
      double yaw = Rotations.getYaw(blockPos);
      double pitch = Rotations.getPitch(blockPos);
      Rotations.rotate(yaw, pitch);
   }

   public static void rotate(Vec3d vec3d, Runnable runnable) {
      double yaw = Rotations.getYaw(vec3d);
      double pitch = Rotations.getPitch(vec3d);
      Rotations.rotate(yaw, pitch, 6666, runnable);
   }

   public static void rotate(BlockPos blockPos, Runnable runnable) {
      double yaw = Rotations.getYaw(blockPos);
      double pitch = Rotations.getPitch(blockPos);
      Rotations.rotate(yaw, pitch, 6666, runnable);
   }

   public static void rotateSilent(float yaw, float pitch) {
      MeteorClient.mc
         .getNetworkHandler()
         .sendPacket(
            new Full(
               MeteorClient.mc.player.getX(),
               MeteorClient.mc.player.getY(),
               MeteorClient.mc.player.getZ(),
               yaw,
               pitch,
               MeteorClient.mc.player.isOnGround(),
               false
            )
         );
   }
}
