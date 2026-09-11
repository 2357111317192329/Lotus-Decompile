/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.PlayerInput
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import net.minecraft.util.PlayerInput;

public class KeyboardInputTickEvent extends Event {
    private PlayerInput playerInput;

    public KeyboardInputTickEvent() {
        super(Stage.Post);
    }

    public PlayerInput getPlayerInput() {
        return this.playerInput;
    }

    public void setPlayerInput(PlayerInput playerInput) {
        this.playerInput = playerInput;
    }
}
