/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  meteordevelopment.meteorclient.systems.modules.render.Freecam
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.ExperienceOrbEntity
 *  net.minecraft.entity.decoration.EndCrystalEntity
 *  net.minecraft.entity.decoration.ItemFrameEntity
 *  net.minecraft.entity.ItemEntity
 *  net.minecraft.entity.projectile.ArrowEntity
 *  net.minecraft.entity.projectile.thrown.ExperienceBottleEntity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.BlockView
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.ChestBlock
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.function.BooleanBiFunction
 *  net.minecraft.block.entity.BlockEntity
 *  net.minecraft.util.shape.VoxelShapes
 *  net.minecraft.block.entity.ChestBlockEntity
 *  net.minecraft.util.shape.VoxelShape
 *  net.minecraft.block.BlockState
 *  net.minecraft.block.enums.ChestType
 *  net.minecraft.state.property.Property
 *  */
package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Property;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class HePosUtils {
    public static boolean isEntityInside(BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape((BlockView)MeteorClient.mc.world, pos);
        if (shape.isEmpty()) {
            return false;
        }
        shape = shape.offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        Box box = new Box(pos);
        World level0 = MeteorClient.mc.world;
        List<Entity> list = level0.getOtherEntities((Entity) null, box, entity -> entity.isAlive() && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ExperienceBottleEntity) && !(entity instanceof ArrowEntity) && !(entity instanceof EndCrystalEntity));
        for (Entity entity : list) {
            if (!VoxelShapes.matchesAnywhere((VoxelShape)shape, (VoxelShape)VoxelShapes.cuboid((Box)entity.getBoundingBox()), (BooleanBiFunction)BooleanBiFunction.AND)) continue;
            return true;
        }
        return false;
    }

    public static Map<ItemBo, BlockPos> getItemFrameKitPosMap(int range) {
        List<ItemFrameEntity> itemFrames = MeteorClient.mc.world.getEntitiesByClass(ItemFrameEntity.class, MeteorClient.mc.player.getBoundingBox().expand((double)range), frame -> true);
        HashMap<ItemBo, BlockPos> kitPosMap = new HashMap<ItemBo, BlockPos>();
        for (ItemFrameEntity frame : itemFrames) {
            BlockPos frameBlockPos;
            BlockPos putPos;
            BlockState putPosState;
            ItemStack frameHeldItemStack = frame.getHeldItemStack();
            if (frameHeldItemStack.isEmpty() || !HeItemUtils.isShulkerBox((putPosState = MeteorClient.mc.world.getBlockState(putPos = (frameBlockPos = frame.getBlockPos()).offset(Direction.Axis.Y, -2))).getBlock().asItem())) continue;
            ItemBo key = new ItemBo((ItemStack)frameHeldItemStack);
            kitPosMap.put(key, putPos);
        }
        if (kitPosMap.isEmpty()) {
            ChatUtils.warning((String)"没有符合条件的盒子", (Object[])new Object[0]);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (ItemBo villagerItem : kitPosMap.keySet()) {
                stringBuilder.append(',').append(villagerItem.getName());
            }
            ChatUtils.info((String)("成功识别: " + stringBuilder.substring(1)), (Object[])new Object[0]);
        }
        return kitPosMap;
    }

    public static Optional<BlockPos> getOtherChestPos(ItemFrameEntity itemFrame) {
        BlockPos attachedPos = itemFrame.getAttachedBlockPos();
        if (attachedPos == null) {
            return Optional.empty();
        }
        BlockPos chestPos = attachedPos.offset(itemFrame.getFacing().getOpposite());
        BlockState chestState = MeteorClient.mc.world.getBlockState(chestPos);
        if (chestState.getBlock() != Blocks.CHEST) {
            return Optional.empty();
        }
        ChestType chestType = (ChestType)chestState.get((Property)ChestBlock.CHEST_TYPE);
        if (chestType == ChestType.SINGLE) {
            return Optional.empty();
        }
        BlockEntity blockEntity = MeteorClient.mc.world.getBlockEntity(chestPos);
        if (!(blockEntity instanceof ChestBlockEntity)) {
            return Optional.empty();
        }
        ChestBlockEntity chestEntity = (ChestBlockEntity)blockEntity;
        Direction facing = (Direction)chestState.get((Property)ChestBlock.FACING);
        BlockPos otherPos = null;
        if (chestType == ChestType.LEFT) {
            otherPos = HePosUtils.getRightChestPos(chestPos, facing);
        } else if (chestType == ChestType.RIGHT) {
            otherPos = HePosUtils.getLeftChestPos(chestPos, facing);
        }
        if (otherPos == null) {
            return Optional.empty();
        }
        BlockState otherState = MeteorClient.mc.world.getBlockState(otherPos);
        if (otherState.getBlock() != Blocks.CHEST) {
            return Optional.empty();
        }
        ChestType otherChestType = (ChestType)otherState.get((Property)ChestBlock.CHEST_TYPE);
        if (otherChestType == ChestType.SINGLE) {
            return Optional.empty();
        }
        Direction otherFacing = (Direction)otherState.get((Property)ChestBlock.FACING);
        if (facing != otherFacing) {
            return Optional.empty();
        }
        boolean typesMatch = chestType == ChestType.LEFT && otherChestType == ChestType.RIGHT || chestType == ChestType.RIGHT && otherChestType == ChestType.LEFT;
        if (!typesMatch) {
            return Optional.empty();
        }
        return Optional.of(otherPos);
    }

    private static BlockPos getRightChestPos(BlockPos pos, Direction facing) {
        return switch (facing) {
            case Direction.NORTH -> pos.east();
            case Direction.SOUTH -> pos.west();
            case Direction.WEST -> pos.north();
            case Direction.EAST -> pos.south();
            default -> null;
        };
    }

    private static BlockPos getLeftChestPos(BlockPos pos, Direction facing) {
        return switch (facing) {
            case Direction.NORTH -> pos.west();
            case Direction.SOUTH -> pos.east();
            case Direction.WEST -> pos.south();
            case Direction.EAST -> pos.north();
            default -> null;
        };
    }

    public static Vec3d getCameraPos() {
        Freecam freecam = (Freecam)Modules.get().get(Freecam.class);
        if (freecam != null && freecam.isActive()) {
            return new Vec3d(freecam.pos.x, freecam.pos.y, freecam.pos.z);
        }
        return MeteorClient.mc.player.getEntityPos();
    }

    public static Map<ItemBo, StoragePos> scanFrames(int range, int verticalRange) {
        BlockPos playerPos = MeteorClient.mc.player.getBlockPos();
        int x = playerPos.getX();
        int y = playerPos.getY();
        int z = playerPos.getZ();
        List<ItemFrameEntity> frames = MeteorClient.mc.world.getEntitiesByClass(ItemFrameEntity.class,
            new Box(x - range, y, z - range, x + range, y + verticalRange, z + range),
            frame -> !frame.getHeldItemStack().isEmpty());
        Vec3d playerEyePos = MeteorClient.mc.player.getEntityPos();
        HashMap<ItemBo, StoragePos> result = new HashMap<>();
        for (ItemFrameEntity frame : frames) {
            ItemStack stack = frame.getHeldItemStack();
            ItemBo itemBo = new ItemBo(stack);
            BlockPos framePos = frame.getBlockPos();
            BlockPos attachedPos = frame.getAttachedBlockPos();
            Direction facing = frame.getFacing();
            BlockPos putPos = attachedPos.up().offset(facing.getOpposite());
            if (MeteorClient.mc.world.getBlockState(putPos).getBlock() != Blocks.CHEST) {
                continue;
            }
            BlockPos kitPos = framePos.down(2);
            BlockState kitState = MeteorClient.mc.world.getBlockState(kitPos);
            if ((!kitState.isAir() && !(kitState.getBlock() instanceof ShulkerBoxBlock)) || kitPos.getY() != MeteorClient.mc.player.getBlockPos().getY()) {
                continue;
            }
            BlockPos btnPos = kitPos.offset(facing, 2);
            BlockPos takePos = putPos.down(2);
            StoragePos storagePos = new StoragePos(StorageItem.valueOf(itemBo), framePos, putPos, takePos, kitPos, btnPos);
            StoragePos existing = result.get(itemBo);
            if (existing == null) {
                result.put(itemBo, storagePos);
            } else {
                ChatUtils.warning("存在多个位置: %s", itemBo.getName());
                double newDistance = storagePos.getBtnPos().toCenterPos().distanceTo(playerEyePos);
                double oldDistance = existing.getBtnPos().toCenterPos().distanceTo(playerEyePos);
                if (newDistance < oldDistance) {
                    result.put(itemBo, storagePos);
                }
            }
        }
        return result;
    }

    public static Map<ItemBo, StoragePos> scanFramesPiston(int range, int verticalRange) {
        BlockPos playerPos = MeteorClient.mc.player.getBlockPos();
        int x = playerPos.getX();
        int y = playerPos.getY();
        int z = playerPos.getZ();
        List<ItemFrameEntity> frames = MeteorClient.mc.world.getEntitiesByClass(ItemFrameEntity.class,
            new Box(x - range, y, z - range, x + range, y + verticalRange, z + range),
            frame -> !frame.getHeldItemStack().isEmpty());
        Vec3d playerPosVec = MeteorClient.mc.player.getEntityPos();
        HashMap<ItemBo, StoragePos> result = new HashMap<>();
        for (ItemFrameEntity frame : frames) {
            ItemStack stack = frame.getHeldItemStack();
            ItemBo itemBo = new ItemBo(stack);
            BlockPos framePos = frame.getBlockPos();
            BlockPos attachedPos = frame.getAttachedBlockPos();
            Direction facing = frame.getFacing();
            BlockPos putPos = attachedPos.up().offset(facing.getOpposite());
            if (MeteorClient.mc.world.getBlockState(putPos).getBlock() != Blocks.CHEST) {
                continue;
            }
            BlockPos kitPos = framePos.down(2);
            BlockState kitState = MeteorClient.mc.world.getBlockState(kitPos);
            if (!kitState.isAir() && !(kitState.getBlock() instanceof ShulkerBoxBlock)) {
                continue;
            }
            if (MeteorClient.mc.world.getBlockState(kitPos.down()).getBlock() != Blocks.PISTON || kitPos.getY() != MeteorClient.mc.player.getBlockPos().getY()) {
                continue;
            }
            BlockPos btnPos = kitPos.offset(facing);
            BlockPos takePos = putPos.down(2);
            if (!(MeteorClient.mc.world.getBlockState(btnPos).getBlock() instanceof ButtonBlock)) {
                continue;
            }
            StoragePos storagePos = new StoragePos(StorageItem.valueOf(itemBo), framePos, putPos, takePos, kitPos, btnPos);
            StoragePos existing = result.get(itemBo);
            if (existing == null) {
                result.put(itemBo, storagePos);
            } else {
                ChatUtils.warning("存在多个位置: %s", itemBo.getName());
                double newDistance = storagePos.getBtnPos().toCenterPos().distanceTo(playerPosVec);
                double oldDistance = existing.getBtnPos().toCenterPos().distanceTo(playerPosVec);
                if (newDistance < oldDistance) {
                    result.put(itemBo, storagePos);
                }
            }
        }
        return result;
    }

    public static BlockPos getBlockPos(Vec3d pos) {
        int playerY = MeteorClient.mc.player.getBlockY();
        if (pos.y < (double) playerY - 0.75 || pos.y > (double) playerY + 2.3) {
            return null;
        }
        double range = 2.030625;
        Vec3d playerPos = MeteorClient.mc.player.getEntityPos();
        BlockPos floored = BlockPos.ofFloored(pos);
        BlockPos nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                BlockPos candidate = floored.add(dx, 0, dz).withY(playerY);
                double dX = candidate.getX() + 0.5 - pos.x;
                double dZ = candidate.getZ() + 0.5 - pos.z;
                if (dX * dX + dZ * dZ <= range && isStandableSpot(candidate)) {
                    double distance = candidate.toCenterPos().squaredDistanceTo(playerPos);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearest = candidate;
                    }
                }
            }
        }
        return nearest;
    }

    private static boolean isStandableSpot(BlockPos pos) {
        if (!MeteorClient.mc.world.getBlockState(pos).getCollisionShape(MeteorClient.mc.world, pos).isEmpty()) {
            return false;
        }
        if (!MeteorClient.mc.world.getBlockState(pos.up()).getCollisionShape(MeteorClient.mc.world, pos.up()).isEmpty()) {
            return false;
        }
        return !MeteorClient.mc.world.getBlockState(pos.down()).getCollisionShape(MeteorClient.mc.world, pos.down()).isEmpty();
    }

    static class HePosUtilsDirectionSwitchMap {
        static final /* synthetic */ int[] a;

        static {
            a = new int[Direction.values().length];
            try {
                HePosUtilsDirectionSwitchMap.a[Direction.NORTH.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                HePosUtilsDirectionSwitchMap.a[Direction.SOUTH.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                HePosUtilsDirectionSwitchMap.a[Direction.WEST.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                HePosUtilsDirectionSwitchMap.a[Direction.EAST.ordinal()] = 4;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
        }
    }
}
