package com.xiaohe66.mc.meteor.lotus.util;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.block.enums.SlabType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
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
      BlockPos adjacentPos = schematicPos.offset(direction);
      if (rotate) {
         BlockState adjacentState = MeteorClient.mc.world.getBlockState(adjacentPos);
         if (!canPlaceAgainst(adjacentState, adjacentPos)) {
            return false;
         }

         Vec3d hitPos = raycastToFace(schematicBlockState, adjacentPos, direction, printingRange);
         if (hitPos == null) {
            return false;
         }

         log.info("place hitPos, {} {} {}", new Object[]{hitPos.getX(), hitPos.getY(), hitPos.getZ()});
         double yaw = Rotations.getYaw(hitPos);
         double pitch = Rotations.getPitch(hitPos);
         log.info("place rotate, yaw : {}, pitch : {}", yaw, pitch);
         Rotations.rotate(yaw, pitch, 50, clientSide, () -> place(new BlockHitResult(hitPos, direction.getOpposite(), adjacentPos, false), swingHand));
      } else {
         Vec3d hitPos = new Vec3d(schematicPos.getX() + 0.5, schematicPos.getY() + 0.5, schematicPos.getZ() + 0.5);
         place(new BlockHitResult(hitPos, direction.getOpposite(), adjacentPos, false), swingHand);
      }

      return true;
   }

   private static void place(BlockHitResult blockHitResult, boolean swing) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.interactionManager != null && MeteorClient.mc.getNetworkHandler() != null) {
         boolean wasSneaking = MeteorClient.mc.player.isSneaking();
         MeteorClient.mc.player.setSneaking(false);
         ActionResult result = MeteorClient.mc.interactionManager.interactBlock(MeteorClient.mc.player, Hand.MAIN_HAND, blockHitResult);
         if (result instanceof Success) {
            if (swing) {
               MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
            } else {
               MeteorClient.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
         }

         MeteorClient.mc.player.setSneaking(wasSneaking);
      }
   }

   public static Direction getVisiblePlaceSide(BlockPos schematicPos, BlockState schematicState, double printingRange) {
      if (MeteorClient.mc.world != null && MeteorClient.mc.player != null) {
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
      BlockPos tryClickBlockPos = schematicPos.offset(lookDirection);
      BlockState tryClickBlockState = MeteorClient.mc.world.getBlockState(tryClickBlockPos);
      if (!canPlaceAgainst(tryClickBlockState, tryClickBlockPos)) {
         return false;
      }

      if (schematicState.getBlock() instanceof SlabBlock slabBlock) {
         SlabType slabType = (SlabType)schematicState.get(SlabBlock.TYPE);
         if (lookDirection == Direction.DOWN && slabType == SlabType.TOP) {
            return false;
         }

         if (lookDirection == Direction.UP && slabType == SlabType.BOTTOM) {
            return false;
         }
      }

      Vec3d target = raycastToFace(schematicState, tryClickBlockPos, lookDirection, printingRange);
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
      if (state.contains(Properties.FACING)) {
         return (Direction)state.get(Properties.FACING);
      } else if (state.contains(Properties.HORIZONTAL_FACING)) {
         return (Direction)state.get(Properties.HORIZONTAL_FACING);
      } else if (state.contains(Properties.AXIS)) {
         Axis axis = (Axis)state.get(Properties.AXIS);
         return Direction.from(axis, AxisDirection.POSITIVE);
      } else {
         return null;
      }
   }

   private static boolean isSlabFaceVisible(BlockPos schematicPos, Direction direction, BlockState slabState) {
      SlabType slabType = (SlabType)slabState.get(SlabBlock.TYPE);
      if (slabType == SlabType.DOUBLE) {
         return false;
      }

      Box collisionBox = slabState.getCollisionShape(MeteorClient.mc.world, schematicPos).getBoundingBox();
      double minY = slabType == SlabType.BOTTOM ? 0.0 : 0.5;
      double maxY = slabType == SlabType.TOP ? 1.0 : 0.5;
      Box adjustedBox = new Box(collisionBox.minX, minY, collisionBox.minZ, collisionBox.maxX, maxY, collisionBox.maxZ);
      return raycastToAdjustedBox(schematicPos, direction, adjustedBox);
   }

   private static boolean isTrapdoorFaceVisible(Direction dir, BlockState trapdoorState) {
      Direction facing = (Direction)trapdoorState.get(TrapdoorBlock.FACING);
      Boolean open = (Boolean)trapdoorState.get(TrapdoorBlock.OPEN);
      return !open && dir == facing;
   }

   private static Vec3d raycastToFace(BlockState schematicState, BlockPos adjacentPos, Direction direction, double printingRange) {
      Direction opposite = direction.getOpposite();
      Vec3d faceCenter = getFaceCenter(adjacentPos, opposite);
      BlockState adjacentBlockState = MeteorClient.mc.world.getBlockState(adjacentPos);
      faceCenter = fixSlabBlock(schematicState, adjacentBlockState, faceCenter);
      Vec3d eyePos = MeteorClient.mc.player.getCameraPosVec(1.0F);
      Vec3d rayVec = faceCenter.subtract(eyePos);
      double maxDistance = Math.min(eyePos.distanceTo(faceCenter) + 0.5, printingRange);
      Vec3d end = eyePos.add(rayVec.normalize().multiply(maxDistance));
      log.info(
         " eyePos : {} {} {}, faceCenter : {} {} {}, end : {} {} {}",
         new Object[]{
            eyePos.getX(),
            eyePos.getY(),
            eyePos.getZ(),
            faceCenter.getX(),
            faceCenter.getY(),
            faceCenter.getZ(),
            end.getX(),
            end.getY(),
            end.getZ()
         }
      );
      RaycastContext context = new RaycastContext(eyePos, end, ShapeType.COLLIDER, FluidHandling.NONE, MeteorClient.mc.player);
      BlockHitResult hit = MeteorClient.mc.world.raycast(context);
      BlockPos hitBlockPos = hit.getBlockPos();
      log.info("hit pos, {},{},{},{}", new Object[]{hitBlockPos.getX(), hitBlockPos.getY(), hitBlockPos.getZ(), hit.getSide()});
      boolean ok = hit.getType() == Type.BLOCK && hit.getBlockPos().equals(adjacentPos) && hit.getSide() == opposite;
      return ok ? faceCenter : null;
   }

   private static Vec3d fixSlabBlock(BlockState schematicBlockState, BlockState adjacentBlockState, Vec3d faceCenter) {
      Block schematicBlock = schematicBlockState.getBlock();
      if (!(schematicBlock instanceof SlabBlock)) {
         return faceCenter;
      }

      Block adjacentBlock = adjacentBlockState.getBlock();
      if (!(adjacentBlock instanceof SlabBlock)) {
         SlabType schematicBlockSlabType = (SlabType)schematicBlockState.get(SlabBlock.TYPE);
         if (schematicBlockSlabType == SlabType.TOP) {
            return new Vec3d(faceCenter.getX(), faceCenter.getY() + 0.25, faceCenter.getZ());
         }

         if (schematicBlockSlabType == SlabType.BOTTOM) {
            return new Vec3d(faceCenter.getX(), faceCenter.getY() - 0.25, faceCenter.getZ());
         }
      }

      return faceCenter;
   }

   private static Vec3d getFaceCenter(BlockPos pos, Direction direction) {
      Box box = MeteorClient.mc.world.getBlockState(pos).getOutlineShape(MeteorClient.mc.world, pos).getBoundingBox();
      double x = pos.getX() + box.minX + (box.maxX - box.minX) * 0.5;
      double y = pos.getY() + box.minY + (box.maxY - box.minY) * 0.5;
      double z = pos.getZ() + box.minZ + (box.maxZ - box.minZ) * 0.5;
      return new Vec3d(
         x + direction.getOffsetX() * (box.maxX - box.minX) * 0.5,
         y + direction.getOffsetY() * (box.maxY - box.minY) * 0.5,
         z + direction.getOffsetZ() * (box.maxZ - box.minZ) * 0.5
      );
   }

   private static Direction getDirectionFromPlayerView(BlockPos targetPos) {
      Vec3d eyePos = new Vec3d(
         MeteorClient.mc.player.getX(), MeteorClient.mc.player.getEyeY(), MeteorClient.mc.player.getZ()
      );
      Vec3d targetCenter = Vec3d.ofCenter(targetPos);
      Vec3d lookVec = targetCenter.subtract(eyePos).normalize();
      return Direction.getFacing(lookVec.x, lookVec.y, lookVec.z);
   }

   private static boolean canPlaceAgainst(BlockState tryClickBlockState, BlockPos tryClickPos) {
      if (!tryClickBlockState.isAir() && !tryClickBlockState.isLiquid()) {
         VoxelShape shape = tryClickBlockState.getCollisionShape(MeteorClient.mc.world, tryClickPos);
         if (shape.isEmpty()) {
            return false;
         }

         Block block = tryClickBlockState.getBlock();
         return block != Blocks.GLASS && block != Blocks.ICE
            ? Block.isShapeFullCube(shape)
               || tryClickBlockState.getBlock() == Blocks.GLASS
               || tryClickBlockState.getBlock() instanceof StainedGlassBlock
               || tryClickBlockState.getBlock() instanceof StairsBlock
               || tryClickBlockState.getBlock() instanceof SlabBlock
            : false;
      } else {
         return false;
      }
   }

   private static boolean raycastToAdjustedBox(BlockPos schematicPos, Direction direction, Box adjustedBox) {
      Direction opposite = direction.getOpposite();
      Vec3d eyePos = MeteorClient.mc.player.getCameraPosVec(1.0F);
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
               Vec3d point = new Vec3d(schematicPos.getX() + x, schematicPos.getY() + y, schematicPos.getZ() + z);
               BlockHitResult hit = MeteorClient.mc
                  .world
                  .raycast(new RaycastContext(eyePos, point, ShapeType.COLLIDER, FluidHandling.NONE, MeteorClient.mc.player));
               if (hit.getType() == Type.BLOCK && hit.getBlockPos().equals(schematicPos) && hit.getSide() == opposite) {
                  log.info("raycastToAdjustedBox return false");
                  return true;
               }
            }
         }
      }

      log.info("raycastToAdjustedBox return false");
      return false;
   }

   private record PlacementContext(BlockPos pos, Direction side, Vec3d hitPos) {
   }
}
