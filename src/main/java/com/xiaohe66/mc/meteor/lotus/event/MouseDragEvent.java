/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.Cancellable
 */
package com.xiaohe66.mc.meteor.lotus.event;

import meteordevelopment.meteorclient.events.Cancellable;

public class MouseDragEvent extends Cancellable {
    private static final MouseDragEvent INSTANCE = new MouseDragEvent();
    public double mouseX;
    public double mouseY;
    public int button;
    public double deltaX;
    public double deltaY;
    public int screenX;
    public int screenY;

    public static MouseDragEvent get(double mouseX, double mouseY, int button, double deltaX, double deltaY, int screenX, int screenY) {
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        INSTANCE.button = button;
        INSTANCE.deltaX = deltaX;
        INSTANCE.deltaY = deltaY;
        INSTANCE.screenX = screenX;
        INSTANCE.screenY = screenY;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
