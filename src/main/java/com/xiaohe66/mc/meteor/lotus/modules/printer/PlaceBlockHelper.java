package com.xiaohe66.mc.meteor.lotus.modules.printer;

import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceBlockHelper {
   private static final Logger log = LoggerFactory.getLogger(PlaceBlockHelper.class);
   private final boolean debug;
   private final BlockPos schematicBlockPos;
   private final BlockState schematicBlockState;
   private final Block schematicBlock;
   private final double printingRange;
   private final boolean rotate;
   private boolean swingHand;
   private boolean airPlace;
   private Direction schematicBlockOffsetDir;
   private Direction blockFacing;
   private BlockPos tryClickBlockPos;
   private BlockState tryClickBlockState;
   private Block tryClickBlock;
   private Vec3d hitPos;

   public PlaceBlockHelper(boolean debug, BlockPos schematicBlockPos, BlockState schematicBlockState, double printingRange, boolean rotate) {
      this.debug = debug;
      this.schematicBlockPos = schematicBlockPos;
      this.schematicBlockState = schematicBlockState;
      this.printingRange = printingRange;
      this.rotate = rotate;
      this.schematicBlock = schematicBlockState.getBlock();
   }

   public Direction getCanPlaceDirection() {
      this.blockFacing = HeBlockUtils.getBlockFacingDirection(this.schematicBlockState);
      if (this.airPlace) {
         Direction direction = this.blockFacing != null ? this.blockFacing : Direction.UP;
         this.setTmpVariable(direction);
         return direction;
      }

      if (this.blockFacing != null) {
         return this.getCanPlaceDirectionHaveFacing(this.blockFacing);
      }

      for (Direction offsetDir : Direction.values()) {
         if (this.canViewDirection(offsetDir)) {
            return offsetDir;
         }
      }

      return null;
   }

   private Direction getCanPlaceDirectionHaveFacing(Direction blockFacing) {
      if (this.schematicBlock instanceof StairsBlock stairsBlock) {
         return this.getCanPlaceDirectionStairsBlock(blockFacing);
      } else {
         Direction lookDirection = blockFacing.getOpposite();
         return this.canViewDirection(lookDirection) ? lookDirection : null;
      }
   }

   private Direction getCanPlaceDirectionStairsBlock(Direction blockFacing) {
      BlockHalf blockHalf = (BlockHalf)this.schematicBlockState.get(StairsBlock.HALF);
      if (blockHalf == BlockHalf.BOTTOM) {
         this.printLog("handel bottom StairsBlock : {}", blockFacing);
         if (this.canViewDirection(Direction.DOWN)) {
            return Direction.DOWN;
         }

         if (this.canViewDirection(blockFacing)) {
            return blockFacing;
         }

         Direction direction = blockFacing.rotateYClockwise();
         if (this.canViewDirection(direction)) {
            return direction;
         }

         direction = blockFacing.rotateYCounterclockwise();
         if (this.canViewDirection(direction)) {
            return direction;
         }
      }

      return null;
   }

   private boolean canViewDirection(Direction offsetDir) {
      this.setTmpVariable(offsetDir);
      if (this.canNotClick()) {
         return false;
      }

      if (this.schematicBlock instanceof SlabBlock slabBlock) {
         SlabType slabType = (SlabType)this.schematicBlockState.get(SlabBlock.TYPE);
         if (offsetDir == Direction.DOWN && slabType == SlabType.TOP) {
            return false;
         }

         if (offsetDir == Direction.UP && slabType == SlabType.BOTTOM) {
            return false;
         }
      } else if (this.schematicBlock instanceof TrapdoorBlock) {
      }

      this.hitPos = this.rayToFace();
      this.printLog(
         "raycastToFace : {}, {}, {} {} {} -> {} {} {} : {}",
         this.hitPos != null,
         this.schematicBlockOffsetDir.name(),
         this.schematicBlockPos.getX(),
         this.schematicBlockPos.getY(),
         this.schematicBlockPos.getZ(),
         this.tryClickBlockPos.getX(),
         this.tryClickBlockPos.getY(),
         this.tryClickBlockPos.getZ(),
         this.schematicBlock.getName()
      );
      if (this.hitPos == null) {
         return false;
      } else if (this.schematicBlock instanceof StairsBlock) {
         boolean ret = this.isSameHorizontalDirection(MeteorClient.mc.player.getEyePos(), this.hitPos, this.blockFacing);
         this.printLog("StairsBlock result : {}, offsetDir : {}", ret, this.blockFacing);
         return ret;
      } else {
         return true;
      }
   }

   private void setTmpVariable(Direction offsetDir) {
      this.schematicBlockOffsetDir = offsetDir;
      this.tryClickBlockPos = this.schematicBlockPos.offset(offsetDir);
      this.tryClickBlockState = MeteorClient.mc.world.getBlockState(this.tryClickBlockPos);
      this.tryClickBlock = this.tryClickBlockState.getBlock();
   }

   public boolean isSameHorizontalDirection(Vec3d eyePos, Vec3d hitPos, Direction offsetDir) {
      Vec3d directionVector = hitPos.subtract(eyePos);
      double horizontalX = directionVector.x;
      double horizontalZ = directionVector.z;
      double length = Math.sqrt(horizontalX * horizontalX + horizontalZ * horizontalZ);
      if (length < 1.0E-5) {
         return false;
      }

      double normalizedX = horizontalX / length;
      double normalizedZ = horizontalZ / length;
      Vec3d stairVector = Vec3d.of(offsetDir.getVector());
      double stairX = stairVector.x;
      double stairZ = stairVector.z;
      double dotProduct = normalizedX * stairX + normalizedZ * stairZ;
      this.printLog("dotProduct : {}", dotProduct);
      return dotProduct > 0.707;
   }

   private boolean canNotClick() {
      boolean canClick = this.canClick();
      this.printLog(
         "canClick : {}, {} {} {}", canClick, this.tryClickBlockPos.getX(), this.tryClickBlockPos.getY(), this.tryClickBlockPos.getZ()
      );
      return !canClick;
   }

   private boolean canClick() {
      if (!this.tryClickBlockState.isAir() && !this.tryClickBlockState.isLiquid()) {
         VoxelShape shape = this.tryClickBlockState.getCollisionShape(MeteorClient.mc.world, this.tryClickBlockPos);
         if (shape.isEmpty()) {
            this.printLog("CollisionShape empty");
            return false;
         } else {
            return Block.isShapeFullCube(shape)
               || this.tryClickBlock == Blocks.GLASS
               || this.tryClickBlock instanceof StainedGlassBlock
               || this.tryClickBlock instanceof StairsBlock
               || this.tryClickBlock instanceof SlabBlock;
         }
      } else {
         this.printLog("canClick liquid");
         return false;
      }
   }

   private Vec3d rayToFace() {
      Direction hitDir = this.schematicBlockOffsetDir.getOpposite();
      Vec3d hitCenter = HeBlockUtils.getFaceCenter(this.tryClickBlockPos, hitDir);
      hitCenter = this.tryFixHitCenter(hitCenter);
      Vec3d eyePos = MeteorClient.mc.player.getCameraPosVec(1.0F);
      Vec3d rayVec = hitCenter.subtract(eyePos);
      double maxDistance = Math.min(eyePos.distanceTo(hitCenter) + 0.5, this.printingRange);
      Vec3d end = eyePos.add(rayVec.normalize().multiply(maxDistance));
      RaycastContext context = new RaycastContext(eyePos, end, ShapeType.COLLIDER, FluidHandling.NONE, MeteorClient.mc.player);
      BlockHitResult hitResult = MeteorClient.mc.world.raycast(context);
      BlockPos hitBlockPos = hitResult.getBlockPos();
      this.printLog("hitBlockPos, {},{},{},{}", hitBlockPos.getX(), hitBlockPos.getY(), hitBlockPos.getZ(), hitResult.getSide());
      boolean ok = hitResult.getType() == Type.BLOCK
         && hitResult.getBlockPos().equals(this.tryClickBlockPos)
         && hitResult.getSide() == hitDir;
      return ok ? hitCenter : null;
   }

   private Vec3d tryFixHitCenter(Vec3d hitCenter) {
      if (this.schematicBlock instanceof SlabBlock) {
         if (this.tryClickBlock instanceof SlabBlock) {
            return hitCenter;
         } else {
            SlabType schematicBlockSlabType = (SlabType)this.schematicBlockState.get(SlabBlock.TYPE);
            if (schematicBlockSlabType == SlabType.TOP) {
               return new Vec3d(hitCenter.getX(), hitCenter.getY() + 0.25, hitCenter.getZ());
            } else {
               return schematicBlockSlabType == SlabType.BOTTOM
                  ? new Vec3d(hitCenter.getX(), hitCenter.getY() - 0.25, hitCenter.getZ())
                  : hitCenter;
            }
         }
      } else {
         if (this.schematicBlock instanceof StairsBlock) {
         }

         return hitCenter;
      }
   }

   public boolean tryPlace() {
      this.printLog("tryPlace : {} {} {}", this.schematicBlockPos.getX(), this.schematicBlockPos.getY(), this.schematicBlockPos.getZ());
      if (MeteorClient.mc.player != null && MeteorClient.mc.world != null) {
         BlockState worldBlockState = MeteorClient.mc.world.getBlockState(this.schematicBlockPos);
         if (!worldBlockState.isReplaceable()) {
            this.printLog("tryPlace fail, isReplaceable false : {}", this.schematicBlockState);
            return false;
         } else if (!this.airPlace && !this.canViewDirection(this.schematicBlockOffsetDir)) {
            this.printLog("tryPlace fail, canViewDirection false : {}", this.schematicBlockOffsetDir);
            return false;
         } else {
            return this.place();
         }
      } else {
         return false;
      }
   }

   private boolean place() {
      this.printLog("place : {}", this.schematicBlockPos);
      if (this.airPlace) {
         this.placeAir(this.schematicBlockPos);
         return true;
      }

      if (this.canNotClick()) {
         return false;
      }

      Direction opposite = this.schematicBlockOffsetDir.getOpposite();
      if (this.rotate) {
         this.printLog("place hitPos, {} {} {}", this.hitPos.getX(), this.hitPos.getY(), this.hitPos.getZ());
         double yaw = Rotations.getYaw(this.hitPos);
         double pitch = Rotations.getPitch(this.hitPos);
         this.printLog("place rotate, yaw : {}, pitch : {}", yaw, pitch);
         Rotations.rotate(yaw, pitch, 50, true, () -> {
            BlockHitResult blockHitResult = new BlockHitResult(this.hitPos, opposite, this.tryClickBlockPos, false);
            this.place(blockHitResult, true);
         });
      } else {
         Vec3d hitPos = new Vec3d(
            this.schematicBlockPos.getX() + 0.5, this.schematicBlockPos.getY() + 0.5, this.schematicBlockPos.getZ() + 0.5
         );
         this.place(new BlockHitResult(hitPos, opposite, this.tryClickBlockPos, false), this.swingHand);
      }

      return true;
   }

   private void place(BlockHitResult blockHitResult, boolean swing) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.interactionManager != null && MeteorClient.mc.getNetworkHandler() != null) {
         ActionResult result = MeteorClient.mc.interactionManager.interactBlock(MeteorClient.mc.player, Hand.MAIN_HAND, blockHitResult);
         if (result instanceof Success) {
            if (swing) {
               MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
            } else {
               MeteorClient.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
         }
      }
   }

   private void placeAir(BlockPos blockPos) {
      BlockHitResult blockHitResult = new BlockHitResult(Vec3d.ofCenter(blockPos), Direction.UP, blockPos, false);
      MeteorClient.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      MeteorClient.mc
         .player
         .networkHandler
         .sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, blockHitResult, MeteorClient.mc.player.currentScreenHandler.getRevision() + 2));
      MeteorClient.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
   }

   private void printLog(String str, Object arg) {
      if (this.debug) {
         log.info(str, arg);
      }
   }

   private void printLog(String str, Object... args) {
      if (this.debug) {
         log.info(str, args);
      }
   }

   public BlockPos getSchematicBlockPos() {
      return this.schematicBlockPos;
   }

   public BlockState getSchematicBlockState() {
      return this.schematicBlockState;
   }

   public Block getSchematicBlock() {
      return this.schematicBlock;
   }

   public void setAirPlace(boolean airPlace) {
      this.airPlace = airPlace;
   }
}
