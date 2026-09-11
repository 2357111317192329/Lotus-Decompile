/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.block.BlockState
 */
package com.xiaohe66.mc.meteor.lotus.modules.printer;

import com.xiaohe66.mc.meteor.lotus.modules.printer.BlockPosWarp;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class PlaceBlockHelper extends BlockPosWarp {
    private List<BlockState> candidateStates;
    private BlockState targetState;
    private boolean requiresSneaking;
    private Direction clickDirection;

    public PlaceBlockHelper(BlockPos blockPos, double distance) {
        super(blockPos, distance);
    }

    public List<BlockState> getCandidateStates() {
        return this.candidateStates;
    }

    public void setCandidateStates(List<BlockState> candidateStates) {
        this.candidateStates = candidateStates;
    }

    public BlockState getTargetState() {
        return this.targetState;
    }

    public void setTargetState(BlockState targetState) {
        this.targetState = targetState;
    }

    public boolean requiresSneaking() {
        return this.requiresSneaking;
    }

    public void setRequiresSneaking(boolean requiresSneaking) {
        this.requiresSneaking = requiresSneaking;
    }

    public Direction getClickDirection() {
        return this.clickDirection;
    }

    public void setClickDirection(Direction clickDirection) {
        this.clickDirection = clickDirection;
    }
}
