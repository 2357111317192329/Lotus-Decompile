package com.xiaohe66.mc.meteor.lotus.modules.printer;

import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.Success;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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
   private Vec3 hitPos;

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
      if (this.schematicBlock instanceof StairBlock stairsBlock) {
         return this.getCanPlaceDirectionStairsBlock(blockFacing);
      } else {
         Direction lookDirection = blockFacing.getOpposite();
         return this.canViewDirection(lookDirection) ? lookDirection : null;
      }
   }

   private Direction getCanPlaceDirectionStairsBlock(Direction blockFacing) {
      Half blockHalf = (Half)this.schematicBlockState.getValue(StairBlock.HALF);
      if (blockHalf == Half.BOTTOM) {
         this.printLog("handel bottom StairsBlock : {}", blockFacing);
         if (this.canViewDirection(Direction.DOWN)) {
            return Direction.DOWN;
         }

         if (this.canViewDirection(blockFacing)) {
            return blockFacing;
         }

         Direction direction = blockFacing.getClockWise();
         if (this.canViewDirection(direction)) {
            return direction;
         }

         direction = blockFacing.getCounterClockWise();
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
         SlabType slabType = (SlabType)this.schematicBlockState.getValue(SlabBlock.TYPE);
         if (offsetDir == Direction.DOWN && slabType == SlabType.TOP) {
            return false;
         }

         if (offsetDir == Direction.UP && slabType == SlabType.BOTTOM) {
            return false;
         }
      } else if (this.schematicBlock instanceof TrapDoorBlock) {
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
      } else if (this.schematicBlock instanceof StairBlock) {
         boolean ret = this.isSameHorizontalDirection(MeteorClient.mc.player.getEyePosition(), this.hitPos, this.blockFacing);
         this.printLog("StairsBlock result : {}, offsetDir : {}", ret, this.blockFacing);
         return ret;
      } else {
         return true;
      }
   }

   private void setTmpVariable(Direction offsetDir) {
      this.schematicBlockOffsetDir = offsetDir;
      this.tryClickBlockPos = this.schematicBlockPos.relative(offsetDir);
      this.tryClickBlockState = MeteorClient.mc.level.getBlockState(this.tryClickBlockPos);
      this.tryClickBlock = this.tryClickBlockState.getBlock();
   }

   public boolean isSameHorizontalDirection(Vec3 eyePos, Vec3 hitPos, Direction offsetDir) {
      Vec3 directionVector = hitPos.subtract(eyePos);
      double horizontalX = directionVector.x;
      double horizontalZ = directionVector.z;
      double length = Math.sqrt(horizontalX * horizontalX + horizontalZ * horizontalZ);
      if (length < 1.0E-5) {
         return false;
      }

      double normalizedX = horizontalX / length;
      double normalizedZ = horizontalZ / length;
      Vec3 stairVector = Vec3.atLowerCornerOf(offsetDir.getUnitVec3i());
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
      if (!this.tryClickBlockState.isAir() && !this.tryClickBlockState.liquid()) {
         VoxelShape shape = this.tryClickBlockState.getCollisionShape(MeteorClient.mc.level, this.tryClickBlockPos);
         if (shape.isEmpty()) {
            this.printLog("CollisionShape empty");
            return false;
         } else {
            return Block.isShapeFullBlock(shape)
               || this.tryClickBlock == Blocks.GLASS
               || this.tryClickBlock instanceof StainedGlassBlock
               || this.tryClickBlock instanceof StairBlock
               || this.tryClickBlock instanceof SlabBlock;
         }
      } else {
         this.printLog("canClick liquid");
         return false;
      }
   }

   private Vec3 rayToFace() {
      Direction hitDir = this.schematicBlockOffsetDir.getOpposite();
      Vec3 hitCenter = HeBlockUtils.getFaceCenter(this.tryClickBlockPos, hitDir);
      hitCenter = this.tryFixHitCenter(hitCenter);
      Vec3 eyePos = MeteorClient.mc.player.getEyePosition(1.0F);
      Vec3 rayVec = hitCenter.subtract(eyePos);
      double maxDistance = Math.min(eyePos.distanceTo(hitCenter) + 0.5, this.printingRange);
      Vec3 end = eyePos.add(rayVec.normalize().scale(maxDistance));
      ClipContext context = new ClipContext(eyePos, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, MeteorClient.mc.player);
      BlockHitResult hitResult = MeteorClient.mc.level.clip(context);
      BlockPos hitBlockPos = hitResult.getBlockPos();
      this.printLog("hitBlockPos, {},{},{},{}", hitBlockPos.getX(), hitBlockPos.getY(), hitBlockPos.getZ(), hitResult.getDirection());
      boolean ok = hitResult.getType() == Type.BLOCK
         && hitResult.getBlockPos().equals(this.tryClickBlockPos)
         && hitResult.getDirection() == hitDir;
      return ok ? hitCenter : null;
   }

   private Vec3 tryFixHitCenter(Vec3 hitCenter) {
      if (this.schematicBlock instanceof SlabBlock) {
         if (this.tryClickBlock instanceof SlabBlock) {
            return hitCenter;
         } else {
            SlabType schematicBlockSlabType = (SlabType)this.schematicBlockState.getValue(SlabBlock.TYPE);
            if (schematicBlockSlabType == SlabType.TOP) {
               return new Vec3(hitCenter.x(), hitCenter.y() + 0.25, hitCenter.z());
            } else {
               return schematicBlockSlabType == SlabType.BOTTOM
                  ? new Vec3(hitCenter.x(), hitCenter.y() - 0.25, hitCenter.z())
                  : hitCenter;
            }
         }
      } else {
         if (this.schematicBlock instanceof StairBlock) {
         }

         return hitCenter;
      }
   }

   public boolean tryPlace() {
      this.printLog("tryPlace : {} {} {}", this.schematicBlockPos.getX(), this.schematicBlockPos.getY(), this.schematicBlockPos.getZ());
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         BlockState worldBlockState = MeteorClient.mc.level.getBlockState(this.schematicBlockPos);
         if (!worldBlockState.canBeReplaced()) {
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
         this.printLog("place hitPos, {} {} {}", this.hitPos.x(), this.hitPos.y(), this.hitPos.z());
         double yaw = Rotations.getYaw(this.hitPos);
         double pitch = Rotations.getPitch(this.hitPos);
         this.printLog("place rotate, yaw : {}, pitch : {}", yaw, pitch);
         Rotations.rotate(yaw, pitch, 50, true, () -> {
            BlockHitResult blockHitResult = new BlockHitResult(this.hitPos, opposite, this.tryClickBlockPos, false);
            this.place(blockHitResult, true);
         });
      } else {
         Vec3 hitPos = new Vec3(
            this.schematicBlockPos.getX() + 0.5, this.schematicBlockPos.getY() + 0.5, this.schematicBlockPos.getZ() + 0.5
         );
         this.place(new BlockHitResult(hitPos, opposite, this.tryClickBlockPos, false), this.swingHand);
      }

      return true;
   }

   private void place(BlockHitResult blockHitResult, boolean swing) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null && MeteorClient.mc.getConnection() != null) {
         InteractionResult result = MeteorClient.mc.gameMode.useItemOn(MeteorClient.mc.player, InteractionHand.MAIN_HAND, blockHitResult);
         if (result instanceof Success) {
            if (swing) {
               MeteorClient.mc.player.swing(InteractionHand.MAIN_HAND);
            } else {
               MeteorClient.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
         }
      }
   }

   private void placeAir(BlockPos blockPos) {
      BlockHitResult blockHitResult = new BlockHitResult(Vec3.atCenterOf(blockPos), Direction.UP, blockPos, false);
      MeteorClient.mc.player.connection.send(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
      MeteorClient.mc
         .player
         .connection
         .send(new ServerboundUseItemOnPacket(InteractionHand.OFF_HAND, blockHitResult, MeteorClient.mc.player.containerMenu.getStateId() + 2));
      MeteorClient.mc.player.connection.send(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
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
