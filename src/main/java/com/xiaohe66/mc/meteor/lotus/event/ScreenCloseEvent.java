/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;

public class ScreenCloseEvent extends Event {
    private static final ScreenCloseEvent INSTANCE = new ScreenCloseEvent();

    public static ScreenCloseEvent get() {
        return INSTANCE;
    }

    public ScreenCloseEvent() {
        super(Stage.Pre);
    }
}
