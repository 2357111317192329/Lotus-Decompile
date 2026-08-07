package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils.MobSpawn;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.LightType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos.Mutable;

public class RedstoneAssist extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup lightOverlayGroup = this.settings.createGroup("亮度显示");
   private final BoolSetting lightOverlay = (BoolSetting)this.lightOverlayGroup
      .add(((Builder)((Builder)((Builder)new Builder().name("亮度显示")).description("渲染亮度数值")).defaultValue(true)).build());
   private final Setting<Integer> horizontalRange = this.lightOverlayGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("水平范围"))
                  .description("水平扫描范围（格）"))
               .defaultValue(16))
            .min(1)
            .sliderMax(64)
            .build()
      );
   private final Setting<Integer> verticalRange = this.lightOverlayGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("垂直范围"))
                  .description("垂直扫描范围（格）"))
               .defaultValue(12))
            .min(1)
            .sliderMax(64)
            .build()
      );
   private final Setting<Boolean> seeThroughBlocks = this.lightOverlayGroup
      .add(((Builder)((Builder)((Builder)new Builder().name("透视显示")).description("允许透过方块看到显示")).defaultValue(false)).build());
   private final Setting<Integer> spawnThreshold = this.lightOverlayGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("刷怪阈值"))
                  .description("低于此亮度可能刷怪（原版1.21为0, 旧版为7, 下界猪人为11）"))
               .defaultValue(0))
            .min(0)
            .sliderMax(15)
            .build()
      );
   private final Setting<Boolean> showBox = this.lightOverlayGroup
      .add(((Builder)((Builder)((Builder)new Builder().name("显示方框")).description("危险区域显示方框")).defaultValue(false)).build());
   private final Setting<Boolean> showNumber = this.lightOverlayGroup
      .add(((Builder)((Builder)((Builder)new Builder().name("显示数字")).description("显示亮度数值")).defaultValue(true)).build());
   private final Setting<Double> numberScale = this.lightOverlayGroup
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("数字大小"))
               .description("数字显示大小"))
            .defaultValue(0.1)
            .min(0.01)
            .sliderMax(0.2)
            .decimalPlaces(2)
            .build()
      );
   private final Setting<SettingColor> spawnColor = this.lightOverlayGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("刷怪颜色"))
               .description("可能刷怪时的显示颜色"))
            .defaultValue(new SettingColor(190, 0, 0))
            .build()
      );
   private final Setting<SettingColor> safeColor = this.lightOverlayGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("安全颜色"))
               .description("亮度安全时的显示颜色"))
            .defaultValue(new SettingColor(0, 190, 0))
            .build()
      );
   private final Pool<RedstoneAssist.Marker> markerPool = new Pool(RedstoneAssist.Marker::new);
   private final List<RedstoneAssist.Marker> markers = new ArrayList<>();
   private static final boolean[][] DIGIT_SEGMENTS = new boolean[][]{
      {true, true, true, false, true, true, true},
      {false, false, true, false, false, true, false},
      {true, false, true, true, true, false, true},
      {true, false, true, true, false, true, true},
      {false, true, true, true, false, true, false},
      {true, true, false, true, false, true, true},
      {true, true, false, true, true, true, true},
      {true, false, true, false, false, true, false},
      {true, true, true, true, true, true, true},
      {true, true, true, true, false, true, true}
   };
   private static final double[][][] SEGMENT_LINES = new double[10][7][4];
   private final double[] cornersCache = new double[8];

   public RedstoneAssist() {
      super(Const.CATEGORY, "生电辅助", "显示方块亮度等级和刷怪风险区域");
   }

   @EventHandler
   private void onTick(Pre event) {
      if ((Boolean)this.lightOverlay.get()) {
         this.markerPool.freeAll(this.markers);
         this.markers.clear();
         Vec3d center = this.getCameraPos();
         int px = (int)Math.floor(center.x);
         int py = (int)Math.floor(center.y);
         int pz = (int)Math.floor(center.z);
         int hRange = (Integer)this.horizontalRange.get();
         int vRange = (Integer)this.verticalRange.get();
         Mutable pos = new Mutable();

         for (int x = px - hRange; x <= px + hRange; x++) {
            for (int z = pz - hRange; z <= pz + hRange; z++) {
               for (int y = Math.max(this.mc.world.getBottomY(), py - vRange); y <= py + vRange && y <= this.mc.world.getHeight(); y++) {
                  pos.set(x, y, z);
                  BlockState blockState = this.mc.world.getBlockState(pos);
                  MobSpawn spawn = BlockUtils.isValidMobSpawn(pos, blockState, (Integer)this.spawnThreshold.get());
                  if (spawn == MobSpawn.Always || spawn == MobSpawn.Potential) {
                     int lightLevel = this.getLightLevel(pos);
                     this.markers.add(((RedstoneAssist.Marker)this.markerPool.get()).set(pos, false, lightLevel));
                  } else if (blockState.isAir()) {
                     BlockPos downPos = pos.down();
                     if (this.isValidSpawnBase(downPos)) {
                        int lightLevel = this.getLightLevel(pos);
                        this.markers.add(((RedstoneAssist.Marker)this.markerPool.get()).set(pos, true, lightLevel));
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (!this.markers.isEmpty()) {
         Renderer3D renderer = this.seeThroughBlocks.get() ? event.renderer : event.depthRenderer;
         Direction facing = this.getCameraFacing();

         for (RedstoneAssist.Marker marker : this.markers) {
            this.renderMarker(renderer, marker, facing);
         }
      }
   }

   private void renderMarker(Renderer3D renderer, RedstoneAssist.Marker marker, Direction facing) {
      Color color = marker.safe ? (Color)this.safeColor.get() : (Color)this.spawnColor.get();
      double yo = marker.y + 0.01;
      if ((Boolean)this.showBox.get() && !marker.safe) {
         this.renderBox(renderer, marker, color, yo);
      }

      if ((Boolean)this.showNumber.get()) {
         this.renderNumber(renderer, marker, color, yo, facing);
      }
   }

   private void renderBox(Renderer3D renderer, RedstoneAssist.Marker m, Color c, double yo) {
      renderer.line(m.x, yo, m.z, m.x + 1.0, yo, m.z, c);
      renderer.line(m.x + 1.0, yo, m.z, m.x + 1.0, yo, m.z + 1.0, c);
      renderer.line(m.x + 1.0, yo, m.z + 1.0, m.x, yo, m.z + 1.0, c);
      renderer.line(m.x, yo, m.z + 1.0, m.x, yo, m.z, c);
   }

   private void renderNumber(Renderer3D renderer, RedstoneAssist.Marker m, Color color, double yo, Direction facing) {
      double cx = m.x + 0.5;
      double cz = m.z + 0.5;
      double halfSize = (Double)this.numberScale.get();
      if (m.lightLevel >= 0 && m.lightLevel <= 9) {
         this.calculateCorners(cx, cz, halfSize, facing, this.cornersCache);
         this.renderDigit7Segment(renderer, color, this.cornersCache, yo, m.lightLevel);
      } else if (m.lightLevel >= 10 && m.lightLevel <= 15) {
         double spacing = 0.05;
         double digitHalfSize = halfSize * 0.9;
         double offset = digitHalfSize + spacing;
         double tensCx;
         double tensCz;
         double onesCx;
         double onesCz;
         switch (facing) {
            case NORTH:
               tensCx = cx - offset - 0.1;
               tensCz = cz;
               onesCx = cx + offset - 0.1;
               onesCz = cz;
               break;
            case SOUTH:
               tensCx = cx - offset + 0.1;
               tensCz = cz;
               onesCx = cx + offset + 0.1;
               onesCz = cz;
               break;
            case WEST:
               tensCx = cx;
               tensCz = cz - offset + 0.1;
               onesCx = cx;
               onesCz = cz + offset + 0.1;
               break;
            case EAST:
               tensCx = cx;
               tensCz = cz - offset - 0.1;
               onesCx = cx;
               onesCz = cz + offset - 0.1;
               break;
            default:
               tensCx = cx - offset;
               tensCz = cz;
               onesCx = cx + offset;
               onesCz = cz;
         }

         if (facing != Direction.NORTH && facing != Direction.EAST) {
            this.calculateCorners(onesCx, onesCz, digitHalfSize, facing, this.cornersCache);
            this.renderDigit7Segment(renderer, color, this.cornersCache, yo, m.lightLevel / 10);
            this.calculateCorners(tensCx, tensCz, digitHalfSize, facing, this.cornersCache);
            this.renderDigit7Segment(renderer, color, this.cornersCache, yo, m.lightLevel % 10);
         } else {
            this.calculateCorners(onesCx, onesCz, digitHalfSize, facing, this.cornersCache);
            this.renderDigit7Segment(renderer, color, this.cornersCache, yo, m.lightLevel % 10);
            this.calculateCorners(tensCx, tensCz, digitHalfSize, facing, this.cornersCache);
            this.renderDigit7Segment(renderer, color, this.cornersCache, yo, m.lightLevel / 10);
         }
      }
   }

   private void calculateCorners(double cx, double cz, double halfWidth, Direction facing, double[] out) {
      double halfHeight = halfWidth * 2.0;
      switch (facing) {
         case NORTH:
            out[0] = cx - halfWidth;
            out[1] = cz - halfHeight;
            out[2] = cx + halfWidth;
            out[3] = cz - halfHeight;
            out[4] = cx + halfWidth;
            out[5] = cz + halfHeight;
            out[6] = cx - halfWidth;
            out[7] = cz + halfHeight;
            break;
         case SOUTH:
            out[0] = cx + halfWidth;
            out[1] = cz + halfHeight;
            out[2] = cx - halfWidth;
            out[3] = cz + halfHeight;
            out[4] = cx - halfWidth;
            out[5] = cz - halfHeight;
            out[6] = cx + halfWidth;
            out[7] = cz - halfHeight;
            break;
         case WEST:
            out[0] = cx - halfHeight;
            out[1] = cz + halfWidth;
            out[2] = cx - halfHeight;
            out[3] = cz - halfWidth;
            out[4] = cx + halfHeight;
            out[5] = cz - halfWidth;
            out[6] = cx + halfHeight;
            out[7] = cz + halfWidth;
            break;
         case EAST:
            out[0] = cx + halfHeight;
            out[1] = cz - halfWidth;
            out[2] = cx + halfHeight;
            out[3] = cz + halfWidth;
            out[4] = cx - halfHeight;
            out[5] = cz + halfWidth;
            out[6] = cx - halfHeight;
            out[7] = cz - halfWidth;
            break;
         default:
            out[0] = cx - halfWidth;
            out[1] = cz - halfHeight;
            out[2] = cx + halfWidth;
            out[3] = cz - halfHeight;
            out[4] = cx + halfWidth;
            out[5] = cz + halfHeight;
            out[6] = cx - halfWidth;
            out[7] = cz + halfHeight;
      }
   }

   private void renderDigit7Segment(Renderer3D renderer, Color color, double[] corners, double y, int digit) {
      double tlx = corners[0];
      double tlz = corners[1];
      double trx = corners[2];
      double trz = corners[3];
      double blx = corners[6];
      double blz = corners[7];
      double hVecX = trx - tlx;
      double hVecZ = trz - tlz;
      double vVecX = (blx - tlx) * 0.5;
      double vVecZ = (blz - tlz) * 0.5;
      boolean[] segs = DIGIT_SEGMENTS[digit];
      double[][] lines = SEGMENT_LINES[digit];

      for (int i = 0; i < 7; i++) {
         if (segs[i]) {
            double[] line = lines[i];
            double x1 = tlx + line[0] * hVecX + line[1] * vVecX;
            double z1 = tlz + line[0] * hVecZ + line[1] * vVecZ;
            double x2 = tlx + line[2] * hVecX + line[3] * vVecX;
            double z2 = tlz + line[2] * hVecZ + line[3] * vVecZ;
            this.drawThickLine(renderer, color, x1, y, z1, x2, y, z2);
         }
      }
   }

   private void drawThickLine(Renderer3D renderer, Color color, double x1, double y1, double z1, double x2, double y2, double z2) {
      double dx = x2 - x1;
      double dz = z2 - z1;
      double length = Math.sqrt(dx * dx + dz * dz);
      if (!(length < 1.0E-4)) {
         double nx = dx / length;
         double nz = dz / length;
         double perpX = -nz * 0.025;
         double perpZ = nx * 0.025;
         double p1x = x1 + perpX;
         double p1z = z1 + perpZ;
         double p2x = x1 - perpX;
         double p2z = z1 - perpZ;
         double p3x = x2 - perpX;
         double p3z = z2 - perpZ;
         double p4x = x2 + perpX;
         double p4z = z2 + perpZ;
         renderer.quad(p1x, y1, p1z, p2x, y1, p2z, p3x, y2, p3z, p4x, y2, p4z, color);
      }
   }

   private int getLightLevel(BlockPos pos) {
      return this.mc.world.getLightLevel(LightType.BLOCK, pos);
   }

   private boolean isValidSpawnBase(BlockPos pos) {
      BlockState state = this.mc.world.getBlockState(pos);
      return state.isSolidBlock(this.mc.world, pos);
   }

   private Vec3d getCameraPos() {
      Freecam freecam = (Freecam)Modules.get().get(Freecam.class);
      return freecam != null && freecam.isActive() ? new Vec3d(freecam.pos.x, freecam.pos.y, freecam.pos.z) : this.mc.player.getEntityPos();
   }

   private Direction getCameraFacing() {
      Freecam freecam = (Freecam)Modules.get().get(Freecam.class);
      return freecam != null && freecam.isActive() ? Direction.fromHorizontalDegrees(freecam.yaw) : this.mc.player.getHorizontalFacing();
   }

   static {
      for (int digit = 0; digit <= 9; digit++) {
         boolean[] segs = DIGIT_SEGMENTS[digit];
         double[][] lines = SEGMENT_LINES[digit];
         if (segs[0]) {
            lines[0][0] = 0.0;
            lines[0][1] = 0.0;
            lines[0][2] = 1.0;
            lines[0][3] = 0.0;
         }

         if (segs[1]) {
            lines[1][0] = 0.0;
            lines[1][1] = 0.0;
            lines[1][2] = 0.0;
            lines[1][3] = 1.0;
         }

         if (segs[2]) {
            lines[2][0] = 1.0;
            lines[2][1] = 0.0;
            lines[2][2] = 1.0;
            lines[2][3] = 1.0;
         }

         if (segs[3]) {
            lines[3][0] = 0.0;
            lines[3][1] = 1.0;
            lines[3][2] = 1.0;
            lines[3][3] = 1.0;
         }

         if (segs[4]) {
            lines[4][0] = 0.0;
            lines[4][1] = 1.0;
            lines[4][2] = 0.0;
            lines[4][3] = 2.0;
         }

         if (segs[5]) {
            lines[5][0] = 1.0;
            lines[5][1] = 1.0;
            lines[5][2] = 1.0;
            lines[5][3] = 2.0;
         }

         if (segs[6]) {
            lines[6][0] = 0.0;
            lines[6][1] = 2.0;
            lines[6][2] = 1.0;
            lines[6][3] = 2.0;
         }
      }
   }

   public static class Marker {
      public double x;
      public double y;
      public double z;
      public boolean safe;
      public int lightLevel;

      public RedstoneAssist.Marker set(BlockPos blockPos, boolean safe, int lightLevel) {
         this.x = blockPos.getX();
         this.y = blockPos.getY();
         this.z = blockPos.getZ();
         this.safe = safe;
         this.lightLevel = lightLevel;
         return this;
      }
   }
}
