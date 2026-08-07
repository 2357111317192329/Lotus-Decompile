package com.xiaohe66.mc.meteor.lotus.util;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.Success;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockPlaceUtils {
   private static final Logger log = LoggerFactory.getLogger(BlockPlaceUtils.class);
   private static final double RAYCAST_OFFSET = 1.0E-5;

   public static boolean place(
      BlockPos schematicPos,
      BlockState schematicBlockState,
      Direction direction,
      boolean rotate,
      boolean swingHand,
      boolean clientSide,
      double printingRange
   ) {
      BlockPos adjacentPos = schematicPos.relative(direction);
      if (rotate) {
         BlockState adjacentState = MeteorClient.mc.level.getBlockState(adjacentPos);
         if (!canPlaceAgainst(adjacentState, adjacentPos)) {
            return false;
         }

         Vec3 hitPos = raycastToFace(schematicBlockState, adjacentPos, direction, printingRange);
         if (hitPos == null) {
            return false;
         }

         log.info("place hitPos, {} {} {}", new Object[]{hitPos.x(), hitPos.y(), hitPos.z()});
         double yaw = Rotations.getYaw(hitPos);
         double pitch = Rotations.getPitch(hitPos);
         log.info("place rotate, yaw : {}, pitch : {}", yaw, pitch);
         Rotations.rotate(yaw, pitch, 50, clientSide, () -> place(new BlockHitResult(hitPos, direction.getOpposite(), adjacentPos, false), swingHand));
      } else {
         Vec3 hitPos = new Vec3(schematicPos.getX() + 0.5, schematicPos.getY() + 0.5, schematicPos.getZ() + 0.5);
         place(new BlockHitResult(hitPos, direction.getOpposite(), adjacentPos, false), swingHand);
      }

      return true;
   }

   private static void place(BlockHitResult blockHitResult, boolean swing) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null && MeteorClient.mc.getConnection() != null) {
         boolean wasSneaking = MeteorClient.mc.player.isShiftKeyDown();
         MeteorClient.mc.player.setShiftKeyDown(false);
         InteractionResult result = MeteorClient.mc.gameMode.useItemOn(MeteorClient.mc.player, InteractionHand.MAIN_HAND, blockHitResult);
         if (result instanceof Success) {
            if (swing) {
               MeteorClient.mc.player.swing(InteractionHand.MAIN_HAND);
            } else {
               MeteorClient.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
         }

         MeteorClient.mc.player.setShiftKeyDown(wasSneaking);
      }
   }

   public static Direction getVisiblePlaceSide(BlockPos schematicPos, BlockState schematicState, double printingRange) {
      if (MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         Direction blockFacing = getBlockFacingDirection(schematicState);
         if (blockFacing != null) {
            Direction lookDirection = blockFacing.getOpposite();
            return isFaceVisible(schematicPos, schematicState, lookDirection, printingRange) ? lookDirection : null;
         }

         for (Direction direction : Direction.values()) {
            if (isFaceVisible(schematicPos, schematicState, direction, printingRange)) {
               return direction;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean isFaceVisible(BlockPos schematicPos, BlockState schematicState, Direction lookDirection, double printingRange) {
      BlockPos tryClickBlockPos = schematicPos.relative(lookDirection);
      BlockState tryClickBlockState = MeteorClient.mc.level.getBlockState(tryClickBlockPos);
      if (!canPlaceAgainst(tryClickBlockState, tryClickBlockPos)) {
         return false;
      }

      if (schematicState.getBlock() instanceof SlabBlock slabBlock) {
         SlabType slabType = (SlabType)schematicState.getValue(SlabBlock.TYPE);
         if (lookDirection == Direction.DOWN && slabType == SlabType.TOP) {
            return false;
         }

         if (lookDirection == Direction.UP && slabType == SlabType.BOTTOM) {
            return false;
         }
      }

      Vec3 target = raycastToFace(schematicState, tryClickBlockPos, lookDirection, printingRange);
      log.info(
         "raycastToFace : {}, {}, {} {} {} -> {} {} {} : {}",
         new Object[]{
            target != null,
            lookDirection.name(),
            schematicPos.getX(),
            schematicPos.getY(),
            schematicPos.getZ(),
            tryClickBlockPos.getX(),
            tryClickBlockPos.getY(),
            tryClickBlockPos.getZ(),
            schematicState.getBlock().getName()
         }
      );
      return target != null;
   }

   private static Direction getBlockFacingDirection(BlockState state) {
      if (state.hasProperty(BlockStateProperties.FACING)) {
         return (Direction)state.getValue(BlockStateProperties.FACING);
      } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
         return (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      } else if (state.hasProperty(BlockStateProperties.AXIS)) {
         Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
         return Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
      } else {
         return null;
      }
   }

   private static boolean isSlabFaceVisible(BlockPos schematicPos, Direction direction, BlockState slabState) {
      SlabType slabType = (SlabType)slabState.getValue(SlabBlock.TYPE);
      if (slabType == SlabType.DOUBLE) {
         return false;
      }

      AABB collisionBox = slabState.getCollisionShape(MeteorClient.mc.level, schematicPos).bounds();
      double minY = slabType == SlabType.BOTTOM ? 0.0 : 0.5;
      double maxY = slabType == SlabType.TOP ? 1.0 : 0.5;
      AABB adjustedBox = new AABB(collisionBox.minX, minY, collisionBox.minZ, collisionBox.maxX, maxY, collisionBox.maxZ);
      return raycastToAdjustedBox(schematicPos, direction, adjustedBox);
   }

   private static boolean isTrapdoorFaceVisible(Direction dir, BlockState trapdoorState) {
      Direction facing = (Direction)trapdoorState.getValue(TrapDoorBlock.FACING);
      Boolean open = (Boolean)trapdoorState.getValue(TrapDoorBlock.OPEN);
      return !open && dir == facing;
   }

   private static Vec3 raycastToFace(BlockState schematicState, BlockPos adjacentPos, Direction direction, double printingRange) {
      Direction opposite = direction.getOpposite();
      Vec3 faceCenter = getFaceCenter(adjacentPos, opposite);
      BlockState adjacentBlockState = MeteorClient.mc.level.getBlockState(adjacentPos);
      faceCenter = fixSlabBlock(schematicState, adjacentBlockState, faceCenter);
      Vec3 eyePos = MeteorClient.mc.player.getEyePosition(1.0F);
      Vec3 rayVec = faceCenter.subtract(eyePos);
      double maxDistance = Math.min(eyePos.distanceTo(faceCenter) + 0.5, printingRange);
      Vec3 end = eyePos.add(rayVec.normalize().scale(maxDistance));
      log.info(
         " eyePos : {} {} {}, faceCenter : {} {} {}, end : {} {} {}",
         new Object[]{
            eyePos.x(),
            eyePos.y(),
            eyePos.z(),
            faceCenter.x(),
            faceCenter.y(),
            faceCenter.z(),
            end.x(),
            end.y(),
            end.z()
         }
      );
      ClipContext context = new ClipContext(eyePos, end, Block.COLLIDER, Fluid.NONE, MeteorClient.mc.player);
      BlockHitResult hit = MeteorClient.mc.level.clip(context);
      BlockPos hitBlockPos = hit.getBlockPos();
      log.info("hit pos, {},{},{},{}", new Object[]{hitBlockPos.getX(), hitBlockPos.getY(), hitBlockPos.getZ(), hit.getDirection()});
      boolean ok = hit.getType() == Type.BLOCK && hit.getBlockPos().equals(adjacentPos) && hit.getDirection() == opposite;
      return ok ? faceCenter : null;
   }

   private static Vec3 fixSlabBlock(BlockState schematicBlockState, BlockState adjacentBlockState, Vec3 faceCenter) {
      net.minecraft.world.level.block.Block schematicBlock = schematicBlockState.getBlock();
      if (!(schematicBlock instanceof SlabBlock)) {
         return faceCenter;
      }

      net.minecraft.world.level.block.Block adjacentBlock = adjacentBlockState.getBlock();
      if (!(adjacentBlock instanceof SlabBlock)) {
         SlabType schematicBlockSlabType = (SlabType)schematicBlockState.getValue(SlabBlock.TYPE);
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
      AABB box = MeteorClient.mc.level.getBlockState(pos).getShape(MeteorClient.mc.level, pos).bounds();
      double x = pos.getX() + box.minX + (box.maxX - box.minX) * 0.5;
      double y = pos.getY() + box.minY + (box.maxY - box.minY) * 0.5;
      double z = pos.getZ() + box.minZ + (box.maxZ - box.minZ) * 0.5;
      return new Vec3(
         x + direction.getStepX() * (box.maxX - box.minX) * 0.5,
         y + direction.getStepY() * (box.maxY - box.minY) * 0.5,
         z + direction.getStepZ() * (box.maxZ - box.minZ) * 0.5
      );
   }

   private static Direction getDirectionFromPlayerView(BlockPos targetPos) {
      Vec3 eyePos = new Vec3(
         MeteorClient.mc.player.getX(), MeteorClient.mc.player.getEyeY(), MeteorClient.mc.player.getZ()
      );
      Vec3 targetCenter = Vec3.atCenterOf(targetPos);
      Vec3 lookVec = targetCenter.subtract(eyePos).normalize();
      return Direction.getApproximateNearest(lookVec.x, lookVec.y, lookVec.z);
   }

   private static boolean canPlaceAgainst(BlockState tryClickBlockState, BlockPos tryClickPos) {
      if (!tryClickBlockState.isAir() && !tryClickBlockState.liquid()) {
         VoxelShape shape = tryClickBlockState.getCollisionShape(MeteorClient.mc.level, tryClickPos);
         if (shape.isEmpty()) {
            return false;
         }

         net.minecraft.world.level.block.Block block = tryClickBlockState.getBlock();
         return block != Blocks.GLASS && block != Blocks.ICE
            ? net.minecraft.world.level.block.Block.isShapeFullBlock(shape)
               || tryClickBlockState.getBlock() == Blocks.GLASS
               || tryClickBlockState.getBlock() instanceof StainedGlassBlock
               || tryClickBlockState.getBlock() instanceof StairBlock
               || tryClickBlockState.getBlock() instanceof SlabBlock
            : false;
      } else {
         return false;
      }
   }

   private static boolean raycastToAdjustedBox(BlockPos schematicPos, Direction direction, AABB adjustedBox) {
      Direction opposite = direction.getOpposite();
      Vec3 eyePos = MeteorClient.mc.player.getEyePosition(1.0F);
      int samples = 3;
      double xStep = (adjustedBox.maxX - adjustedBox.minX) / 4.0;
      double yStep = (adjustedBox.maxY - adjustedBox.minY) / 4.0;
      double zStep = (adjustedBox.maxZ - adjustedBox.minZ) / 4.0;

      for (int i = 1; i <= 3; i++) {
         for (int j = 1; j <= 3; j++) {
            for (int k = 1; k <= 3; k++) {
               double x = adjustedBox.minX + xStep * i;
               double y = adjustedBox.minY + yStep * j;
               double z = adjustedBox.minZ + zStep * k;
               Vec3 point = new Vec3(schematicPos.getX() + x, schematicPos.getY() + y, schematicPos.getZ() + z);
               BlockHitResult hit = MeteorClient.mc
                  .level
                  .clip(new ClipContext(eyePos, point, Block.COLLIDER, Fluid.NONE, MeteorClient.mc.player));
               if (hit.getType() == Type.BLOCK && hit.getBlockPos().equals(schematicPos) && hit.getDirection() == opposite) {
                  log.info("raycastToAdjustedBox return false");
                  return true;
               }
            }
         }
      }

      log.info("raycastToAdjustedBox return false");
      return false;
   }

   private record PlacementContext(BlockPos pos, Direction side, Vec3 hitPos) {
   }
}
