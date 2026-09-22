/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.font.TextRenderer
 *  net.minecraft.client.gui.DrawContext
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

public class HandledScreenRenderEvent extends Event {
    private static final HandledScreenRenderEvent INSTANCE = new HandledScreenRenderEvent();
    private DrawContext drawContext;
    private TextRenderer textRenderer;
    private int mouseX;
    private int mouseY;
    private Slot hoveredSlot;

    public HandledScreenRenderEvent() {
        super(Stage.Post);
    }

    public static HandledScreenRenderEvent get(DrawContext drawContext, TextRenderer textRenderer, int mouseX, int mouseY, Slot hoveredSlot) {
        INSTANCE.drawContext = drawContext;
        INSTANCE.textRenderer = textRenderer;
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        INSTANCE.hoveredSlot = hoveredSlot;
        return INSTANCE;
    }

    public DrawContext getDrawContext() {
        return this.drawContext;
    }

    public TextRenderer getTextRenderer() {
        return this.textRenderer;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }

    public Slot getHoveredSlot() {
        return this.hoveredSlot;
    }
}
