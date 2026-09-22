/*
 * Port of Lotus 19.5 aJ (pinned preview tooltip renderer, decompiled
 * bytecode-accurate).  Holds the "盒子预览" (box preview) feature: pin a
 * shulker box / bundle slot with the configured key to display a tooltip
 * with its contents laid out as clickable cells, and allow pinning a second
 * layer from the first one.
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.ContainerTooltipTextEvent;
import com.xiaohe66.mc.meteor.lotus.event.DrawMouseoverTooltipEvent;
import com.xiaohe66.mc.meteor.lotus.event.IsPointOverSlotEvent;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.TooltipDataEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.tooltip.BundleTooltipComponent;
import meteordevelopment.meteorclient.utils.tooltip.ContainerTooltipComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.OrderedTextTooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.joml.Vector2i;

public class PinnedPreviewRenderer {
    private final Setting<Boolean> enabled;
    private final Setting<Keybind> pinKey;
    private final ItemStack[] containerStacks = new ItemStack[27];
    private Slot pinnedSlot;
    private boolean keyPressed;
    private final TooltipGrid firstGrid = new TooltipGrid();
    private final TooltipGrid secondGrid = new TooltipGrid();
    private int pinnedIndex = -1;

    public PinnedPreviewRenderer(Setting<Boolean> enabled, Setting<Keybind> pinKey) {
        this.enabled = enabled;
        this.pinKey = pinKey;
    }

    /** Cancels the vanilla container tooltip so the pinned preview can render instead. */
    public void cancelTooltip(ContainerTooltipTextEvent event) {
        if (this.enabled.get() && MeteorClient.mc.currentScreen instanceof HandledScreen) {
            event.cancel();
        }
    }

    /** Injects the shulker box / bundle contents image into Meteor's tooltip data event. */
    public void onTooltipData(TooltipDataEvent event) {
        if (this.enabled.get() && MeteorClient.mc.currentScreen instanceof HandledScreen) {
            ItemStack stack = event.itemStack;
            if (HeItemUtils.isShulkerBox(stack.getItem())) {
                if (Utils.hasItems(stack)) {
                    Utils.getItemsInContainerItem(stack, this.containerStacks);
                    event.tooltipData = new ContainerTooltipComponent(this.containerStacks, Utils.getShulkerColor(stack));
                }
            } else {
                BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (bundle != null && !bundle.isEmpty()) {
                    event.tooltipData = new BundleTooltipComponent(this.toArray(bundle), bundle);
                }
            }
        }
    }

    /** Full reset (pin cleared, key state cleared). */
    public void reset() {
        this.clearPin();
        this.keyPressed = false;
    }

    /** Forces the vanilla slot hover result while a pin is active. */
    public void isPointOverSlot(IsPointOverSlotEvent event) {
        if (!this.enabled.get()) {
            this.clearPin();
        } else if (this.pinnedSlot != null) {
            if (this.pinnedSlot.hasStack() && MeteorClient.mc.player.currentScreenHandler.slots.contains(this.pinnedSlot)) {
                event.setResult(event.getSlot() == this.pinnedSlot && MeteorClient.mc.player.currentScreenHandler.getCursorStack().isEmpty());
                event.cancel();
            } else {
                this.clearPin();
            }
        }
    }

    /** Draws the pinned preview and cancels the vanilla mouse-over tooltip. */
    public void drawPinned(DrawMouseoverTooltipEvent event) {
        if (this.enabled.get() && this.pinnedSlot != null) {
            this.draw(event.getDrawContext(), event.getMouseX(), event.getMouseY());
            event.cancel();
        }
    }

    private void draw(DrawContext drawContext, int mouseX, int mouseY) {
        ItemStack stack = this.pinnedSlot.getStack();
        if (!stack.isEmpty() && this.isValidStack(stack)) {
            this.draw(drawContext, this.firstGrid, stack);
            if (this.pinnedIndex >= 0) {
                ItemStack subStack = this.findStackByIndex(this.firstGrid, this.pinnedIndex);
                if (subStack != null && this.isValidStack(subStack)) {
                    this.draw(drawContext, this.secondGrid, subStack);
                } else {
                    this.pinnedIndex = -1;
                }
            }
            if (this.pinnedIndex < 0 || (!this.drawHovered(drawContext, this.secondGrid, mouseX, mouseY) && !this.secondGrid.isPointInTooltip(mouseX, mouseY))) {
                this.drawHovered(drawContext, this.firstGrid, mouseX, mouseY);
            }
        } else {
            this.clearPin();
        }
    }

    private boolean isValidStack(ItemStack stack) {
        if (HeItemUtils.isShulkerBox(stack.getItem())) {
            return Utils.hasItems(stack);
        } else {
            BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            return bundle != null && !bundle.isEmpty();
        }
    }

    /** Handles the pin key press while a container screen is open. */
    public void updatePinned(int mouseX, int mouseY, Slot hoveredSlot) {
        Keybind key = this.pinKey.get();
        boolean pressed = key.isSet() && key.isPressed();
        if (pressed && !this.keyPressed) {
            if (this.pinnedSlot == null) {
                if (hoveredSlot != null && hoveredSlot.hasStack() && this.isValidStack(hoveredSlot.getStack())) {
                    this.pinnedSlot = hoveredSlot;
                    this.firstGrid.x = mouseX;
                    this.firstGrid.y = mouseY;
                }
            } else if (this.pinnedIndex >= 0 && this.secondGrid.isPointInTooltip(mouseX, mouseY)) {
                this.pinnedIndex = -1;
            } else {
                SlotRect rect = this.findRect(this.firstGrid, mouseX, mouseY);
                if (rect != null && this.isValidStack(rect.stack())) {
                    if (this.pinnedIndex == rect.index()) {
                        this.pinnedIndex = -1;
                    } else {
                        this.pinnedIndex = rect.index();
                        this.secondGrid.x = mouseX;
                        this.secondGrid.y = mouseY;
                    }
                } else {
                    this.clearPin();
                }
            }
        }
        this.keyPressed = pressed;
    }

    private void draw(DrawContext drawContext, TooltipGrid grid, ItemStack stack) {
        TextRenderer textRenderer = MeteorClient.mc.textRenderer;
        BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        boolean isBundle = !HeItemUtils.isShulkerBox(stack.getItem()) && bundle != null && !bundle.isEmpty();
        ArrayList<TooltipComponent> components = new ArrayList<TooltipComponent>();
        for (Text line : Screen.getTooltipFromItem(MeteorClient.mc, stack)) {
            components.add(new OrderedTextTooltipComponent(line.asOrderedText()));
        }
        if (isBundle) {
            components.add(new BundleTooltipComponent(this.toArray(bundle), bundle));
        } else {
            Utils.getItemsInContainerItem(stack, this.containerStacks);
            components.add(new ContainerTooltipComponent(this.containerStacks, Utils.getShulkerColor(stack)));
        }
        TooltipPositioner positioner = (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> {
            int tooltipX = grid.x + 12;
            int tooltipY = grid.y - 12;
            if (tooltipX + tooltipWidth > screenWidth) {
                tooltipX = Math.max(tooltipX - 24 - tooltipWidth, 4);
            }
            if (tooltipY + tooltipHeight + 3 > screenHeight) {
                tooltipY = screenHeight - tooltipHeight - 3;
            }
            grid.tx = tooltipX;
            grid.ty = tooltipY;
            grid.tw = tooltipWidth;
            grid.th = tooltipHeight;
            return new Vector2i(tooltipX, tooltipY);
        };
        drawContext.drawTooltipImmediately(textRenderer, components, grid.x, grid.y, positioner, stack.get(DataComponentTypes.TOOLTIP_STYLE));
        grid.rects.clear();
        int y = grid.ty;
        for (int i = 0; i < components.size() - 1; ++i) {
            y += components.get(i).getHeight(textRenderer) + (i == 0 ? 2 : 0);
        }
        if (isBundle) {
            this.buildBundleGrid(grid, bundle, y);
        } else {
            this.buildShulkerGrid(grid, grid.tx + 8, y + 7);
        }
    }

    private void buildShulkerGrid(TooltipGrid grid, int x, int y) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = row * 9 + col;
                ItemStack stack = this.containerStacks[index];
                if (stack != null && !stack.isEmpty()) {
                    int sx = x + col * 18;
                    int sy = y + row * 18;
                    grid.rects.add(new SlotRect(sx, sy, 18, sx, sy, index, stack));
                }
            }
        }
    }

    private void buildBundleGrid(TooltipGrid grid, BundleContentsComponent bundle, int y) {
        List<ItemStack> stacks = bundle.stream().toList();
        for (int i = 0; i < stacks.size(); ++i) {
            int sx = grid.tx + 8 + i % 8 * 24;
            int sy = y + 8 + i / 8 * 24;
            grid.rects.add(new SlotRect(sx, sy, 24, sx + 4, sy + 4, i, stacks.get(i)));
        }
    }

    private boolean drawHovered(DrawContext drawContext, TooltipGrid grid, int mouseX, int mouseY) {
        SlotRect rect = this.findRect(grid, mouseX, mouseY);
        if (rect == null) {
            return false;
        } else {
            drawContext.fill(rect.fillX(), rect.fillY(), rect.fillX() + 16, rect.fillY() + 16, -2130706433);
            drawContext.drawItemTooltip(MeteorClient.mc.textRenderer, rect.stack(), mouseX, mouseY);
            return true;
        }
    }

    private SlotRect findRect(TooltipGrid grid, int mouseX, int mouseY) {
        for (SlotRect rect : grid.rects) {
            if (mouseX >= rect.x() && mouseX < rect.x() + rect.size() && mouseY >= rect.y() && mouseY < rect.y() + rect.size()) {
                return rect;
            }
        }
        return null;
    }

    private ItemStack findStackByIndex(TooltipGrid grid, int index) {
        for (SlotRect rect : grid.rects) {
            if (rect.index() == index) {
                return rect.stack();
            }
        }
        return null;
    }

    private ItemStack[] toArray(BundleContentsComponent bundle) {
        List<ItemStack> stacks = bundle.stream().toList();
        return stacks.toArray(new ItemStack[0]);
    }

    private void clearPin() {
        this.pinnedSlot = null;
        this.pinnedIndex = -1;
    }
}