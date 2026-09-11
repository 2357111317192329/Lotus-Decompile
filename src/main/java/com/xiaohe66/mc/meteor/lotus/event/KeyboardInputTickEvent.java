/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.PlayerInput
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.world.entity.player.Input;

public class KeyboardInputTickEvent extends Event {
    private Input playerInput;

    public KeyboardInputTickEvent() {
        super(Stage.Post);
    }

    public Input getPlayerInput() {
        return this.playerInput;
    }

    public void setPlayerInput(Input playerInput) {
        this.playerInput = playerInput;
    }
}
