/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 *  meteordevelopment.meteorclient.utils.player.Rotations
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket$Full
 *  org.joml.Vector3fc
 */
package com.xiaohe66.mc.meteor.lotus.util;


import com.xiaohe66.mc.meteor.lotus.util.HeRotation;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3fc;

public class HeRotationUtils {
    private static final HeRotation rotationBuffer = new HeRotation(0.0f, 0.0f);
    private static HeRotation keptRotation;

    public static void rotate(Vec3d vec3d) {
        double yaw = Rotations.getYaw((Vec3d)vec3d);
        double pitch = Rotations.getPitch((Vec3d)vec3d);
        Rotations.rotate((double)yaw, (double)pitch);
    }

    public static void rotate(BlockPos blockPos) {
        double yaw = Rotations.getYaw((BlockPos)blockPos);
        double pitch = Rotations.getPitch((BlockPos)blockPos);
        Rotations.rotate((double)yaw, (double)pitch);
    }

    public static void rotate(Vec3d vec3d, Runnable runnable) {
        double yaw = Rotations.getYaw((Vec3d)vec3d);
        double pitch = Rotations.getPitch((Vec3d)vec3d);
        Rotations.rotate((double)yaw, (double)pitch, (int)6666, (Runnable)runnable);
    }

    public static void rotate(BlockPos blockPos, Runnable runnable) {
        double yaw = Rotations.getYaw((BlockPos)blockPos);
        double pitch = Rotations.getPitch((BlockPos)blockPos);
        Rotations.rotate((double)yaw, (double)pitch, (int)6666, (Runnable)runnable);
    }

    public static void keepRotation(Vec3d pos) {
        HeRotation rotation = HeRotationUtils.getRotation(pos);
        HeRotationUtils.keepRotation(rotation);
    }

    public static void keepRotation(BlockPos pos, Direction side) {
        HeRotation rotation = HeRotationUtils.getRotation(pos, side);
        HeRotationUtils.keepRotation(rotation);
    }

    public static void keepRotation(HeRotation rotation) {
        HeRotationUtils.keepRotation(rotation.getYaw(), rotation.getPitch());
    }

    public static void rotateSilent(float yaw, float pitch) {
        MeteorClient.mc.getNetworkHandler().sendPacket((Packet)new PlayerMoveC2SPacket.Full(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), yaw, pitch, MeteorClient.mc.player.isOnGround(), MeteorClient.mc.player.horizontalCollision));
        Rotations.setCamRotation((double)yaw, (double)pitch);
    }

    public static HeRotation getKeptRotation() {
        return keptRotation;
    }

    public static void clearKeptRotation() {
        if (keptRotation != null) {
            keptRotation = null;
        }
    }

    public static HeRotation getRotation(Vec3d pos) {
        Vec3d eyePos = MeteorClient.mc.player.getEyePos();
        double dx = pos.x - eyePos.x;
        double dy = pos.y - eyePos.y;
        double dz = pos.z - eyePos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontalDistance)));
        return new HeRotation(yaw, pitch);
    }

    public static HeRotation getRotation(BlockPos pos, Direction side) {
        Vec3d center = new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
        Vec3d sideOffset = new Vec3d((Vector3fc)side.getUnitVector()).multiply(0.5);
        Vec3d target = center.add(sideOffset);
        return HeRotationUtils.getRotation(target);
    }

    public static void keepRotation(float yaw, float pitch) {
        ChatUtils.info("保持角度", (Object[])new Object[0]);
        keptRotation = rotationBuffer;
        keptRotation.setYaw(yaw);
        keptRotation.setPitch(pitch);
    }
}
