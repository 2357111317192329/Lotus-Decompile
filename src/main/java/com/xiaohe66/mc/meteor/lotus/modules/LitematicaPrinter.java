package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.KeyboardInputTickEvent;
import com.xiaohe66.mc.meteor.lotus.modules.printer.PlaceBlockHelper;
import com.xiaohe66.mc.meteor.lotus.modules.printer.SortAlgorithm;
import com.xiaohe66.mc.meteor.lotus.modules.printer.SortingSecond;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.PlayerInput;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LitematicaPrinter extends BaseModule {
   private static final Logger log = LoggerFactory.getLogger(LitematicaPrinter.class);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRendering = this.settings.createGroup("渲染");
   private final Setting<Double> printingRange = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("放置范围(格)")).description("放置范围(格)")).defaultValue(4.5).min(1.0).sliderMin(1.0).max(6.0).sliderMax(6.0).build());
   private final Setting<Double> yPrintingRange = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("放置高度(格)")).description("放置高度(格)")).defaultValue(1.0).min(1.0).sliderMax(6.0).build());
   private final Setting<Boolean> airPlace = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("空气放置-org"))
                  .description("空气放置, 副手绕过, 用于 2b2t.org"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> movePause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("移动暂停"))
                  .description("移动时暂停放置"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> returnHand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("复原物品栏"))
                     .description("复原物品栏"))
                  .defaultValue(false))
               .visible(() -> false))
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("旋转"))
                  .description("旋转到正在放置的块"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SortAlgorithm> firstAlgorithm = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("优先放置模式"))
                  .description("优先放置模式"))
               .defaultValue(SortAlgorithm.DownTop))
            .build()
      );
   private final Setting<SortingSecond> secondAlgorithm = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("第二放置模式"))
                     .description("第二放置模式"))
                  .defaultValue(SortingSecond.最远的))
               .visible(() -> ((SortAlgorithm)this.firstAlgorithm.get()).applySecondSorting))
            .build()
      );
   private final Setting<List<Block>> blacklist = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)new meteordevelopment.meteorclient.settings.BlockListSetting.Builder()
                  .name("黑名单"))
               .description("不允许放置的方块"))
            .build()
      );
   private final Setting<Boolean> renderBlocks = this.sgRendering
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("放置渲染"))
                     .description("放置渲染"))
                  .defaultValue(true))
               .visible(() -> false))
            .build()
      );
   private final Setting<Integer> fadeTime = this.sgRendering
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("渲染淡出时间(tick)"))
                     .description("渲染淡出时间"))
                  .defaultValue(5))
               .sliderRange(1, 20)
               .sliderMin(1)
               .visible(this.renderBlocks::get))
            .build()
      );
   private final Setting<SettingColor> colour = this.sgRendering
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("颜色"))
                  .description("颜色"))
               .defaultValue(new SettingColor(95, 190, 255))
               .visible(this.renderBlocks::get))
            .build()
      );
   private final Setting<Boolean> debug = this.sgRendering
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("调试模式"))
                  .description("调试模式"))
               .defaultValue(false))
            .build()
      );
   private int lastUsedSlot = -1;
   private boolean isMoving;
   private final List<PlaceBlockHelper> needPlaceBlockList = new ArrayList<>();
   private final List<Pair<Integer, BlockPos>> renderPosList = new ArrayList<>();

   public LitematicaPrinter() {
      super("投影打印", "grim可用，在不复杂场景下的使用, 适合建造刷怪塔、村民交易所等简单生电机器, 暂不支持楼梯、活板门等", 1);
   }

   public void onActivate() {
      this.isMoving = false;
   }

   @EventHandler(priority = 100)
   public void onKeyboardInputTickEvent(KeyboardInputTickEvent event) {
      PlayerInput playerInput = event.getPlayerInput();
      this.isMoving = playerInput.forward() || playerInput.backward() || playerInput.left() || playerInput.right() || playerInput.jump();
   }

   @EventHandler
   private void onTick(Post event) {
      if (!this.isReady()) {
         this.renderPosList.clear();
      } else {
         this.renderPosList.forEach(s -> s.setLeft((Integer)s.getLeft() - 1));
         this.renderPosList.removeIf(s -> (Integer)s.getLeft() <= 0);
         WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
         if (worldSchematic == null) {
            this.warning("未加载投影", new Object[0]);
            this.renderPosList.clear();
            this.toggle();
         } else if (this.checkAndDecrement()) {
            if ((Boolean)this.movePause.get() && this.isMoving) {
               this.setDelay(5);
            } else {
               this.needPlaceBlockList.clear();

               for (BlockPos schematicBlockPos : HeBlockUtils.listPosInSphere(
                  (int)((Double)this.printingRange.get() + 1.0), (int)((Double)this.yPrintingRange.get() + 1.0), this.mc.player.getBlockPos()
               )) {
                  BlockState targetCurBlockState = this.mc.world.getBlockState(schematicBlockPos);
                  BlockState schematicBlockState = worldSchematic.getBlockState(schematicBlockPos);
                  Block targetBlock = targetCurBlockState.getBlock();
                  boolean isNeedPlace = this.mc.player.getBlockPos().isWithinDistance(schematicBlockPos, (Double)this.printingRange.get())
                     && targetCurBlockState.isReplaceable()
                     && !schematicBlockState.isLiquid()
                     && !schematicBlockState.isAir()
                     && targetBlock != schematicBlockState.getBlock()
                     && DataManager.getRenderLayerRange().isPositionWithinRange(schematicBlockPos)
                     && !this.mc
                        .player
                        .getBoundingBox()
                        .intersects(Vec3d.of(schematicBlockPos), Vec3d.of(schematicBlockPos).add(1.0, 1.0, 1.0))
                     && schematicBlockState.canPlaceAt(this.mc.world, schematicBlockPos)
                     && !((List)this.blacklist.get()).contains(schematicBlockState.getBlock());

                  for (Pair<Integer, BlockPos> posPair : this.renderPosList) {
                     if (schematicBlockPos.equals(posPair.getRight())) {
                        isNeedPlace = false;
                        break;
                     }
                  }

                  if (isNeedPlace) {
                     BlockPos blockPos = new BlockPos(schematicBlockPos.getX(), schematicBlockPos.getY(), schematicBlockPos.getZ());
                     PlaceBlockHelper placeBlockHelper = new PlaceBlockHelper(
                        (Boolean)this.debug.get(), blockPos, schematicBlockState, (Double)this.printingRange.get(), (Boolean)this.rotate.get()
                     );
                     placeBlockHelper.setAirPlace((Boolean)this.airPlace.get());
                     if (placeBlockHelper.getCanPlaceDirection() != null) {
                        this.printLog(
                           "BlockIterator add schematicBlockPos : {} {} {}",
                           schematicBlockPos.getX(),
                           schematicBlockPos.getY(),
                           schematicBlockPos.getZ()
                        );
                        this.needPlaceBlockList.add(placeBlockHelper);
                     }
                  }
               }

               if (!this.needPlaceBlockList.isEmpty()) {
                  this.sortPlaceBlock();
                  PlaceBlockHelper placeBlockHelper = this.needPlaceBlockList.getFirst();
                  Item item = placeBlockHelper.getSchematicBlock().asItem();
                  boolean placeSuccess = this.switchItemAndPlace(item, placeBlockHelper);
                  if (placeSuccess && (Boolean)this.renderBlocks.get()) {
                     this.renderPosList.add(new Pair((Integer)this.fadeTime.get(), new BlockPos(placeBlockHelper.getSchematicBlockPos())));
                  }

                  this.setDelay();
               }
            }
         }
      }
   }

   private boolean switchItemAndPlace(Item item, PlaceBlockHelper placeBlockHelper) {
      int selectedSlot = this.getMainSlot();
      if (this.mc.player.getMainHandStack().getItem() == item) {
         this.lastUsedSlot = selectedSlot;
         return placeBlockHelper.tryPlace();
      }

      if (this.lastUsedSlot != -1 && this.getItemStack(this.lastUsedSlot).getItem() == item) {
         InvUtils.swap(this.lastUsedSlot, (Boolean)this.returnHand.get());
         return false;
      }

      FindItemResult result = InvUtils.find(new Item[]{item});
      if (!result.found()) {
         return false;
      }

      if (result.isHotbar()) {
         this.lastUsedSlot = selectedSlot;
         InvUtils.swap(result.slot(), (Boolean)this.returnHand.get());
         return false;
      }

      if (result.isMain()) {
         FindItemResult empty = InvUtils.findEmpty();
         if (empty.found() && empty.isHotbar()) {
            InvUtils.move().from(result.slot()).toHotbar(empty.slot());
            InvUtils.swap(empty.slot(), (Boolean)this.returnHand.get());
         } else if (this.lastUsedSlot != -1) {
            InvUtils.move().from(result.slot()).toHotbar(this.lastUsedSlot);
            InvUtils.swap(this.lastUsedSlot, (Boolean)this.returnHand.get());
         } else {
            this.warning("热栏需要留空", new Object[0]);
         }
      }

      return false;
   }

   private void sortPlaceBlock() {
      if (this.firstAlgorithm.get() != SortAlgorithm.None) {
         if (((SortAlgorithm)this.firstAlgorithm.get()).applySecondSorting && this.secondAlgorithm.get() != SortingSecond.None) {
            this.needPlaceBlockList
               .sort((o1, o2) -> ((SortingSecond)this.secondAlgorithm.get()).algorithm.compare(o1.getSchematicBlockPos(), o2.getSchematicBlockPos()));
         }

         this.needPlaceBlockList
            .sort((o1, o2) -> ((SortAlgorithm)this.firstAlgorithm.get()).algorithm.compare(o1.getSchematicBlockPos(), o2.getSchematicBlockPos()));
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      this.renderPosList
         .forEach(
            s -> {
               Color a = new Color(
                  ((SettingColor)this.colour.get()).r,
                  ((SettingColor)this.colour.get()).g,
                  ((SettingColor)this.colour.get()).b,
                  (int)((float)((Integer)s.getLeft()).intValue() / ((Integer)this.fadeTime.get()).intValue() * ((SettingColor)this.colour.get()).a)
               );
               event.renderer.box((BlockPos)s.getRight(), a, null, ShapeMode.Sides, 0);
            }
         );
   }

   public void onDeactivate() {
   }

   private void printLog(String str, Object arg) {
      if ((Boolean)this.debug.get()) {
         log.info(str, arg);
      }
   }

   private void printLog(String str, Object... args) {
      if ((Boolean)this.debug.get()) {
         log.info(str, args);
      }
   }
}
