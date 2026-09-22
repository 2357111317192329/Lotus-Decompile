/*
 * Port of Lotus 19.5 aQ (kit icon renderer, decompiled bytecode-accurate).
 * Renders a small icon of the dominant item of every shulker box slot, with
 * a vertical fill bar showing how full the box is.
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;

import java.awt.Color;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.joml.Matrix3x2fStack;

public class KitIconRenderer {
    private final Setting<Double> iconScale;
    private final Setting<Integer> offsetX;
    private final Setting<Integer> offsetY;

    public KitIconRenderer(Setting<Double> iconScale, Setting<Integer> offsetX, Setting<Integer> offsetY) {
        this.iconScale = iconScale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /** Renders the icons over the hotbar when no container screen is open. */
    public void render(DrawContext drawContext) {
        int scaledWidth = MeteorClient.mc.getWindow().getScaledWidth();
        int scaledHeight = MeteorClient.mc.getWindow().getScaledHeight();
        int hotbarWidth = 182;
        int hotbarHeight = 22;
        int x = (scaledWidth - hotbarWidth) / 2 + 3;
        int y = scaledHeight - hotbarHeight + 3;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MeteorClient.mc.player.getInventory().getStack(i);
            if (stack != null && HeItemUtils.isShulkerBox(stack.getItem())) {
                this.drawIcon(drawContext, stack, x + i * 20, y);
            }
        }
    }

    /** Renders the icons over every (shulker box) slot of an open container screen. */
    public void render(DrawContext drawContext, List<Slot> slots) {
        for (Slot slot : slots) {
            ItemStack stack = slot.getStack();
            if (stack != null && HeItemUtils.isShulkerBox(stack.getItem())) {
                this.drawIcon(drawContext, stack, slot.x, slot.y);
            }
        }
    }

    private void drawIcon(DrawContext drawContext, ItemStack stack, int x, int y) {
        ShulkerBoxReader reader = new ShulkerBoxReader(stack);
        ItemStack maxItem = reader.getMaximumItem();
        if (!maxItem.isEmpty()) {
            float scale = this.iconScale.get().floatValue();
            Matrix3x2fStack matrices = drawContext.getMatrices();
            matrices.pushMatrix();
            matrices.translate((float)(x + this.offsetX.get()), (float)(y + this.offsetY.get()));
            matrices.scale(scale, scale);
            drawContext.drawItemWithoutEntity(maxItem, 0, 0);
            matrices.popMatrix();
            matrices.pushMatrix();
            matrices.translate(-2.0F, 0.0F);
            int barTop = 2;
            int barHeight = 12;
            int fillHeight = (int)((double)maxItem.getCount() * (double)1.0F / (double)maxItem.getMaxCount() / (double)27.0F * (double)barHeight);
            int barX1 = x + 16;
            int barX2 = x + 17;
            int barY1 = y + barTop;
            int barY2 = barY1 + barHeight;
            if (fillHeight == barHeight) {
                drawContext.fill(barX1, barY1, barX2, barY2, (new Color(255, 0, 0)).getRGB());
            } else {
                int countY = barY2 - fillHeight;
                drawContext.fill(barX1, barY1, barX2, countY, -1);
                drawContext.fill(barX1, countY, barX2, barY2, -16711936);
            }
            matrices.popMatrix();
        }
    }
}