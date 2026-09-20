/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  net.minecraft.inventory.Inventory
 *  net.minecraft.util.Hand
 *  net.minecraft.util.ActionResult
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.PlayerInventory
 *  net.minecraft.item.BlockItem
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.BlockView
 *  net.minecraft.block.AnvilBlock
 *  net.minecraft.block.BedBlock
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.Block
 *  net.minecraft.block.BrewingStandBlock
 *  net.minecraft.block.ChestBlock
 *  net.minecraft.block.CraftingTableBlock
 *  net.minecraft.block.DispenserBlock
 *  net.minecraft.block.EnchantingTableBlock
 *  net.minecraft.block.EnderChestBlock
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Direction$Axis
 *  net.minecraft.util.math.Direction$AxisDirection
 *  net.minecraft.block.HopperBlock
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.hit.HitResult$Type
 *  net.minecraft.block.FluidBlock
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.ShulkerBoxBlock
 *  net.minecraft.block.SlabBlock
 *  net.minecraft.block.TrapdoorBlock
 *  net.minecraft.block.entity.BlockEntity
 *  net.minecraft.util.shape.VoxelShape
 *  net.minecraft.block.BlockState
 *  net.minecraft.state.property.Properties
 *  net.minecraft.block.enums.BlockHalf
 *  net.minecraft.state.property.Property
 *  net.minecraft.block.enums.SlabType
 *  net.minecraft.registry.tag.BlockTags
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.block.BarrelBlock
 *  net.minecraft.block.FurnaceBlock
 *  net.minecraft.world.RaycastContext
 *  net.minecraft.world.RaycastContext$FluidHandling
 *  net.minecraft.world.RaycastContext$ShapeType
 *  net.minecraft.util.hit.BlockHitResult
 *  org.joml.Vector3fc
 */
package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.util.HePlayerUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;

import com.xiaohe66.mc.meteor.lotus.util.HeRotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3fc;

public class HeBlockUtils {
    public static boolean isClickable(BlockPos pos) {
        return HeBlockUtils.isClickable(pos, false);
    }

    public static boolean isClickable(BlockPos pos, boolean allowClick) {
        BlockState state = MeteorClient.mc.level.getBlockState(pos);
        if (state.getShape((BlockGetter)MeteorClient.mc.level, pos).isEmpty()) {
            return false;
        }
        if (allowClick) {
            return true;
        }
        Block block = state.getBlock();
        if (HeBlockUtils.isInteractableBlock(block)) {
            return MeteorClient.mc.player.isShiftKeyDown();
        }
        return true;
    }

    public static boolean isReplaceable(BlockPos pos) {
        return MeteorClient.mc.level.getBlockState(pos).canBeReplaced();
    }

