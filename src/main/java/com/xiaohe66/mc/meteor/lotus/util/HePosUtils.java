package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class HePosUtils {
   public static final Minecraft mc = Minecraft.getInstance();

   public static Map<ItemBo, BlockPos> getItemFrameKitPosMap(int range) {
      List<ItemFrame> itemFrames = mc.level.getEntitiesOfClass(ItemFrame.class, mc.player.getBoundingBox().inflate(range), framex -> true);
      //ChatUtils.info("itemFrames number: " + itemFrames.size());
      Map<ItemBo, BlockPos> kitPosMap = new HashMap<>();

      for (ItemFrame frame : itemFrames) {
         //ChatUtils.info("kitPosMap.size() = " + kitPosMap.size());
         ItemStack frameHeldItemStack = frame.getItem();
         if (!frameHeldItemStack.isEmpty()) {
            BlockPos frameBlockPos = frame.blockPosition();
            BlockPos putPos = frameBlockPos.relative(Axis.Y, -2);
            BlockState putPosState = mc.level.getBlockState(putPos);
            if (HeItemUtils.isShulkerBox(putPosState.getBlock().asItem())) {
               ItemBo key = new ItemBo(frameHeldItemStack);
               kitPosMap.put(key, putPos);
            }
         }
      }
      //ChatUtils.info("kitPosMap.size() = " + kitPosMap.size());

      String s = "";
      //ChatUtils.info("kitPosMap.keySet() = " + kitPosMap.keySet().toString());
      //ChatUtils.info("kitPosMap.keySet().size() = " + kitPosMap.keySet().size());
      for (ItemBo villagerItem : kitPosMap.keySet()) {
         s=s+","+villagerItem.getName();
         //ChatUtils.info("s = "+ s);
      }

      ChatUtils.info("成功识别: " + s);
      return kitPosMap;
   }

   public static Optional<BlockPos> getOtherChestPos(ItemFrame itemFrame) {
      BlockPos attachedPos = itemFrame.getPos();
      if (attachedPos == null) {
         return Optional.empty();
      }

      BlockPos chestPos = attachedPos.relative(itemFrame.getNearestViewDirection().getOpposite());
      BlockState chestState = mc.level.getBlockState(chestPos);
      if (chestState.getBlock() != Blocks.CHEST) {
         return Optional.empty();
      }

      ChestType chestType = (ChestType)chestState.getValue(ChestBlock.TYPE);
      if (chestType == ChestType.SINGLE) {
         return Optional.empty();
      }

      if (mc.level.getBlockEntity(chestPos) instanceof ChestBlockEntity chestEntity) {
         Direction facing = (Direction)chestState.getValue(ChestBlock.FACING);
         BlockPos otherPos = null;
         if (chestType == ChestType.LEFT) {
            otherPos = getRightChestPos(chestPos, facing);
         } else if (chestType == ChestType.RIGHT) {
            otherPos = getLeftChestPos(chestPos, facing);
         }

         if (otherPos == null) {
            return Optional.empty();
         }

         BlockState otherState = mc.level.getBlockState(otherPos);
         if (otherState.getBlock() != Blocks.CHEST) {
            return Optional.empty();
         }

         ChestType otherChestType = (ChestType)otherState.getValue(ChestBlock.TYPE);
         if (otherChestType == ChestType.SINGLE) {
            return Optional.empty();
         }

         Direction otherFacing = (Direction)otherState.getValue(ChestBlock.FACING);
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
