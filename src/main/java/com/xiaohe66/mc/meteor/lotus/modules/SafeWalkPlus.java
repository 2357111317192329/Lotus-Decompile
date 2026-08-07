package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.KeyboardInputTickEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
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
      Input playerInput = event.getPlayerInput();
      if (!playerInput.shift() && !playerInput.jump()) {
         boolean needSneak = this.isOnEdge();
         Input newPlayerInput = new Input(
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
      AABB playerBB = this.mc.player.getBoundingBox();
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
      AABB supportCheck = new AABB(x - 0.05, y - 0.001, z - 0.05, x + 0.05, y, z + 0.05);
      return this.hasSolidSupport(supportCheck);
   }

   private boolean hasSolidSupport(AABB area) {
      for (BlockPos pos : this.getBlocksInBox(area)) {
         BlockState state = this.mc.level.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(this.mc.level, pos);
         if (!shape.isEmpty() && shape.toAabbs().stream().anyMatch(box -> box.move(pos).intersects(area))) {
            return true;
         }
      }

      return false;
   }

   private Iterable<BlockPos> getBlocksInBox(AABB box) {
      int minX = Mth.floor(box.minX);
      int minY = Mth.floor(box.minY);
      int minZ = Mth.floor(box.minZ);
      int maxX = Mth.floor(box.maxX);
      int maxY = Mth.floor(box.maxY);
      int maxZ = Mth.floor(box.maxZ);
      return BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ);
   }
}
