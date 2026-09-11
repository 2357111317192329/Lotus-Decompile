/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.utils.player.Rotations
 *  net.minecraft.util.Hand
 *  net.minecraft.util.ActionResult
 *  net.minecraft.util.ActionResult$Success
 *  net.minecraft.entity.Entity
 *  net.minecraft.world.BlockView
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.Block
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Direction$Axis
 *  net.minecraft.util.math.Direction$AxisDirection
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.hit.HitResult$Type
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.SlabBlock
 *  net.minecraft.block.StainedGlassBlock
 *  net.minecraft.block.StairsBlock
 *  net.minecraft.block.TrapdoorBlock
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.util.shape.VoxelShape
 *  net.minecraft.block.BlockState
 *  net.minecraft.state.property.Properties
 *  net.minecraft.state.property.Property
 *  net.minecraft.block.enums.SlabType
 *  net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
 *  net.minecraft.world.RaycastContext
 *  net.minecraft.world.RaycastContext$FluidHandling
 *  net.minecraft.world.RaycastContext$ShapeType
 *  net.minecraft.util.hit.BlockHitResult
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.util;


import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockPlaceUtils {
    private static final Logger log = LoggerFactory.getLogger(BlockPlaceUtils.class);
    private static final double RAYCAST_OFFSET = 1.0E-5;

    public static boolean place(BlockPos schematicPos, BlockState schematicBlockState, Direction direction, boolean rotate, boolean swingHand, boolean clientSide, double printingRange) {
        BlockPos adjacentPos = schematicPos.relative(direction);
        if (rotate) {
            BlockState adjacentState = MeteorClient.mc.level.getBlockState(adjacentPos);
            if (!BlockPlaceUtils.canPlaceAgainst(adjacentState, adjacentPos)) {
                return false;
            }
            Vec3 hitPos = BlockPlaceUtils.raycastToFace(schematicBlockState, adjacentPos, direction, printingRange);
            if (hitPos == null) {
                return false;
            }
            log.info("place hitPos, {} {} {}", new Object[]{hitPos.x(), hitPos.y(), hitPos.z()});
            double yaw = Rotations.getYaw((Vec3)hitPos);
            double pitch = Rotations.getPitch((Vec3)hitPos);
            log.info("place rotate, yaw : {}, pitch : {}", yaw, pitch);
            Rotations.rotate(yaw, pitch, 50, clientSide, () -> BlockPlaceUtils.place(new BlockHitResult(hitPos, direction.getOpposite(), adjacentPos, false), swingHand));
        } else {
            Vec3 hitPos = new Vec3((double)schematicPos.getX() + 0.5, (double)schematicPos.getY() + 0.5, (double)schematicPos.getZ() + 0.5);
            BlockPlaceUtils.place(new BlockHitResult(hitPos, direction.getOpposite(), adjacentPos, false), swingHand);
        }
        return true;
    }

    private static void place(BlockHitResult blockHitResult, boolean swing) {
        if (MeteorClient.mc.player == null || MeteorClient.mc.gameMode == null || MeteorClient.mc.getConnection() == null) {
            return;
        }
        boolean wasSneaking = MeteorClient.mc.player.isShiftKeyDown();
        MeteorClient.mc.player.setShiftKeyDown(false);
        InteractionResult result = MeteorClient.mc.gameMode.useItemOn(MeteorClient.mc.player, InteractionHand.MAIN_HAND, blockHitResult);
        if (result instanceof InteractionResult.Success) {
            if (swing) {
                MeteorClient.mc.player.swing(InteractionHand.MAIN_HAND);
            } else {
                MeteorClient.mc.getConnection().send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
        MeteorClient.mc.player.setShiftKeyDown(wasSneaking);
    }

    public static Direction getVisiblePlaceSide(BlockPos schematicPos, BlockState schematicState, double printingRange) {
        if (MeteorClient.mc.level == null || MeteorClient.mc.player == null) {
            return null;
        }
        Direction blockFacing = BlockPlaceUtils.getBlockFacingDirection(schematicState);
        if (blockFacing != null) {
            Direction lookDirection = blockFacing.getOpposite();
            if (BlockPlaceUtils.isFaceVisible(schematicPos, schematicState, lookDirection, printingRange)) {
                return lookDirection;
            }
            return null;
        }
        for (Direction direction : Direction.values()) {
            if (!BlockPlaceUtils.isFaceVisible(schematicPos, schematicState, direction, printingRange)) continue;
            return direction;
        }
        return null;
    }

    private static boolean isFaceVisible(BlockPos schematicPos, BlockState schematicState, Direction lookDirection, double printingRange) {
        BlockPos tryClickBlockPos = schematicPos.relative(lookDirection);
        BlockState tryClickBlockState = MeteorClient.mc.level.getBlockState(tryClickBlockPos);
        if (!BlockPlaceUtils.canPlaceAgainst(tryClickBlockState, tryClickBlockPos)) {
            return false;
        }
        Block block = schematicState.getBlock();
        if (block instanceof SlabBlock) {
            SlabBlock slabBlock = (SlabBlock)block;
            SlabType slabType = (SlabType)schematicState.getValue((Property)SlabBlock.TYPE);
            if (lookDirection == Direction.DOWN && slabType == SlabType.TOP) {
                return false;
            }
            if (lookDirection == Direction.UP && slabType == SlabType.BOTTOM) {
                return false;
            }
        }
        Vec3 target = BlockPlaceUtils.raycastToFace(schematicState, tryClickBlockPos, lookDirection, printingRange);
        log.info("raycastToFace : {}, {}, {} {} {} -> {} {} {} : {}", new Object[]{target != null, lookDirection.name(), schematicPos.getX(), schematicPos.getY(), schematicPos.getZ(), tryClickBlockPos.getX(), tryClickBlockPos.getY(), tryClickBlockPos.getZ(), schematicState.getBlock().getName()});
        return target != null;
    }

    private static Direction getBlockFacingDirection(BlockState state) {
        if (state.hasProperty((Property)BlockStateProperties.FACING)) {
            return (Direction)state.getValue((Property)BlockStateProperties.FACING);
        }
        if (state.hasProperty((Property)BlockStateProperties.HORIZONTAL_FACING)) {
            return (Direction)state.getValue((Property)BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty((Property)BlockStateProperties.AXIS)) {
            Direction.Axis axis = (Direction.Axis)state.getValue((Property)BlockStateProperties.AXIS);
            return Direction.fromAxisAndDirection((Direction.Axis)axis, (Direction.AxisDirection)Direction.AxisDirection.POSITIVE);
        }
        return null;
    }

    private static boolean isSlabFaceVisible(BlockPos schematicPos, Direction direction, BlockState slabState) {
        SlabType slabType = (SlabType)slabState.getValue((Property)SlabBlock.TYPE);
        if (slabType == SlabType.DOUBLE) {
            return false;
        }
        AABB collisionBox = slabState.getCollisionShape((BlockGetter)MeteorClient.mc.level, schematicPos).bounds();
        double minY = slabType == SlabType.BOTTOM ? 0.0 : 0.5;
        double maxY = slabType == SlabType.TOP ? 1.0 : 0.5;
        AABB adjustedBox = new AABB(collisionBox.minX, minY, collisionBox.minZ, collisionBox.maxX, maxY, collisionBox.maxZ);
        return BlockPlaceUtils.raycastToAdjustedBox(schematicPos, direction, adjustedBox);
    }

    private static boolean isTrapdoorFaceVisible(Direction dir, BlockState trapdoorState) {
        Direction facing = (Direction)trapdoorState.getValue((Property)TrapDoorBlock.FACING);
        Boolean open = (Boolean)trapdoorState.getValue((Property)TrapDoorBlock.OPEN);
        return !open && dir == facing;
    }

    private static Vec3 raycastToFace(BlockState schematicState, BlockPos adjacentPos, Direction direction, double printingRange) {
        Direction opposite = direction.getOpposite();
        Vec3 faceCenter = BlockPlaceUtils.getFaceCenter(adjacentPos, opposite);
        BlockState adjacentBlockState = MeteorClient.mc.level.getBlockState(adjacentPos);
        faceCenter = BlockPlaceUtils.fixSlabBlock(schematicState, adjacentBlockState, faceCenter);
        Vec3 eyePos = MeteorClient.mc.player.getEyePosition(1.0f);
        Vec3 rayVec = faceCenter.subtract(eyePos);
        double maxDistance = Math.min(eyePos.distanceTo(faceCenter) + 0.5, printingRange);
        Vec3 end = eyePos.add(rayVec.normalize().scale(maxDistance));
        log.info(" eyePos : {} {} {}, faceCenter : {} {} {}, end : {} {} {}", new Object[]{eyePos.x(), eyePos.y(), eyePos.z(), faceCenter.x(), faceCenter.y(), faceCenter.z(), end.x(), end.y(), end.z()});
        ClipContext context = new ClipContext(eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)MeteorClient.mc.player);
        BlockHitResult hit = MeteorClient.mc.level.clip(context);
        BlockPos hitBlockPos = hit.getBlockPos();
        log.info("hit pos, {},{},{},{}", new Object[]{hitBlockPos.getX(), hitBlockPos.getY(), hitBlockPos.getZ(), hit.getDirection()});
        boolean ok = hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(adjacentPos) && hit.getDirection() == opposite;
        return ok ? faceCenter : null;
    }

    private static Vec3 fixSlabBlock(BlockState schematicBlockState, BlockState adjacentBlockState, Vec3 faceCenter) {
        Block schematicBlock = schematicBlockState.getBlock();
        if (!(schematicBlock instanceof SlabBlock)) {
            return faceCenter;
        }
        Block adjacentBlock = adjacentBlockState.getBlock();
        if (!(adjacentBlock instanceof SlabBlock)) {
            SlabType schematicBlockSlabType = (SlabType)schematicBlockState.getValue((Property)SlabBlock.TYPE);
            if (schematicBlockSlabType == SlabType.TOP) {
                return new Vec3(faceCenter.x(), faceCenter.y() + 0.25, faceCenter.z());
            }
            if (schematicBlockSlabType == SlabType.BOTTOM) {
                return new Vec3(faceCenter.x(), faceCenter.y() - 0.25, faceCenter.z());
            }
        }
        return faceCenter;
    }

    private static Vec3 getFaceCenter(BlockPos pos, Direction direction) {
        AABB box = MeteorClient.mc.level.getBlockState(pos).getShape((BlockGetter)MeteorClient.mc.level, pos).bounds();
        double x = pos.getX() + box.minX + (box.maxX - box.minX) * 0.5;
        double y = pos.getY() + box.minY + (box.maxY - box.minY) * 0.5;
        double z = pos.getZ() + box.minZ + (box.maxZ - box.minZ) * 0.5;
        return new Vec3(x + (double)direction.getStepX() * (box.maxX - box.minX) * 0.5, y + (double)direction.getStepY() * (box.maxY - box.minY) * 0.5, z + (double)direction.getStepZ() * (box.maxZ - box.minZ) * 0.5);
    }

    private static Direction getDirectionFromPlayerView(BlockPos targetPos) {
        Vec3 eyePos = new Vec3(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getEyeY(), MeteorClient.mc.player.getZ());
        Vec3 targetCenter = Vec3.atCenterOf((Vec3i)targetPos);
        Vec3 lookVec = targetCenter.subtract(eyePos).normalize();
        return Direction.getApproximateNearest((double)lookVec.x, (double)lookVec.y, (double)lookVec.z);
    }

    private static boolean canPlaceAgainst(BlockState tryClickBlockState, BlockPos tryClickPos) {
        if (tryClickBlockState.isAir() || tryClickBlockState.liquid()) {
            return false;
        }
        VoxelShape shape = tryClickBlockState.getCollisionShape((BlockGetter)MeteorClient.mc.level, tryClickPos);
        if (shape.isEmpty()) {
            return false;
        }
        Block block = tryClickBlockState.getBlock();
        if (block == Blocks.GLASS || block == Blocks.ICE) {
            return false;
        }
        return Block.isShapeFullBlock((VoxelShape)shape) || tryClickBlockState.getBlock() == Blocks.GLASS || tryClickBlockState.getBlock() instanceof StainedGlassBlock || tryClickBlockState.getBlock() instanceof StairBlock || tryClickBlockState.getBlock() instanceof SlabBlock;
    }

    private static boolean raycastToAdjustedBox(BlockPos schematicPos, Direction direction, AABB adjustedBox) {
        Direction opposite = direction.getOpposite();
        Vec3 eyePos = MeteorClient.mc.player.getEyePosition(1.0f);
        int samples = 3;
        double xStep = (adjustedBox.maxX - adjustedBox.minX) / 4.0;
        double yStep = (adjustedBox.maxY - adjustedBox.minY) / 4.0;
        double zStep = (adjustedBox.maxZ - adjustedBox.minZ) / 4.0;
        for (int i = 1; i <= 3; ++i) {
            for (int j = 1; j <= 3; ++j) {
                for (int k = 1; k <= 3; ++k) {
                    double x = adjustedBox.minX + xStep * i;
                    double y = adjustedBox.minY + yStep * j;
                    double z = adjustedBox.minZ + zStep * k;
                    Vec3 point = new Vec3((double)schematicPos.getX() + x, (double)schematicPos.getY() + y, (double)schematicPos.getZ() + z);
                    BlockHitResult hit = MeteorClient.mc.level.clip(new ClipContext(eyePos, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)MeteorClient.mc.player));
                    if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(schematicPos) || hit.getDirection() != opposite) continue;
                    log.info("raycastToAdjustedBox return false");
                    return true;
                }
            }
        }
        log.info("raycastToAdjustedBox return false");
        return false;
    }

    private record PlacementContext(BlockPos pos, Direction side, Vec3 hitPos) {
    }
}
