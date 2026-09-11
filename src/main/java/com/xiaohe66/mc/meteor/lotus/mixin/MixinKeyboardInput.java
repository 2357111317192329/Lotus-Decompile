/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  net.minecraft.client.input.KeyboardInput
 *  net.minecraft.client.input.Input
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.KeyboardInputTickEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={KeyboardInput.class})
public abstract class MixinKeyboardInput
extends ClientInput {
    @Inject(method={"tick"}, at={@At(value="TAIL")})
    private void isPressed(CallbackInfo ci) {
        KeyboardInputTickEvent event = new KeyboardInputTickEvent();
        event.setPlayerInput(this.keyPresses);
        MeteorClient.EVENT_BUS.post(event);
        this.keyPresses = event.getPlayerInput();
    }
}

