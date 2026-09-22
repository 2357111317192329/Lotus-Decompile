/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  net.minecraft.block.ChestBlock
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.block.BlockState
 *  net.minecraft.block.enums.ChestType
 *  net.minecraft.state.property.Property
 *  net.minecraft.client.network.ClientPlayerEntity
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PosUtils {
    private static final Logger log = LoggerFactory.getLogger(PosUtils.class);

    public static void printPlayerDiff(BlockPos blockPos, ClientPlayerEntity player) {
        double xDiff = (double)blockPos.getX() - player.getX();
        double yDiff = (double)blockPos.getY() - player.getY();
        double zDiff = (double)blockPos.getZ() - player.getZ();
        log.info("[{},{},{}] - [{},{},{}] = [{},{},{}]", new Object[]{blockPos.getX(), blockPos.getY(), blockPos.getZ(), String.format("%.2f", player.getX()), String.format("%.2f", player.getY()), String.format("%.2f", player.getZ()), String.format("%.2f", xDiff), String.format("%.2f", yDiff), String.format("%.2f", zDiff)});
    }

    public static List<BlockPos> getChestPositions(BlockPos pos) {
        BlockState state = MeteorClient.mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return Collections.singletonList(pos);
        }
        ArrayList<BlockPos> posList = new ArrayList<BlockPos>(2);
        posList.add(pos);
        ChestType chestType = (ChestType)state.get((Property)ChestBlock.CHEST_TYPE);
        if (chestType != ChestType.SINGLE) {
            Direction facing = (Direction)state.get((Property)ChestBlock.FACING);
            Direction offsetDirection = chestType == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
            BlockPos otherPos = pos.offset(offsetDirection);
            BlockState otherState = MeteorClient.mc.world.getBlockState(otherPos);
            if (otherState.getBlock() instanceof ChestBlock && otherState.get((Property)ChestBlock.FACING) == facing) {
                posList.add(otherPos);
            }
        }
        return posList;
    }
}
