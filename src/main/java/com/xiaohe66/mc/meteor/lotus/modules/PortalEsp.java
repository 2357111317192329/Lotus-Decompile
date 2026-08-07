package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class PortalEsp extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> radius = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("radius")).description("检测半径")).defaultValue(32)).min(16).max(128).build());
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("渲染模式"))
                  .description("方块的渲染方式"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> frameColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("框架颜色"))
               .description("未激活传送门的颜色"))
            .defaultValue(new SettingColor(0, 150, 255, 50))
            .build()
      );
   private final Setting<SettingColor> portalColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("传送门颜色"))
               .description("已激活传送门的颜色"))
            .defaultValue(new SettingColor(255, 0, 255, 50))
            .build()
      );
   private final Setting<Boolean> obsidianFloor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("忽略遗迹"))
                  .description("忽略下界传送门遗迹"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreRelic = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("忽略遗迹"))
                  .description("忽略下界传送门遗迹"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreLake = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("忽略岩浆湖"))
                  .description("忽略岩浆湖"))
               .defaultValue(true))
            .build()
      );
   private final List<PortalEsp.PortalFrame> portals = new ArrayList<>();

   public PortalEsp() {
      super(Const.CATEGORY, "!下界门透视", "(计划中的功能, 暂不可用)自动搜索可疑的下界门(激活的、未激活的、破损的), 忽略自然生成的遗迹");
   }

   public void onActivate() {
      this.portals.clear();
   }

   @EventHandler
   private void onTick(Post event) {
   }

   private void checkPortalFrame(BlockPos startPos) {
      for (Axis axis : new Axis[]{Axis.X, Axis.Z}) {
         if (this.isValidPortalFrame(startPos, axis)) {
            this.portals.add(new PortalEsp.PortalFrame(startPos, false));
            return;
         }
      }
   }

   private boolean isValidPortalFrame(BlockPos center, Axis axis) {
      int maxSize = 23;
      int width = this.findFrameLength(center, Direction.EAST, axis, maxSize) + this.findFrameLength(center, Direction.WEST, axis, maxSize) + 1;
      int height = this.findFrameLength(center, Direction.NORTH, axis, maxSize)
         + this.findFrameLength(center, Direction.SOUTH, axis, maxSize)
         + 1;
      return width >= 4 && height >= 5 || width >= 5 && height >= 4;
   }

   private int findFrameLength(BlockPos start, Direction direction, Axis axis, int max) {
      int length = 0;
      MutableBlockPos pos = start.mutable();

      while (length < max) {
         pos.move(direction);
         BlockState state = this.mc.level.getBlockState(pos);
         length++;
      }

      return length;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
   }

   private static class PortalFrame {
      public final BlockPos pos;
      public final boolean isActive;

      public PortalFrame(BlockPos pos, boolean isActive) {
         this.pos = pos;
         this.isActive = isActive;
      }
   }
}
