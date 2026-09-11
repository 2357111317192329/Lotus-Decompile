/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.block.entity.SignText
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.SignText;

public class HeOpenScreenEvent extends Event {
    private static final HeOpenScreenEvent INSTANCE = new HeOpenScreenEvent();
    public Screen screen;
    private SignText signText;

    public HeOpenScreenEvent() {
        super(Stage.Post);
    }

    public SignText getSignText() {
        return this.signText;
    }

    public void setSignText(SignText signText) {
        this.signText = signText;
    }

    public static HeOpenScreenEvent get(Screen screen) {
        INSTANCE.signText = null;
        INSTANCE.setCancelled(false);
        INSTANCE.screen = screen;
        return INSTANCE;
    }
}
