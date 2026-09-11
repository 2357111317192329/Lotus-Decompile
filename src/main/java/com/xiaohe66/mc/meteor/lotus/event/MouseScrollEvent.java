/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.Cancellable
 */
package com.xiaohe66.mc.meteor.lotus.event;

import meteordevelopment.meteorclient.events.Cancellable;

public class MouseScrollEvent extends Cancellable {
    private static final MouseScrollEvent INSTANCE = new MouseScrollEvent();
    private double verticalAmount;
    private double mouseX;
    private double mouseY;
    private int screenX;
    private int screenY;

    public static MouseScrollEvent get(double mouseX, double mouseY, double verticalAmount, int screenX, int screenY) {
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        INSTANCE.verticalAmount = verticalAmount;
        INSTANCE.screenX = screenX;
        INSTANCE.screenY = screenY;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }

    public double getVerticalAmount() {
        return this.verticalAmount;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public int getScreenX() {
        return this.screenX;
    }

    public int getScreenY() {
        return this.screenY;
    }
}
