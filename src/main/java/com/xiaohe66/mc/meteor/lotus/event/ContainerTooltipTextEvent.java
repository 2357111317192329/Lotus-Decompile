package com.xiaohe66.mc.meteor.lotus.event;

import meteordevelopment.meteorclient.events.Cancellable;

public class ContainerTooltipTextEvent extends Cancellable {
    private static final ContainerTooltipTextEvent INSTANCE = new ContainerTooltipTextEvent();

    public static ContainerTooltipTextEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
