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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PosUtils {
    private static final Logger log = LoggerFactory.getLogger(PosUtils.class);

    public static void printPlayerDiff(BlockPos blockPos, LocalPlayer player) {
        double xDiff = (double)blockPos.getX() - player.getX();
        double yDiff = (double)blockPos.getY() - player.getY();
        double zDiff = (double)blockPos.getZ() - player.getZ();
        log.info("[{},{},{}] - [{},{},{}] = [{},{},{}]", new Object[]{blockPos.getX(), blockPos.getY(), blockPos.getZ(), String.format("%.2f", player.getX()), String.format("%.2f", player.getY()), String.format("%.2f", player.getZ()), String.format("%.2f", xDiff), String.format("%.2f", yDiff), String.format("%.2f", zDiff)});
    }

    public static List<BlockPos> getChestPositions(BlockPos pos) {
        BlockState state = MeteorClient.mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return Collections.singletonList(pos);
        }
        ArrayList<BlockPos> posList = new ArrayList<BlockPos>(2);
        posList.add(pos);
        ChestType chestType = (ChestType)state.getValue((Property)ChestBlock.TYPE);
        if (chestType != ChestType.SINGLE) {
            Direction facing = (Direction)state.getValue((Property)ChestBlock.FACING);
            Direction offsetDirection = chestType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
            BlockPos otherPos = pos.relative(offsetDirection);
            BlockState otherState = MeteorClient.mc.level.getBlockState(otherPos);
            if (otherState.getBlock() instanceof ChestBlock && otherState.getValue((Property)ChestBlock.FACING) == facing) {
                posList.add(otherPos);
            }
        }
        return posList;
    }
}
