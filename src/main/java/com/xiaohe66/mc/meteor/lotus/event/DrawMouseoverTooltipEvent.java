package com.xiaohe66.mc.meteor.lotus.event;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.gui.DrawContext;

public class DrawMouseoverTooltipEvent extends Cancellable {
    private static final DrawMouseoverTooltipEvent INSTANCE = new DrawMouseoverTooltipEvent();
    private DrawContext drawContext;
    private int mouseX;
    private int mouseY;

    public static DrawMouseoverTooltipEvent get(DrawContext drawContext, int mouseX, int mouseY) {
        INSTANCE.setCancelled(false);
        INSTANCE.drawContext = drawContext;
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        return INSTANCE;
    }

    public DrawContext getDrawContext() {
        return this.drawContext;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }
}
