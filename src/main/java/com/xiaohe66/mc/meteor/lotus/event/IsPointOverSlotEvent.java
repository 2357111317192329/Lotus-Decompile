package com.xiaohe66.mc.meteor.lotus.event;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.screen.slot.Slot;

public class IsPointOverSlotEvent extends Cancellable {
    private static final IsPointOverSlotEvent INSTANCE = new IsPointOverSlotEvent();
    private Slot slot;
    private double mouseX;
    private double mouseY;
    private boolean result;

    public static IsPointOverSlotEvent get(Slot slot, double mouseX, double mouseY) {
        INSTANCE.setCancelled(false);
        INSTANCE.slot = slot;
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        INSTANCE.result = false;
        return INSTANCE;
    }

    public Slot getSlot() {
        return this.slot;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
