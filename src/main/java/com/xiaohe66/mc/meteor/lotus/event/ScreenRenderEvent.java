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

public class ScreenRenderEvent extends Event {
    private static final ScreenRenderEvent INSTANCE = new ScreenRenderEvent();
    private DrawContext drawContext;
    private TextRenderer textRenderer;
    private int mouseX;
    private int mouseY;

    public ScreenRenderEvent() {
        super(Stage.Post);
    }

    public static ScreenRenderEvent get(DrawContext drawContext, TextRenderer textRenderer, int mouseX, int mouseY) {
        INSTANCE.drawContext = drawContext;
        INSTANCE.textRenderer = textRenderer;
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
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
}
