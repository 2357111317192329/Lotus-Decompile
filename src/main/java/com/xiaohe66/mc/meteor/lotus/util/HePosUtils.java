package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction.Axis;

public class HePosUtils {
   public static final MinecraftClient mc = MinecraftClient.getInstance();

   public static Map<ItemBo, BlockPos> getItemFrameKitPosMap(int range) {
      List<ItemFrameEntity> itemFrames = mc.world.getEntitiesByClass(ItemFrameEntity.class, mc.player.getBoundingBox().expand(range), framex -> true);
      Map<ItemBo, BlockPos> kitPosMap = new HashMap<>();

      for (ItemFrameEntity frame : itemFrames) {
         ItemStack frameHeldItemStack = frame.getHeldItemStack();
         if (!frameHeldItemStack.isEmpty()) {
            BlockPos frameBlockPos = frame.getBlockPos();
            BlockPos putPos = frameBlockPos.offset(Axis.Y, -2);
            BlockState putPosState = mc.world.getBlockState(putPos);
            if (HeItemUtils.isShulkerBox(putPosState.getBlock().asItem())) {
               ItemBo key = new ItemBo(frameHeldItemStack);
               kitPosMap.put(key, putPos);
            }
         }
      }

      StringBuilder stringBuilder = new StringBuilder();

      for (ItemBo villagerItem : kitPosMap.keySet()) {
         stringBuilder.append(',').append(villagerItem.getName());
      }

      ChatUtils.info("成功识别: " + stringBuilder.substring(1), new Object[0]);
      return kitPosMap;
   }

   public static Optional<BlockPos> getOtherChestPos(ItemFrameEntity itemFrame) {
      BlockPos attachedPos = itemFrame.getAttachedBlockPos();
      if (attachedPos == null) {
         return Optional.empty();
      }

      BlockPos chestPos = attachedPos.offset(itemFrame.getFacing().getOpposite());
      BlockState chestState = mc.world.getBlockState(chestPos);
      if (chestState.getBlock() != Blocks.CHEST) {
         return Optional.empty();
      }

      ChestType chestType = (ChestType)chestState.get(ChestBlock.CHEST_TYPE);
      if (chestType == ChestType.SINGLE) {
         return Optional.empty();
      }

      if (mc.world.getBlockEntity(chestPos) instanceof ChestBlockEntity chestEntity) {
         Direction facing = (Direction)chestState.get(ChestBlock.FACING);
         BlockPos otherPos = null;
         if (chestType == ChestType.LEFT) {
            otherPos = getRightChestPos(chestPos, facing);
         } else if (chestType == ChestType.RIGHT) {
            otherPos = getLeftChestPos(chestPos, facing);
         }

         if (otherPos == null) {
            return Optional.empty();
         }

         BlockState otherState = mc.world.getBlockState(otherPos);
         if (otherState.getBlock() != Blocks.CHEST) {
            return Optional.empty();
         }

         ChestType otherChestType = (ChestType)otherState.get(ChestBlock.CHEST_TYPE);
         if (otherChestType == ChestType.SINGLE) {
            return Optional.empty();
         }

         Direction otherFacing = (Direction)otherState.get(ChestBlock.FACING);
         if (facing != otherFacing) {
            return Optional.empty();
         }

         boolean typesMatch = chestType == ChestType.LEFT && otherChestType == ChestType.RIGHT
            || chestType == ChestType.RIGHT && otherChestType == ChestType.LEFT;
         return !typesMatch ? Optional.empty() : Optional.of(otherPos);
      } else {
         return Optional.empty();
      }
   }

   private static BlockPos getRightChestPos(BlockPos pos, Direction facing) {
      return switch (facing) {
         case NORTH -> pos.east();
         case SOUTH -> pos.west();
         case WEST -> pos.north();
         case EAST -> pos.south();
         default -> null;
      };
   }

   private static BlockPos getLeftChestPos(BlockPos pos, Direction facing) {
      return switch (facing) {
         case NORTH -> pos.west();
         case SOUTH -> pos.east();
         case WEST -> pos.south();
         case EAST -> pos.north();
         default -> null;
      };
   }
}
