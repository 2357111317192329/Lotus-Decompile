package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.KeyboardInputTickEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeWalkPlus extends Module {
   private static final Logger log = LoggerFactory.getLogger(SafeWalkPlus.class);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Double> distance = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("边缘距离")).description("站住的地面剩下多少时下蹲")).min(0.1).sliderMax(1.0).defaultValue(0.2).build());

   public SafeWalkPlus() {
      super(Const.CATEGORY, "安全行走+", "自动在边缘下蹲");
   }

   @EventHandler(priority = -100)
   public void onKeyboardInputTickEvent(KeyboardInputTickEvent event) {
      PlayerInput playerInput = event.getPlayerInput();
      if (!playerInput.sneak() && !playerInput.jump()) {
         boolean needSneak = this.isOnEdge();
         PlayerInput newPlayerInput = new PlayerInput(
            playerInput.forward(),
            playerInput.backward(),
            playerInput.left(),
            playerInput.right(),
            playerInput.jump(),
            needSneak,
            playerInput.sprint()
         );
         event.setPlayerInput(newPlayerInput);
      }
   }

   private boolean isOnEdge() {
      Box playerBB = this.mc.player.getBoundingBox();
      Double distanceValue = (Double)this.distance.get();
      double minX = playerBB.minX + distanceValue;
      double maxX = playerBB.maxX - distanceValue;
      double minZ = playerBB.minZ + distanceValue;
      double maxZ = playerBB.maxZ - distanceValue;
      if (!this.isCornerSupported(minX, playerBB.minY, minZ)) {
         return true;
      } else if (!this.isCornerSupported(minX, playerBB.minY, maxZ)) {
         return true;
      } else {
         return !this.isCornerSupported(maxX, playerBB.minY, minZ) ? true : !this.isCornerSupported(maxX, playerBB.minY, maxZ);
      }
   }

   private boolean isCornerSupported(double x, double y, double z) {
      Box supportCheck = new Box(x - 0.05, y - 0.001, z - 0.05, x + 0.05, y, z + 0.05);
      return this.hasSolidSupport(supportCheck);
   }

   private boolean hasSolidSupport(Box area) {
      for (BlockPos pos : this.getBlocksInBox(area)) {
         BlockState state = this.mc.world.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(this.mc.world, pos);
         if (!shape.isEmpty() && shape.getBoundingBoxes().stream().anyMatch(box -> box.offset(pos).intersects(area))) {
            return true;
         }
      }

      return false;
   }

   private Iterable<BlockPos> getBlocksInBox(Box box) {
      int minX = MathHelper.floor(box.minX);
      int minY = MathHelper.floor(box.minY);
      int minZ = MathHelper.floor(box.minZ);
      int maxX = MathHelper.floor(box.maxX);
      int maxY = MathHelper.floor(box.maxY);
      int maxZ = MathHelper.floor(box.maxZ);
      return BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ);
   }
}
