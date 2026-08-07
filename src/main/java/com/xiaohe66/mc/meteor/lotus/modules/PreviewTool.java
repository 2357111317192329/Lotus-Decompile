package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.HandledScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public class PreviewTool extends BaseModule {
   private static final int SLOT_SIZE = 18;
   private static final int MARGIN = 2;
   private final SettingGroup iconGroup = this.settings.createGroup("盒子图标");
   public final Setting<Boolean> kitIcon = this.iconGroup
      .add(((Builder)((Builder)((Builder)new Builder().name("盒子图标")).description("显示盒子数量最多的物品(以组为单位)")).defaultValue(true)).build());
   private final Setting<Double> iconScale = this.iconGroup
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("图标比例"))
               .description("盒子图标比例"))
            .defaultValue(0.4)
            .sliderRange(0.1, 1.0)
            .build()
      );
   private final Setting<Integer> iconOffsetX = this.iconGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("图标偏移量X"))
                  .description("偏移量"))
               .defaultValue(7))
            .sliderRange(-20, 20)
            .build()
      );
   private final Setting<Integer> iconOffsetY = this.iconGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("图标偏移量Y"))
                  .description("偏移量"))
               .defaultValue(7))
            .sliderRange(-20, 20)
            .build()
      );
   private final SettingGroup kitSpreadGroup = this.settings.createGroup("盒子平铺");
   public final Setting<Boolean> kitSpread = this.kitSpreadGroup
      .add(((Builder)((Builder)((Builder)new Builder().name("盒子平铺")).description("将kit在界面中平铺显示")).defaultValue(true)).build());
   private final Setting<Integer> top = this.kitSpreadGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("上边距"))
                  .description("上边距"))
               .defaultValue(6))
            .min(0)
            .sliderMax(100)
            .build()
      );
   private final Setting<Integer> left = this.kitSpreadGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("左边距"))
                  .description("左边距"))
               .defaultValue(6))
            .min(0)
            .sliderMax(500)
            .build()
      );
   private final Setting<Integer> marge = this.kitSpreadGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("间距"))
                  .description("间距"))
               .defaultValue(4))
            .min(0)
            .sliderMax(10)
            .build()
      );
   private final Setting<Double> scale = this.kitSpreadGroup
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("缩放"))
               .description("平铺区域的缩放比例"))
            .defaultValue(1.0)
            .sliderRange(0.5, 1.5)
            .build()
      );
   private final Setting<Boolean> compact = this.kitSpreadGroup
      .add(((Builder)((Builder)((Builder)new Builder().name("紧凑模式")).description("将相同物品合并显示，节省空间")).defaultValue(true)).build());
   private final Setting<Integer> backgroundAlpha = this.kitSpreadGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("背景透明度"))
                  .description("盒子背景的不透明度"))
               .defaultValue(30))
            .min(0)
            .sliderMax(255)
            .build()
      );
   private int scrollOffset = 0;
   private int lastSyncId = 0;

   public PreviewTool() {
      super("预览器", "盒子小图标预览、盒子内容平铺");
   }

   @EventHandler
   private void onOpenScreen(ScreenRenderEvent event) {
      if ((Boolean)this.kitSpread.get() && this.mc.screen instanceof AbstractContainerScreen) {
         List<ShulkerBoxReader> readerList = this.collectReaders();
         if (!readerList.isEmpty()) {
            if (this.lastSyncId != this.mc.player.containerMenu.containerId) {
               this.scrollOffset = 0;
               this.lastSyncId = this.mc.player.containerMenu.containerId;
            }

            GuiGraphicsExtractor drawContext = event.getDrawContext();
            drawContext.nextStratum();
            Matrix3x2fStack matrices = drawContext.pose();
            matrices.pushMatrix();
            float scaleValue = ((Double)this.scale.get()).floatValue();
            matrices.scale(scaleValue, scaleValue);
            float scaledOffset = this.scrollOffset / scaleValue;
            int startX = (Integer)this.left.get();
            int currentY = (Integer)this.top.get() + (int)scaledOffset;
            ItemStack tooltipStack = ItemStack.EMPTY;

            for (ShulkerBoxReader reader : readerList) {
               List<ItemStack> stacks = this.compact.get() ? reader.getCondensed() : new ArrayList<>(reader.getStacks());
               if (!(Boolean)this.compact.get()) {
                  while (stacks.size() < 27) {
                     stacks.add(ItemStack.EMPTY);
                  }
               }

               int itemCount = this.compact.get() ? (int)stacks.stream().filter(s -> !s.isEmpty()).count() : stacks.size();
               int rows = Math.max(1, (itemCount - 1) / 9 + 1);
               int cols = Mth.clamp(itemCount, 1, 9);
               int boxWidth = cols * 18 + 4;
               int boxHeight = rows * 18 + 4;
               int color = reader.getColor();
               int bgAlpha = (Integer)this.backgroundAlpha.get();
               int bgColor = bgAlpha << 24;
               drawContext.fill(startX, currentY, startX + boxWidth, currentY + boxHeight, bgColor);
               drawContext.fill(startX, currentY - 1, startX + boxWidth, currentY, color);
               int count = 0;

               for (ItemStack stack : stacks) {
                  if (!(Boolean)this.compact.get() || !stack.isEmpty()) {
                     int col = count % 9;
                     int row = count / 9;
                     int x = startX + 2 + col * 18;
                     int y = currentY + 2 + row * 18;
                     this.drawStack(drawContext, event.getTextRenderer(), stack, x, y);
                     if (this.isMouseOverSlot(event.getMouseX(), event.getMouseY(), x, y, scaleValue)) {
                        tooltipStack = stack;
                     }

                     count++;
                  }
               }

               if (reader.getOriginItemStackCount() > 1) {
                  String text = "x" + reader.getOriginItemStackCount();
                  int textX = startX + boxWidth + 2;
                  int textY = currentY + boxHeight - 9 - 2;
                  drawContext.text(event.getTextRenderer(), text, textX, textY, Color.GREEN.getRGB(), true);
               }

               currentY += boxHeight + this.marge.get();
            }

            if (!tooltipStack.isEmpty()) {
               float invScale = 1.0F / scaleValue;
               matrices.pushMatrix();
               matrices.scale(invScale, invScale);
               drawContext.setTooltipForNextFrame(event.getTextRenderer(), tooltipStack, event.getMouseX(), event.getMouseY());
               matrices.popMatrix();
            }

            matrices.popMatrix();
         }
      }
   }

   @EventHandler
   private void onMouseScroll(MouseScrollEvent event) {
      if ((Boolean)this.kitSpread.get() && this.mc.screen instanceof AbstractContainerScreen) {
         List<ShulkerBoxReader> readerList = this.collectReaders();
         if (!readerList.isEmpty()) {
            float totalHeight = 0.0F;

            for (ShulkerBoxReader reader : readerList) {
               List<ItemStack> stacks = this.compact.get() ? reader.getCondensed() : reader.getStacks();
               int itemCount = this.compact.get() ? (int)stacks.stream().filter(s -> !s.isEmpty()).count() : 27;
               int rows = Math.max(1, (itemCount - 1) / 9 + 1);
               totalHeight += rows * 18 + 4 + this.marge.get();
            }

            totalHeight += ((Integer)this.marge.get()).intValue();
            float scaleValue = ((Double)this.scale.get()).floatValue();
            float maxScroll = Math.min(-totalHeight + this.mc.getWindow().getGuiScaledHeight() / scaleValue, 0.0F);
            this.scrollOffset = (int)Mth.clamp(this.scrollOffset + Math.ceil(event.getVerticalAmount()) * 15.0, maxScroll, 0.0);
         }
      }
   }

   private List<ShulkerBoxReader> collectReaders() {
      List<ShulkerBoxReader> list = new ArrayList<>();

      for (ItemStack boxItemStack : HeInvUtils.findAndMargeShulkerBox()) {
         ShulkerBoxReader reader = new ShulkerBoxReader(boxItemStack);
         if (!reader.isEmpty()) {
            list.add(reader);
         }
      }

      return list;
   }

   private void drawStack(GuiGraphicsExtractor ctx, Font textRenderer, ItemStack stack, int x, int y) {
      if (!stack.isEmpty()) {
         ctx.item(stack, x, y);
         if (stack.getCount() > 999) {
            String text = "%.1fk".formatted(stack.getCount() / 1000.0F);
            ctx.itemDecorations(textRenderer, stack, x, y, text);
         } else {
            ctx.itemDecorations(textRenderer, stack, x, y);
         }
      }
   }

   private boolean isMouseOverSlot(int mouseX, int mouseY, int slotX, int slotY, float scale) {
      int scaledX = (int)(mouseX / scale);
      int scaledY = (int)(mouseY / scale);
      return scaledX >= slotX && scaledX < slotX + 18 && scaledY >= slotY && scaledY < slotY + 18;
   }

   @EventHandler
   private void onHandledScreenRenderEvent(HandledScreenRenderEvent event) {
      if ((Boolean)this.kitIcon.get() && this.mc.screen instanceof AbstractContainerScreen screen) {
         for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack != null && HeItemUtils.isShulkerBox(stack.getItem())) {
               this.drawKitIcon(event.getDrawContext(), stack, slot.x, slot.y);
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if ((Boolean)this.kitIcon.get() && !(this.mc.screen instanceof AbstractContainerScreen)) {
         int scaledWidth = this.mc.getWindow().getGuiScaledWidth();
         int scaledHeight = this.mc.getWindow().getGuiScaledHeight();
         int hotbarWidth = 182;
         int hotbarHeight = 22;
         int startX = (scaledWidth - hotbarWidth) / 2 + 3;
         int startY = scaledHeight - hotbarHeight + 3;

         for (int i = 0; i < 9; i++) {
            ItemStack stack = this.mc.player.getInventory().getItem(i);
            if (stack != null && HeItemUtils.isShulkerBox(stack.getItem())) {
               int slotX = startX + i * 20;
               int slotY = startY;
               this.drawKitIcon(event.graphics, stack, slotX, slotY);
            }
         }
      }
   }

   private void drawKitIcon(GuiGraphicsExtractor drawContext, ItemStack kitItemStack, int originX, int originY) {
      ShulkerBoxReader reader = new ShulkerBoxReader(kitItemStack);
      ItemStack maximumItemStack = reader.getMaximumItem();
      if (!maximumItemStack.isEmpty()) {
         float scaleValue = ((Double)this.iconScale.get()).floatValue();
         Matrix3x2fStack matrices = drawContext.pose();
         matrices.pushMatrix();
         matrices.translate(originX + (Integer)this.iconOffsetX.get(), originY + (Integer)this.iconOffsetY.get());
         matrices.scale(scaleValue, scaleValue);
         drawContext.item(maximumItemStack, 0, 0);
         matrices.popMatrix();
         matrices.pushMatrix();
         matrices.translate(-2.0F, 0.0F);
         int beginY = 2;
         int maxHeight = 12;
         int height = (int)(maximumItemStack.getCount() * 1.0 / maximumItemStack.getMaxStackSize() / 27.0 * maxHeight);
         drawContext.fill(originX + 16, originY + beginY, originX + 17, originY + beginY + maxHeight, -1);
         drawContext.fill(originX + 16, originY + (16 - beginY - height), originX + 17, originY + (16 - beginY), -16711936);
         matrices.popMatrix();
      }
   }

   public void onDeactivate() {
   }
}
