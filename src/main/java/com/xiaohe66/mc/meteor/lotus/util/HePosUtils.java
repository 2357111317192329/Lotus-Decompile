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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HePosUtils {
    public static boolean isEntityInside(BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape((BlockGetter)MeteorClient.mc.level, pos);
        if (shape.isEmpty()) {
            return false;
        }
        shape = shape.move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        AABB box = new AABB(pos);
        Level level0 = MeteorClient.mc.level;
        List<Entity> list = level0.getEntities((Entity) null, box, entity -> entity.isAlive() && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb) && !(entity instanceof ThrownExperienceBottle) && !(entity instanceof Arrow) && !(entity instanceof EndCrystal));
        for (Entity entity : list) {
            if (!Shapes.joinIsNotEmpty((VoxelShape)shape, (VoxelShape)Shapes.create((AABB)entity.getBoundingBox()), (BooleanOp)BooleanOp.AND)) continue;
            return true;
        }
        return false;
    }

    public static Map<ItemBo, BlockPos> getItemFrameKitPosMap(int range) {
        List<ItemFrame> itemFrames = MeteorClient.mc.level.getEntitiesOfClass(ItemFrame.class, MeteorClient.mc.player.getBoundingBox().inflate((double)range), frame -> true);
        HashMap<ItemBo, BlockPos> kitPosMap = new HashMap<ItemBo, BlockPos>();
        for (ItemFrame frame : itemFrames) {
            BlockPos frameBlockPos;
            BlockPos putPos;
            BlockState putPosState;
            ItemStack frameHeldItemStack = frame.getItem();
            if (frameHeldItemStack.isEmpty() || !HeItemUtils.isShulkerBox((putPosState = MeteorClient.mc.level.getBlockState(putPos = (frameBlockPos = frame.blockPosition()).relative(Direction.Axis.Y, -2))).getBlock().asItem())) continue;
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

    public static Optional<BlockPos> getOtherChestPos(ItemFrame itemFrame) {
        BlockPos attachedPos = itemFrame.getPos();
        if (attachedPos == null) {
            return Optional.empty();
        }
        BlockPos chestPos = attachedPos.relative(itemFrame.getNearestViewDirection().getOpposite());
        BlockState chestState = MeteorClient.mc.level.getBlockState(chestPos);
        if (chestState.getBlock() != Blocks.CHEST) {
            return Optional.empty();
        }
        ChestType chestType = (ChestType)chestState.getValue((Property)ChestBlock.TYPE);
        if (chestType == ChestType.SINGLE) {
            return Optional.empty();
        }
        BlockEntity blockEntity = MeteorClient.mc.level.getBlockEntity(chestPos);
        if (!(blockEntity instanceof ChestBlockEntity)) {
            return Optional.empty();
        }
        ChestBlockEntity chestEntity = (ChestBlockEntity)blockEntity;
        Direction facing = (Direction)chestState.getValue((Property)ChestBlock.FACING);
        BlockPos otherPos = null;
        if (chestType == ChestType.LEFT) {
            otherPos = HePosUtils.getRightChestPos(chestPos, facing);
        } else if (chestType == ChestType.RIGHT) {
            otherPos = HePosUtils.getLeftChestPos(chestPos, facing);
        }
        if (otherPos == null) {
            return Optional.empty();
        }
        BlockState otherState = MeteorClient.mc.level.getBlockState(otherPos);
        if (otherState.getBlock() != Blocks.CHEST) {
            return Optional.empty();
        }
        ChestType otherChestType = (ChestType)otherState.getValue((Property)ChestBlock.TYPE);
        if (otherChestType == ChestType.SINGLE) {
            return Optional.empty();
        }
        Direction otherFacing = (Direction)otherState.getValue((Property)ChestBlock.FACING);
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

    public static Vec3 getCameraPos() {
        Freecam freecam = (Freecam)Modules.get().get(Freecam.class);
        if (freecam != null && freecam.isActive()) {
            return new Vec3(freecam.pos.x, freecam.pos.y, freecam.pos.z);
        }
        return MeteorClient.mc.player.position();
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
