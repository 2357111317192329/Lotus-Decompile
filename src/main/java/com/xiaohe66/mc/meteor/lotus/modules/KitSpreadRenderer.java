/*
 * Port of Lotus 19.5 aR (kit spread renderer, decompiled bytecode-accurate).
 * Spreads the contents of every shulker box in the open container screen
 * over the screen, one box per row, scrollable with the mouse wheel.
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

public class KitSpreadRenderer {
    private static final int SLOT_SIZE = 18;
    private static final int MARGIN = 2;

    private final Setting<Integer> top;
    private final Setting<Integer> left;
    private final Setting<Integer> spacing;
    private final Setting<Double> scale;
    private final Setting<Boolean> compact;
    private final Setting<Integer> backgroundAlpha;
    private int scrollOffset;
    private int lastSyncId;

    public KitSpreadRenderer(Setting<Integer> top, Setting<Integer> left, Setting<Integer> spacing, Setting<Double> scale, Setting<Boolean> compact, Setting<Integer> backgroundAlpha) {
        this.top = top;
        this.left = left;
        this.spacing = spacing;
        this.scale = scale;
        this.compact = compact;
        this.backgroundAlpha = backgroundAlpha;
        this.scrollOffset = 0;
        this.lastSyncId = 0;
    }

    public void onOpenScreen(ScreenRenderEvent event) {
        List<ShulkerBoxReader> readerList = this.collectReaders();
        if (readerList.isEmpty()) {
            return;
        }
        if (this.lastSyncId != MeteorClient.mc.player.currentScreenHandler.syncId) {
            this.scrollOffset = 0;
            this.lastSyncId = MeteorClient.mc.player.currentScreenHandler.syncId;
        }
        DrawContext drawContext = event.getDrawContext();
        drawContext.createNewRootLayer();
        Matrix3x2fStack matrices = drawContext.getMatrices();
        matrices.pushMatrix();
        float scaleFactor = this.scale.get().floatValue();
        matrices.scale(scaleFactor, scaleFactor);
        float scrollOffsetScaled = (float)this.scrollOffset / scaleFactor;
        int leftPos = this.left.get();
        int topPos = this.top.get() + (int)scrollOffsetScaled;
        ItemStack tooltipStack = ItemStack.EMPTY;
        for (ShulkerBoxReader reader : readerList) {
            List<ItemStack> stacks = this.compact.get() != false ? reader.getCondensed() : new ArrayList<ItemStack>(reader.getStacks());
            if (!this.compact.get()) {
                while (stacks.size() < 27) {
                    stacks.add(ItemStack.EMPTY);
                }
            }
            int itemCount = this.compact.get() != false ? (int)stacks.stream().filter(s -> !s.isEmpty()).count() : stacks.size();
            int rowCount = Math.max(1, (itemCount - 1) / 9 + 1);
            int colCount = MathHelper.clamp(itemCount, 1, 9);
            int boxWidth = colCount * 18 + 4;
            int boxHeight = rowCount * 18 + 4;
            int boxColor = reader.getColor();
            int bgColor = this.backgroundAlpha.get() << 24;
            drawContext.fill(leftPos, topPos, leftPos + boxWidth, topPos + boxHeight, bgColor);
            drawContext.fill(leftPos, topPos - 1, leftPos + boxWidth, topPos, boxColor);
            int index = 0;
            for (ItemStack stack : stacks) {
                if (this.compact.get() && stack.isEmpty()) continue;
                int col = index % 9;
                int row = index / 9;
                int x = leftPos + 2 + col * 18;
                int y = topPos + 2 + row * 18;
                this.drawStack(drawContext, event.getTextRenderer(), stack, x, y);
                if (this.isMouseOverSlot(event.getMouseX(), event.getMouseY(), x, y, scaleFactor)) {
                    tooltipStack = stack;
                }
                ++index;
            }
            if (reader.getOriginItemStackCount() > 1) {
                String countText = "x" + reader.getOriginItemStackCount();
                int countX = leftPos + boxWidth + 2;
                int countY = topPos + boxHeight - 9 - 2;
                drawContext.drawText(event.getTextRenderer(), countText, countX, countY, Color.GREEN.getPacked(), true);
            }
            topPos += boxHeight + this.spacing.get();
        }
        if (!tooltipStack.isEmpty()) {
            float inverseScale = 1.0f / scaleFactor;
            matrices.pushMatrix();
            matrices.scale(inverseScale, inverseScale);
            drawContext.drawItemTooltip(event.getTextRenderer(), tooltipStack, event.getMouseX(), event.getMouseY());
            matrices.popMatrix();
        }
        matrices.popMatrix();
    }

    public void onMouseScroll(MouseScrollEvent event) {
        List<ShulkerBoxReader> readerList = this.collectReaders();
        if (readerList.isEmpty()) {
            return;
        }
        float totalHeight = 0.0f;
        for (ShulkerBoxReader reader : readerList) {
            List<ItemStack> stacks = this.compact.get() != false ? reader.getCondensed() : reader.getStacks();
            int itemCount = this.compact.get() != false ? (int)stacks.stream().filter(s -> !s.isEmpty()).count() : 27;
            int rowCount = Math.max(1, (itemCount - 1) / 9 + 1);
            totalHeight += (float)(rowCount * 18 + 4 + this.spacing.get());
        }
        float scaleFactor = this.scale.get().floatValue();
        float maxScroll = Math.min(-(totalHeight += (float)this.spacing.get()) + (float)MeteorClient.mc.getWindow().getScaledHeight() / scaleFactor, 0.0f);
        this.scrollOffset = (int)MathHelper.clamp((double)this.scrollOffset + Math.ceil(event.getVerticalAmount()) * 15.0, (double)maxScroll, (double)0.0);
    }

    private List<ShulkerBoxReader> collectReaders() {
        ArrayList<ShulkerBoxReader> readerList = new ArrayList<ShulkerBoxReader>();
        for (ItemStack boxItemStack : HeInvUtils.findAndMargeShulkerBox()) {
            ShulkerBoxReader reader = new ShulkerBoxReader(boxItemStack);
            if (reader.isEmpty()) continue;
            readerList.add(reader);
        }
        return readerList;
    }

    private void drawStack(DrawContext drawContext, TextRenderer textRenderer, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        drawContext.drawItemWithoutEntity(stack, x, y);
        if (stack.getCount() > 999) {
            String countText = "%.1fk".formatted(Float.valueOf((float)stack.getCount() / 1000.0f));
            drawContext.drawStackOverlay(textRenderer, stack, x, y, countText);
        } else {
            drawContext.drawStackOverlay(textRenderer, stack, x, y);
        }
    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int slotX, int slotY, float scale) {
        int scaledX = (int)((float)mouseX / scale);
        int scaledY = (int)((float)mouseY / scale);
        return scaledX >= slotX && scaledX < slotX + SLOT_SIZE && scaledY >= slotY && scaledY < slotY + SLOT_SIZE;
    }
}