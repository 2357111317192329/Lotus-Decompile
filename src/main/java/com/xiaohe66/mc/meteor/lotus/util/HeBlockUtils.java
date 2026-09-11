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
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.BlockView;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.HopperBlock;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.hit.HitResult;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.state.property.Property;
import net.minecraft.block.enums.SlabType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import org.joml.Vector3fc;

public class HeBlockUtils {
    public static boolean isClickable(BlockPos pos) {
        return HeBlockUtils.isClickable(pos, false);
    }

    public static boolean isClickable(BlockPos pos, boolean allowClick) {
        BlockState state = MeteorClient.mc.world.getBlockState(pos);
        if (state.getOutlineShape((BlockView)MeteorClient.mc.world, pos).isEmpty()) {
            return false;
        }
        if (allowClick) {
            return true;
        }
        Block block = state.getBlock();
        if (HeBlockUtils.isInteractableBlock(block)) {
            return MeteorClient.mc.player.isSneaking();
        }
        return true;
    }

    public static boolean isReplaceable(BlockPos pos) {
        return MeteorClient.mc.world.getBlockState(pos).isReplaceable();
    }

    public static boolean hasLineOfSight(Vec3d target, Direction side) {
        if (side == null) {
            return false;
        }
        BlockHitResult result = MeteorClient.mc.world.raycast(new RaycastContext(MeteorClient.mc.player.getEyePos(), target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)MeteorClient.mc.player));
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
        Vec3d eyePos = MeteorClient.mc.player.getEyePos();
        Set<Direction> visibleSides = HeBlockUtils.getVisibleDirections(eyePos, pos.toCenterPos());
        Direction airSide = null;
        double airDistance = Double.MAX_VALUE;
        Direction blockSide = null;
        double blockDistance = Double.MAX_VALUE;
        for (Direction direction : visibleSides) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = MeteorClient.mc.world.getBlockState(neighborPos);
            Vec3d faceCenter = pos.toCenterPos().add((double)direction.getOffsetX() * 0.5, (double)direction.getOffsetY() * 0.5, (double)direction.getOffsetZ() * 0.5);
            double distance = eyePos.squaredDistanceTo(faceCenter);
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
        Vec3i vector = side.getVector();
        double offset = 0.45;
        Vec3d hitPos = new Vec3d((double)pos.getX() + 0.5 + (double)vector.getX() * offset, (double)pos.getY() + 0.5 + (double)vector.getY() * offset, (double)pos.getZ() + 0.5 + (double)vector.getZ() * offset);
        HeBlockUtils.open(pos, side, hitPos);
    }

    public static void open(BlockPos pos, Direction side, Vec3d hitPos) {
        BlockHitResult result = new BlockHitResult(hitPos, side, pos, false);
        MeteorClient.mc.interactionManager.interactBlock(MeteorClient.mc.player, Hand.MAIN_HAND, result);
    }

    public static boolean clickAdjacentBlock(BlockPos pos) {
        Direction clickSide = HeBlockUtils.getClickSide(pos);
        if (clickSide == null) {
            return false;
        }
        BlockPos neighborPos = pos.offset(clickSide.getOpposite());
        return HeBlockUtils.clickBlock(neighborPos, clickSide);
    }

    public static boolean clickAdjacentBlock(BlockPos pos, Direction side) {
        BlockPos neighborPos = pos.offset(side);
        return HeBlockUtils.clickBlock(neighborPos, side.getOpposite());
    }

    public static boolean clickBlock(BlockPos pos, Direction side) {
        Vec3d hitPos = HeBlockUtils.getClickPoint(pos, side);
        return HeBlockUtils.clickBlock(pos, side, hitPos);
    }

    private static boolean clickBlockWithOffset(BlockPos pos, Direction side, Vec3d offset) {
        return HeBlockUtils.clickBlock(pos, side, pos.toCenterPos().add(offset));
    }

    private static boolean clickBlock(BlockPos pos, Direction side, Vec3d hitPos) {
        float yaw = MeteorClient.mc.player.getYaw();
        float pitch = MeteorClient.mc.player.getPitch();
        HeRotation rotation = HeRotationUtils.getRotation(hitPos);
        HeRotationUtils.keepRotation(rotation);
        BlockHitResult result = new BlockHitResult(hitPos, side, pos, false);
        boolean clicked = HeBlockUtils.interact(result);
        HeRotationUtils.keepRotation(yaw, pitch);
        return clicked;
    }

    private static boolean interact(BlockHitResult result) {
        ActionResult actionResult;
        BlockState state = MeteorClient.mc.world.getBlockState(result.getBlockPos());
        boolean shouldSneak = HeBlockUtils.isInteractableBlock(state.getBlock()) && !MeteorClient.mc.player.isSneaking();
        if (shouldSneak) {
            HePlayerUtils.startSneaking();
        }
        if ((actionResult = MeteorClient.mc.interactionManager.interactBlock(MeteorClient.mc.player, Hand.MAIN_HAND, result)).isAccepted()) {
            MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
        }
        if (shouldSneak) {
            HePlayerUtils.stopSneaking();
        }
        return actionResult.isAccepted();
    }

    @Deprecated
    public static boolean place(BlockPos blockPos, int slot, boolean checkEntities, Direction side) {
        BlockItem blockItem;
        if (slot < 0 || slot > 8) {
            return false;
        }
        Block toPlace = Blocks.OBSIDIAN;
        PlayerInventory inventory = MeteorClient.mc.player.getInventory();
        ItemStack itemStack = inventory.getStack(slot);
        Item item = itemStack.getItem();
        if (item instanceof BlockItem) {
            blockItem = (BlockItem)item;
            toPlace = blockItem.getBlock();
        }
        if (!BlockUtils.canPlaceBlock((BlockPos)blockPos, (boolean)checkEntities, (Block)toPlace)) {
            return false;
        }
        BlockPos neighbour = blockPos.offset(side);
        Vec3i vector = side.getOpposite().getVector();
        double offset = 0.45;
        Vec3d hitPos = new Vec3d((double)neighbour.getX() + 0.5 + (double)vector.getX() * offset, (double)neighbour.getY() + 0.5 + (double)vector.getY() * offset, (double)neighbour.getZ() + 0.5 + (double)vector.getZ() * offset);
        BlockHitResult bhr = new BlockHitResult(hitPos, side.getOpposite(), (BlockPos)neighbour, false);
        if (inventory.getSelectedSlot() != slot) {
            InvUtils.swap((int)slot, (boolean)false);
        }
        HeRotationUtils.rotate(hitPos, () -> BlockUtils.interact((BlockHitResult)bhr, (Hand)Hand.MAIN_HAND, (boolean)true));
        return true;
    }

    public static boolean placeBlock(BlockPos pos, Direction faceDirection) {
        Direction clickSide = HeBlockUtils.getClickSide(pos);
        if (clickSide == null) {
            return false;
        }
        BlockPos neighborPos = pos.offset(clickSide.getOpposite());
        return HeBlockUtils.placeBlock(neighborPos, neighborPos, clickSide, faceDirection);
    }

    public static boolean placeBlock(BlockPos pos, Direction faceDirection, Direction blockFacing) {
        BlockPos neighborPos = pos.offset(blockFacing);
        Direction clickSide = blockFacing.getOpposite();
        return HeBlockUtils.placeBlock(neighborPos, neighborPos, clickSide, faceDirection);
    }

    private static boolean placeBlock(BlockPos targetPos, BlockPos clickPos, Direction clickSide, Direction faceDirection) {
        return HeBlockUtils.placeBlock(targetPos, clickPos, clickSide, faceDirection, new Vec3d((Vector3fc)clickSide.getUnitVector()).multiply(0.5));
    }

    private static boolean placeBlock(BlockPos targetPos, BlockPos clickPos, Direction clickSide, Direction faceDirection, Vec3d offset) {
        Vec3d clickPoint = clickPos.toCenterPos().add(offset);
        Vec3d visiblePoint = HeBlockUtils.findVisiblePoint(clickPoint, clickSide, true);
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
        if (!state.getProperties().contains(Properties.SLAB_TYPE)) {
            HeBlockUtils.clickAdjacentBlock(pos);
            return;
        }
        SlabType slabType = (SlabType)state.get((Property)Properties.SLAB_TYPE);
        Direction clickSide = HeBlockUtils.getSlabPlaceDirection(pos, slabType == SlabType.TOP);
        if (clickSide == null) {
            return;
        }
        BlockPos neighborPos = pos.offset(clickSide.getOpposite());
        if (slabType == SlabType.TOP || slabType == SlabType.BOTTOM) {
            HeBlockUtils.clickBlock(neighborPos, clickSide);
        } else if (slabType == SlabType.DOUBLE) {
            HeBlockUtils.clickBlockWithOffset(neighborPos, clickSide, new Vec3d(0.0, 0.25, 0.0));
        } else {
            HeBlockUtils.clickAdjacentBlock(pos);
        }
    }

    public static void placeStairs(BlockPos pos, BlockState state) {
        Direction facing = (Direction)state.get((Property)Properties.HORIZONTAL_FACING);
        BlockHalf blockHalf = (BlockHalf)state.get((Property)Properties.BLOCK_HALF);
        if (blockHalf == BlockHalf.TOP) {
            Direction clickSide = HeBlockUtils.getSlabPlaceDirection(pos, true);
            if (clickSide == null) {
                return;
            }
            BlockPos neighborPos = pos.offset(clickSide.getOpposite());
            HeBlockUtils.placeBlock(neighborPos, pos, clickSide, facing, new Vec3d(0.0, 0.25, 0.0));
        } else {
            Direction clickSide = HeBlockUtils.getSlabPlaceDirection(pos, false);
            if (clickSide == null) {
                return;
            }
            BlockPos neighborPos = pos.offset(clickSide.getOpposite());
            HeBlockUtils.placeBlock(neighborPos, pos, clickSide, facing, new Vec3d(0.0, -0.25, 0.0));
        }
    }

    public static Direction getSlabPlaceDirection(BlockPos pos, boolean isTopHalf) {
        BlockState state;
        Set<Direction> visibleSides = HeBlockUtils.getVisibleDirections(MeteorClient.mc.player.getEyePos(), pos.toCenterPos());
        if (visibleSides.remove(Direction.DOWN) && isTopHalf && HeBlockUtils.isSolid(state = MeteorClient.mc.world.getBlockState(pos.up()))) {
            return Direction.DOWN;
        }
        if (visibleSides.remove(Direction.UP) && !isTopHalf && HeBlockUtils.isSolid(state = MeteorClient.mc.world.getBlockState(pos.down()))) {
            return Direction.UP;
        }
        for (Direction direction : visibleSides) {
            BlockState neighborState = MeteorClient.mc.world.getBlockState(pos.offset(direction.getOpposite()));
            if (!HeBlockUtils.isSolid(neighborState)) continue;
            if (neighborState.getBlock() instanceof SlabBlock) {
                SlabType slabType = (SlabType)neighborState.get((Property)Properties.SLAB_TYPE);
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
        BlockPos upPos = pos.up();
        return MeteorClient.mc.world.getBlockState(upPos).getCollisionShape((BlockView)MeteorClient.mc.world, upPos).isEmpty();
    }

    public static boolean isReplaceable(BlockState state, BlockPos pos) {
        return state.getCollisionShape((BlockView)MeteorClient.mc.world, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    public static boolean isRail(BlockState state) {
        return state.isIn(BlockTags.RAILS);
    }

    private static boolean isNonFullCube(BlockState state, BlockPos pos) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        VoxelShape shape = state.getOutlineShape((BlockView)MeteorClient.mc.world, pos);
        if (shape.isEmpty()) {
            return false;
        }
        return !Block.isShapeFullCube((VoxelShape)shape);
    }

    public static List<BlockPos> listPosInSphere(int range, BlockPos pos) {
        Vec3d center = pos.toCenterPos();
        ArrayList<BlockPos> posList = new ArrayList<BlockPos>();
        for (int x = pos.getX() - range; x < pos.getX() + range; ++x) {
            for (int z = pos.getZ() - range; z < pos.getZ() + range; ++z) {
                for (int y = pos.getY() - range; y < pos.getY() + range; ++y) {
                    BlockPos curPos = new BlockPos(x, y, z);
                    if (curPos.toCenterPos().distanceTo(center) > (double)range || posList.contains(curPos)) continue;
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
        if (state.contains((Property)Properties.FACING)) {
            return (Direction)state.get((Property)Properties.FACING);
        }
        if (state.contains((Property)Properties.HORIZONTAL_FACING)) {
            return (Direction)state.get((Property)Properties.HORIZONTAL_FACING);
        }
        if (state.contains((Property)Properties.AXIS)) {
            Direction.Axis axis = (Direction.Axis)state.get((Property)Properties.AXIS);
            return Direction.from((Direction.Axis)axis, (Direction.AxisDirection)Direction.AxisDirection.POSITIVE);
        }
        return null;
    }

    public static Vec3d getFaceCenter(BlockPos pos, Direction direction) {
        Box box = MeteorClient.mc.world.getBlockState(pos).getOutlineShape((BlockView)MeteorClient.mc.world, pos).getBoundingBox();
        double x = (double)pos.getX() + box.minX + (box.maxX - box.minX) * 0.5;
        double y = (double)pos.getY() + box.minY + (box.maxY - box.minY) * 0.5;
        double z = (double)pos.getZ() + box.minZ + (box.maxZ - box.minZ) * 0.5;
        return new Vec3d(x + (double)direction.getOffsetX() * (box.maxX - box.minX) * 0.5, y + (double)direction.getOffsetY() * (box.maxY - box.minY) * 0.5, z + (double)direction.getOffsetZ() * (box.maxZ - box.minZ) * 0.5);
    }

    private static Vec3d getClickPoint(BlockPos pos, Direction side) {
        VoxelShape shape = MeteorClient.mc.world.getBlockState(pos).getOutlineShape((BlockView)MeteorClient.mc.world, pos);
        if (shape.isEmpty()) {
            return new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
        }
        Box box = shape.getBoundingBox();
        double halfWidth = (box.maxX - box.minX) * 0.5;
        double halfHeight = (box.maxY - box.minY) * 0.5;
        double halfDepth = (box.maxZ - box.minZ) * 0.5;
        double x = (double)pos.getX() + box.minX + halfWidth;
        double y = (double)pos.getY() + box.minY + halfHeight;
        double z = (double)pos.getZ() + box.minZ + halfDepth;
        return new Vec3d(x + (double)side.getOffsetX() * halfWidth, y + (double)side.getOffsetY() * halfHeight, z + (double)side.getOffsetZ() * halfDepth);
    }

    private static boolean hasLineOfSight(Vec3d from, Vec3d to) {
        BlockHitResult result = MeteorClient.mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)MeteorClient.mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    private static Vec3d findVisiblePoint(Vec3d target, Direction side, boolean includeYOffset) {
        Vec3d eyePos = MeteorClient.mc.player.getEyePos();
        if (HeBlockUtils.hasLineOfSight(eyePos, target)) {
            return target;
        }
        double xOffset = side.getAxis() == Direction.Axis.X ? 0.0 : 0.25;
        double yOffset = side.getAxis() == Direction.Axis.Y ? 0.0 : 0.25;
        double zOffset = side.getAxis() == Direction.Axis.Z ? 0.0 : 0.25;
        if (!includeYOffset) {
            yOffset = 0.0;
        }
        Vec3d visiblePoint = null;
        double nearestDistance = Double.MAX_VALUE;
        for (double offsetX : new double[]{-xOffset, 0.0, xOffset}) {
            for (double offsetY : new double[]{-yOffset, 0.0, yOffset}) {
                for (double offsetZ : new double[]{-zOffset, 0.0, zOffset}) {
                    double distance;
                    Vec3d candidate;
                    if (offsetX == 0.0 && offsetY == 0.0 && offsetZ == 0.0 || !HeBlockUtils.hasLineOfSight(eyePos, candidate = target.add(offsetX, offsetY, offsetZ)) || !((distance = eyePos.squaredDistanceTo(candidate)) < nearestDistance)) continue;
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
        return MeteorClient.mc.world.getBlockState(blockPos).isAir() && MeteorClient.mc.world.getBlockState(blockPos.up()).isAir();
    }

    public static Block getBlock(BlockPos pos) {
        return MeteorClient.mc.world.getBlockState(pos).getBlock();
    }

    public static Direction getClickSide(BlockPos pos) {
        Set<Direction> visibleSides = HeBlockUtils.getVisibleDirections(MeteorClient.mc.player.getEyePos(), pos.toCenterPos());
        for (Direction direction : Direction.values()) {
            Block block;
            BlockState state = MeteorClient.mc.world.getBlockState(pos.offset(direction));
            if (state.isAir() || (block = state.getBlock()) instanceof FluidBlock || HeBlockUtils.isInteractableBlock(block) && !MeteorClient.mc.player.isSneaking() || !visibleSides.contains(direction.getOpposite())) continue;
            return direction.getOpposite();
        }
        return null;
    }

    public static Set<Direction> getVisibleDirections(Vec3d from, Vec3d to) {
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
        Vec3d eyePos = MeteorClient.mc.player.getEyePos();
        Set<Direction> visibleSides = HeBlockUtils.getVisibleDirections(eyePos, pos.toCenterPos());
        for (Direction direction : Direction.values()) {
            double distance;
            Vec3d clickPoint;
            BlockPos neighborPos;
            if (!visibleSides.contains(direction.getOpposite()) || !HeBlockUtils.isClickable(neighborPos = pos.offset(direction)) || HeBlockUtils.isReplaceable(neighborPos) || !HeBlockUtils.hasLineOfSight(clickPoint = HeBlockUtils.getClickPoint(neighborPos, direction.getOpposite()), direction.getOpposite()) || (double)MathHelper.sqrt((float)((float)(distance = eyePos.squaredDistanceTo(clickPoint)))) > maxDistance || !(distance < nearestDistance)) continue;
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
        BlockEntity blockEntity = MeteorClient.mc.world.getBlockEntity(pos);
        return blockEntity instanceof Inventory;
    }

    public static boolean isInteractableBlock(Block block) {
        return block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof CraftingTableBlock || block instanceof FurnaceBlock || block instanceof AnvilBlock || block instanceof BrewingStandBlock || block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof EnchantingTableBlock || block instanceof ShulkerBoxBlock || block instanceof BarrelBlock || block instanceof BedBlock || block instanceof TrapdoorBlock;
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