    public static boolean hasLineOfSight(Vec3 target, Direction side) {
        if (side == null) {
            return false;
        }
        BlockHitResult result = MeteorClient.mc.level.clip(new ClipContext(MeteorClient.mc.player.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)MeteorClient.mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    public static void open(BlockPos pos) {
        Direction clickSide = HeBlockUtils.findClickSide(pos);
        if (clickSide != null) {
            HeBlockUtils.open(pos, clickSide);
        } else {
            ChatUtils.warning((String)"未找到合适的点击面", (Object[])new Object[0]);
        }
    }

    private static Direction findClickSide(BlockPos pos) {
        Vec3 eyePos = MeteorClient.mc.player.getEyePosition();
        Set<Direction> visibleSides = HeBlockUtils.getVisibleDirections(eyePos, pos.getCenter());
        Direction airSide = null;
        double airDistance = Double.MAX_VALUE;
        Direction blockSide = null;
        double blockDistance = Double.MAX_VALUE;
        for (Direction direction : visibleSides) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = MeteorClient.mc.level.getBlockState(neighborPos);
            Vec3 faceCenter = pos.getCenter().add((double)direction.getStepX() * 0.5, (double)direction.getStepY() * 0.5, (double)direction.getStepZ() * 0.5);
            double distance = eyePos.distanceToSqr(faceCenter);
            if (neighborState.isAir()) {
                if (!(distance < airDistance)) continue;
                airDistance = distance;
                airSide = direction;
                continue;
            }
            if (!HeBlockUtils.isNonFullCube(neighborState, neighborPos) || !(distance < blockDistance)) continue;
            blockDistance = distance;
            blockSide = direction;
        }
        if (airSide != null) {
            return airSide;
        }
        return blockSide;
    }

    public static void open(BlockPos pos, Direction side) {
        HeRotationUtils.keepRotation(pos, side);
        Vec3i vector = side.getUnitVec3i();
        double offset = 0.45;
        Vec3 hitPos = new Vec3((double)pos.getX() + 0.5 + (double)vector.getX() * offset, (double)pos.getY() + 0.5 + (double)vector.getY() * offset, (double)pos.getZ() + 0.5 + (double)vector.getZ() * offset);
        HeBlockUtils.open(pos, side, hitPos);
    }

    public static void open(BlockPos pos, Direction side, Vec3 hitPos) {
        BlockHitResult result = new BlockHitResult(hitPos, side, pos, false);
        MeteorClient.mc.gameMode.useItemOn(MeteorClient.mc.player, InteractionHand.MAIN_HAND, result);
    }

    public static boolean clickAdjacentBlock(BlockPos pos) {
        Direction clickSide = HeBlockUtils.getClickSide(pos);
        if (clickSide == null) {
            return false;
        }
        BlockPos neighborPos = pos.relative(clickSide.getOpposite());
        return HeBlockUtils.clickBlock(neighborPos, clickSide);
    }

    public static boolean clickAdjacentBlock(BlockPos pos, Direction side) {
        BlockPos neighborPos = pos.relative(side);
        return HeBlockUtils.clickBlock(neighborPos, side.getOpposite());
    }

    public static boolean clickBlock(BlockPos pos, Direction side) {
        Vec3 hitPos = HeBlockUtils.getClickPoint(pos, side);
        return HeBlockUtils.clickBlock(pos, side, hitPos);
    }

    private static boolean clickBlockWithOffset(BlockPos pos, Direction side, Vec3 offset) {
        return HeBlockUtils.clickBlock(pos, side, pos.getCenter().add(offset));
    }

    private static boolean clickBlock(BlockPos pos, Direction side, Vec3 hitPos) {
        float yaw = MeteorClient.mc.player.getYRot();
        float pitch = MeteorClient.mc.player.getXRot();
        HeRotation rotation = HeRotationUtils.getRotation(hitPos);
        HeRotationUtils.keepRotation(rotation);
        BlockHitResult result = new BlockHitResult(hitPos, side, pos, false);
        boolean clicked = HeBlockUtils.interact(result);
        HeRotationUtils.keepRotation(yaw, pitch);
        return clicked;
    }

    private static boolean interact(BlockHitResult result) {
        InteractionResult actionResult;
        BlockState state = MeteorClient.mc.level.getBlockState(result.getBlockPos());
        boolean shouldSneak = HeBlockUtils.isInteractableBlock(state.getBlock()) && !MeteorClient.mc.player.isShiftKeyDown();
        if (shouldSneak) {
            HePlayerUtils.startSneaking();
        }
        if ((actionResult = MeteorClient.mc.gameMode.useItemOn(MeteorClient.mc.player, InteractionHand.MAIN_HAND, result)).consumesAction()) {
            MeteorClient.mc.player.swing(InteractionHand.MAIN_HAND);
        }
        if (shouldSneak) {
            HePlayerUtils.stopSneaking();
        }
        return actionResult.consumesAction();
    }

    @Deprecated
    public static boolean place(BlockPos blockPos, int slot, boolean checkEntities, Direction side) {
        BlockItem blockItem;
        if (slot < 0 || slot > 8) {
            return false;
        }
        Block toPlace = Blocks.OBSIDIAN;
        Inventory inventory = MeteorClient.mc.player.getInventory();
        ItemStack itemStack = inventory.getItem(slot);
        Item item = itemStack.getItem();
        if (item instanceof BlockItem) {
            blockItem = (BlockItem)item;
            toPlace = blockItem.getBlock();
        }
        if (!BlockUtils.canPlaceBlock((BlockPos)blockPos, (boolean)checkEntities, (Block)toPlace)) {
            return false;
        }
        BlockPos neighbour = blockPos.relative(side);
        Vec3i vector = side.getOpposite().getUnitVec3i();
        double offset = 0.45;
        Vec3 hitPos = new Vec3((double)neighbour.getX() + 0.5 + (double)vector.getX() * offset, (double)neighbour.getY() + 0.5 + (double)vector.getY() * offset, (double)neighbour.getZ() + 0.5 + (double)vector.getZ() * offset);
        BlockHitResult bhr = new BlockHitResult(hitPos, side.getOpposite(), (BlockPos)neighbour, false);
        if (inventory.getSelectedSlot() != slot) {
            InvUtils.swap((int)slot, (boolean)false);
        }
        HeRotationUtils.rotate(hitPos, () -> BlockUtils.interact((BlockHitResult)bhr, (InteractionHand)InteractionHand.MAIN_HAND, (boolean)true));
        return true;
    }

    public static boolean placeBlock(BlockPos pos, Direction faceDirection) {
        Direction clickSide = HeBlockUtils.getClickSide(pos);
        if (clickSide == null) {
            return false;
        }
        BlockPos neighborPos = pos.relative(clickSide.getOpposite());
        return HeBlockUtils.placeBlock(neighborPos, neighborPos, clickSide, faceDirection);
    }

    public static boolean placeBlock(BlockPos pos, Direction faceDirection, Direction blockFacing) {
        BlockPos neighborPos = pos.relative(blockFacing);
        Direction clickSide = blockFacing.getOpposite();
        return HeBlockUtils.placeBlock(neighborPos, neighborPos, clickSide, faceDirection);
    }

    private static boolean placeBlock(BlockPos targetPos, BlockPos clickPos, Direction clickSide, Direction faceDirection) {
        return HeBlockUtils.placeBlock(targetPos, clickPos, clickSide, faceDirection, new Vec3((Vector3fc)clickSide.step()).scale(0.5));
    }

    private static boolean placeBlock(BlockPos targetPos, BlockPos clickPos, Direction clickSide, Direction faceDirection, Vec3 offset) {
        Vec3 clickPoint = clickPos.getCenter().add(offset);
        Vec3 visiblePoint = HeBlockUtils.findVisiblePoint(clickPoint, clickSide, true);
        if (visiblePoint == null) {
            visiblePoint = clickPoint;
        }
        HeRotation rotation = HeRotationUtils.getRotation(visiblePoint);
        HeRotationUtils.keepRotation(rotation);
        float yaw = HeBlockUtils.getDirectionYaw(faceDirection);
        float pitch = faceDirection == Direction.UP ? -90.0f : (faceDirection == Direction.DOWN ? 90.0f : 5.0f);
        HeRotationUtils.keepRotation(yaw, pitch);
        BlockHitResult result = new BlockHitResult(visiblePoint, clickSide, targetPos, false);
        boolean clicked = HeBlockUtils.interact(result);
        return clicked;
    }

    public static void placeSlab(BlockPos pos, BlockState state) {
        if (!state.getProperties().contains(BlockStateProperties.SLAB_TYPE)) {
            HeBlockUtils.clickAdjacentBlock(pos);
            return;
        }
        SlabType slabType = (SlabType)state.getValue((Property)BlockStateProperties.SLAB_TYPE);
        Direction clickSide = HeBlockUtils.getSlabPlaceDirection(pos, slabType == SlabType.TOP);
        if (clickSide == null) {
            return;
        }
        BlockPos neighborPos = pos.relative(clickSide.getOpposite());
        if (slabType == SlabType.TOP || slabType == SlabType.BOTTOM) {
            HeBlockUtils.clickBlock(neighborPos, clickSide);
        } else if (slabType == SlabType.DOUBLE) {
            HeBlockUtils.clickBlockWithOffset(neighborPos, clickSide, new Vec3(0.0, 0.25, 0.0));
        } else {
            HeBlockUtils.clickAdjacentBlock(pos);
        }
    }

    public static void placeStairs(BlockPos pos, BlockState state) {
        Direction facing = (Direction)state.getValue((Property)BlockStateProperties.HORIZONTAL_FACING);
        Half blockHalf = (Half)state.getValue((Property)BlockStateProperties.HALF);
        if (blockHalf == Half.TOP) {
            Direction clickSide = HeBlockUtils.getSlabPlaceDirection(pos, true);
            if (clickSide == null) {
                return;
            }
            BlockPos neighborPos = pos.relative(clickSide.getOpposite());
            HeBlockUtils.placeBlock(neighborPos, pos, clickSide, facing, new Vec3(0.0, 0.25, 0.0));
        } else {
            Direction clickSide = HeBlockUtils.getSlabPlaceDirection(pos, false);
            if (clickSide == null) {
                return;
            }
            BlockPos neighborPos = pos.relative(clickSide.getOpposite());
            HeBlockUtils.placeBlock(neighborPos, pos, clickSide, facing, new Vec3(0.0, -0.25, 0.0));
        }
    }

    public static boolean isTorch(Block block) {
        return block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH;
    }

    public static boolean isWallTorch(Block block) {
        return block == Blocks.WALL_TORCH || block == Blocks.REDSTONE_WALL_TORCH || block == Blocks.SOUL_WALL_TORCH;
    }

    public static void placeTorch(BlockPos pos, BlockState state) {
        if (HeBlockUtils.isWallTorch(state.getBlock())) {
            Direction facing = (Direction)state.getValue((Property)BlockStateProperties.HORIZONTAL_FACING);
            BlockPos wallPos = pos.relative(facing.getOpposite());
            BlockState wallState = MeteorClient.mc.level.getBlockState(wallPos);
            if (wallState.isAir() || !wallState.getFluidState().isEmpty()) {
                return;
            }
            if (HeBlockUtils.hasLineOfSight(HeBlockUtils.getClickPoint(wallPos, facing), facing)) {
                HeBlockUtils.clickBlock(wallPos, facing);
            }
        } else {
            BlockPos downPos = pos.below();
            BlockState downState = MeteorClient.mc.level.getBlockState(downPos);
            boolean supported = !downState.isAir() && Block.isFaceFull(downState.getCollisionShape(MeteorClient.mc.level, downPos), Direction.UP);
            if (supported && HeBlockUtils.hasLineOfSight(HeBlockUtils.getClickPoint(downPos, Direction.UP), Direction.UP)) {
                HeBlockUtils.clickBlock(downPos, Direction.UP);
            }
        }
    }

    public static void placeHopper(BlockPos pos, BlockState state) {
        Direction facing = (Direction)state.getValue((Property)BlockStateProperties.FACING_HOPPER);
        BlockPos targetPos = pos.relative(facing);
        BlockState targetState = MeteorClient.mc.level.getBlockState(targetPos);
        if (!targetState.isAir() && targetState.getFluidState().isEmpty()) {
            if (HeBlockUtils.hasLineOfSight(HeBlockUtils.getClickPoint(targetPos, facing.getOpposite()), facing.getOpposite())) {
                HeBlockUtils.clickBlock(targetPos, facing.getOpposite());
            }
        }
    }

    public static Direction getSlabPlaceDirection(BlockPos pos, boolean isTopHalf) {
        BlockState state;
        Set<Direction> visibleSides = HeBlockUtils.getVisibleDirections(MeteorClient.mc.player.getEyePosition(), pos.getCenter());
        if (visibleSides.remove(Direction.DOWN) && isTopHalf && HeBlockUtils.isSolid(state = MeteorClient.mc.level.getBlockState(pos.above()))) {
            return Direction.DOWN;
        }
        if (visibleSides.remove(Direction.UP) && !isTopHalf && HeBlockUtils.isSolid(state = MeteorClient.mc.level.getBlockState(pos.below()))) {
            return Direction.UP;
        }
        for (Direction direction : visibleSides) {
            BlockState neighborState = MeteorClient.mc.level.getBlockState(pos.relative(direction.getOpposite()));
            if (!HeBlockUtils.isSolid(neighborState)) continue;
            if (neighborState.getBlock() instanceof SlabBlock) {
                SlabType slabType = (SlabType)neighborState.getValue((Property)BlockStateProperties.SLAB_TYPE);
                if (slabType != SlabType.DOUBLE && (slabType != SlabType.BOTTOM || isTopHalf) && (slabType != SlabType.TOP || !isTopHalf)) continue;
                return direction;
            }
            return direction;
        }
        return null;
    }

    public static boolean isNotSolid(BlockState state) {
        return !HeBlockUtils.isSolid(state);
    }

    public static boolean isSolid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    public static boolean isAboveClear(BlockPos pos) {
        BlockPos upPos = pos.above();
        return MeteorClient.mc.level.getBlockState(upPos).getCollisionShape((BlockGetter)MeteorClient.mc.level, upPos).isEmpty();
    }

    public static boolean isReplaceable(BlockState state, BlockPos pos) {
        return state.getCollisionShape((BlockGetter)MeteorClient.mc.level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    public static boolean isRail(BlockState state) {
        return state.is(BlockTags.RAILS);
    }

    private static boolean isNonFullCube(BlockState state, BlockPos pos) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        VoxelShape shape = state.getShape((BlockGetter)MeteorClient.mc.level, pos);
        if (shape.isEmpty()) {
            return false;
        }
        return !Block.isShapeFullBlock((VoxelShape)shape);
    }

    public static List<BlockPos> listPosInSphere(int range, BlockPos pos) {
        Vec3 center = pos.getCenter();
        ArrayList<BlockPos> posList = new ArrayList<BlockPos>();
        for (int x = pos.getX() - range; x < pos.getX() + range; ++x) {
            for (int z = pos.getZ() - range; z < pos.getZ() + range; ++z) {
                for (int y = pos.getY() - range; y < pos.getY() + range; ++y) {
                    BlockPos curPos = new BlockPos(x, y, z);
                    if (curPos.getCenter().distanceTo(center) > (double)range || posList.contains(curPos)) continue;
                    posList.add(curPos);
                }
            }
        }
        return posList;
    }

    public static List<BlockPos> listPosInSphere(int xRange, int yRange, BlockPos pos) {
        ArrayList<BlockPos> posList = new ArrayList<BlockPos>();
        for (int x = pos.getX() - xRange; x < pos.getX() + xRange; ++x) {
            for (int z = pos.getZ() - xRange; z < pos.getZ() + xRange; ++z) {
                for (int y = pos.getY() - yRange; y < pos.getY() + yRange; ++y) {
                    BlockPos curPos = new BlockPos(x, y, z);
                    posList.add(curPos);
                }
            }
        }
        return posList;
    }

    public static Direction getBlockFacingDirection(BlockState state) {
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

    public static Vec3 getFaceCenter(BlockPos pos, Direction direction) {
        AABB box = MeteorClient.mc.level.getBlockState(pos).getShape((BlockGetter)MeteorClient.mc.level, pos).bounds();
        double x = (double)pos.getX() + box.minX + (box.maxX - box.minX) * 0.5;
        double y = (double)pos.getY() + box.minY + (box.maxY - box.minY) * 0.5;
        double z = (double)pos.getZ() + box.minZ + (box.maxZ - box.minZ) * 0.5;
        return new Vec3(x + (double)direction.getStepX() * (box.maxX - box.minX) * 0.5, y + (double)direction.getStepY() * (box.maxY - box.minY) * 0.5, z + (double)direction.getStepZ() * (box.maxZ - box.minZ) * 0.5);
    }

    private static Vec3 getClickPoint(BlockPos pos, Direction side) {
        VoxelShape shape = MeteorClient.mc.level.getBlockState(pos).getShape((BlockGetter)MeteorClient.mc.level, pos);
        if (shape.isEmpty()) {
            return new Vec3((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
        }
        AABB box = shape.bounds();
        double halfWidth = (box.maxX - box.minX) * 0.5;
        double halfHeight = (box.maxY - box.minY) * 0.5;
        double halfDepth = (box.maxZ - box.minZ) * 0.5;
        double x = (double)pos.getX() + box.minX + halfWidth;
        double y = (double)pos.getY() + box.minY + halfHeight;
        double z = (double)pos.getZ() + box.minZ + halfDepth;
        return new Vec3(x + (double)side.getStepX() * halfWidth, y + (double)side.getStepY() * halfHeight, z + (double)side.getStepZ() * halfDepth);
    }

    private static boolean hasLineOfSight(Vec3 from, Vec3 to) {
        BlockHitResult result = MeteorClient.mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)MeteorClient.mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    private static Vec3 findVisiblePoint(Vec3 target, Direction side, boolean includeYOffset) {
        Vec3 eyePos = MeteorClient.mc.player.getEyePosition();
        if (HeBlockUtils.hasLineOfSight(eyePos, target)) {
            return target;
        }
        double xOffset = side.getAxis() == Direction.Axis.X ? 0.0 : 0.25;
        double yOffset = side.getAxis() == Direction.Axis.Y ? 0.0 : 0.25;
        double zOffset = side.getAxis() == Direction.Axis.Z ? 0.0 : 0.25;
        if (!includeYOffset) {
            yOffset = 0.0;
        }
        Vec3 visiblePoint = null;
        double nearestDistance = Double.MAX_VALUE;
        for (double offsetX : new double[]{-xOffset, 0.0, xOffset}) {
            for (double offsetY : new double[]{-yOffset, 0.0, yOffset}) {
                for (double offsetZ : new double[]{-zOffset, 0.0, zOffset}) {
                    double distance;
                    Vec3 candidate;
                    if (offsetX == 0.0 && offsetY == 0.0 && offsetZ == 0.0 || !HeBlockUtils.hasLineOfSight(eyePos, candidate = target.add(offsetX, offsetY, offsetZ)) || !((distance = eyePos.distanceToSqr(candidate)) < nearestDistance)) continue;
                    nearestDistance = distance;
                    visiblePoint = candidate;
                }
            }
        }
        return visiblePoint;
    }

    public static BlockPos getCanStandPos(BlockPos pos, int range) {
        for (int x = pos.getX() - range; x <= pos.getX() + range; ++x) {
            for (int z = pos.getZ() - range; z <= pos.getZ() + range; ++z) {
                BlockPos curPos = new BlockPos(x, pos.getY(), z);
                if (!HeBlockUtils.canStand(curPos)) continue;
                return curPos;
            }
        }
        return null;
    }

    public static boolean canStand(BlockPos blockPos) {
        return MeteorClient.mc.level.getBlockState(blockPos).isAir() && MeteorClient.mc.level.getBlockState(blockPos.above()).isAir();
    }

    public static Block getBlock(BlockPos pos) {
        return MeteorClient.mc.level.getBlockState(pos).getBlock();
    }

    public static Direction getClickSide(BlockPos pos) {
        Set<Direction> visibleSides = HeBlockUtils.getVisibleDirections(MeteorClient.mc.player.getEyePosition(), pos.getCenter());
        for (Direction direction : Direction.values()) {
            Block block;
            BlockState state = MeteorClient.mc.level.getBlockState(pos.relative(direction));
            if (state.isAir() || (block = state.getBlock()) instanceof LiquidBlock || HeBlockUtils.isInteractableBlock(block) && !MeteorClient.mc.player.isShiftKeyDown() || !visibleSides.contains(direction.getOpposite())) continue;
            return direction.getOpposite();
        }
        return null;
    }

    public static Set<Direction> getVisibleDirections(Vec3 from, Vec3 to) {
        return HeBlockUtils.getVisibleDirections(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    public static Set<Direction> getVisibleDirections(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = fromX - toX;
        double dy = fromY - toY;
        double dz = fromZ - toZ;
        HashSet<Direction> directions = new HashSet<Direction>(6);
        if (dy > 0.5) {
            directions.add(Direction.UP);
        } else if (dy < -0.5) {
            directions.add(Direction.DOWN);
        } else {
            directions.add(Direction.UP);
            directions.add(Direction.DOWN);
        }
        if (dx > 0.5) {
            directions.add(Direction.EAST);
        } else if (dx < -0.5) {
            directions.add(Direction.WEST);
        } else {
            directions.add(Direction.EAST);
            directions.add(Direction.WEST);
        }
        if (dz > 0.5) {
            directions.add(Direction.SOUTH);
        } else if (dz < -0.5) {
            directions.add(Direction.NORTH);
        } else {
            directions.add(Direction.SOUTH);
            directions.add(Direction.NORTH);
        }
        return directions;
    }

    public static Direction getPlaceSide(BlockPos pos, double maxDistance, double unused) {
        double nearestDistance = 2.147483647E9;
        Direction bestSide = null;
        Vec3 eyePos = MeteorClient.mc.player.getEyePosition();
        Set<Direction> visibleSides = HeBlockUtils.getVisibleDirections(eyePos, pos.getCenter());
        for (Direction direction : Direction.values()) {
            double distance;
            Vec3 clickPoint;
            BlockPos neighborPos;
            if (!visibleSides.contains(direction.getOpposite()) || !HeBlockUtils.isClickable(neighborPos = pos.relative(direction)) || HeBlockUtils.isReplaceable(neighborPos) || !HeBlockUtils.hasLineOfSight(clickPoint = HeBlockUtils.getClickPoint(neighborPos, direction.getOpposite()), direction.getOpposite()) || (double)Mth.sqrt((float)((float)(distance = eyePos.distanceToSqr(clickPoint)))) > maxDistance || !(distance < nearestDistance)) continue;
            bestSide = direction;
            nearestDistance = distance;
        }
        return bestSide;
    }

    public static float getDirectionYaw(Direction direction) {
        if (direction == null) {
            return 0.0f;
        }
        return switch (direction) {
            case Direction.NORTH -> 180.0f;
            case Direction.WEST -> 90.0f;
            case Direction.EAST -> -90.0f;
            default -> 0.0f;
        };
    }

    public static boolean isContainer(BlockPos pos) {
        BlockEntity blockEntity = MeteorClient.mc.level.getBlockEntity(pos);
        return blockEntity instanceof Container;
    }

    public static boolean isInteractableBlock(Block block) {
        return block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof CraftingTableBlock || block instanceof FurnaceBlock || block instanceof AnvilBlock || block instanceof BrewingStandBlock || block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof EnchantingTableBlock || block instanceof ShulkerBoxBlock || block instanceof BarrelBlock || block instanceof BedBlock || block instanceof TrapDoorBlock;
    }

    public static boolean isObserverOrHopper(Block block) {
        return block == Blocks.OBSERVER || block == Blocks.HOPPER;
    }

    static class HeBlockUtilsDirectionSwitchMap {
        static final /* synthetic */ int[] a;

        static {
            a = new int[Direction.values().length];
            try {
                HeBlockUtilsDirectionSwitchMap.a[Direction.NORTH.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                HeBlockUtilsDirectionSwitchMap.a[Direction.WEST.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                HeBlockUtilsDirectionSwitchMap.a[Direction.EAST.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
        }
    }
}
