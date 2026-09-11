/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.font.TextRenderer
 *  net.minecraft.client.gui.DrawContext
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HandledScreenRenderEvent extends Event {
    private static final HandledScreenRenderEvent INSTANCE = new HandledScreenRenderEvent();
    private GuiGraphicsExtractor drawContext;
    private Font textRenderer;
    private int mouseX;
    private int mouseY;

    public HandledScreenRenderEvent() {
        super(Stage.Post);
    }

    public static HandledScreenRenderEvent get(GuiGraphicsExtractor drawContext, Font textRenderer, int mouseX, int mouseY) {
        INSTANCE.drawContext = drawContext;
        INSTANCE.textRenderer = textRenderer;
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        return INSTANCE;
    }

    public GuiGraphicsExtractor getDrawContext() {
        return this.drawContext;
    }

    public Font getTextRenderer() {
        return this.textRenderer;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }
}
