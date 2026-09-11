/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  net.minecraft.text.Text
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen
 *  net.minecraft.block.entity.SignText
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.HeOpenScreenEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={AbstractSignEditScreen.class})
public abstract class MixinAbstractSignEditScreen
extends Screen {
    @Shadow
    private SignText text;
    @Shadow
    @Final
    private String[] messages;

    protected MixinAbstractSignEditScreen(Component textComponent) {
        super(textComponent);
    }

    @Inject(method={"init"}, at={@At(value="RETURN")})
    private void preventGuiOpen(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        HeOpenScreenEvent event = HeOpenScreenEvent.get(mc.screen);
        MeteorClient.EVENT_BUS.post(event);
        if (event.getSignText() != null) {
            this.text = event.getSignText();
            for (int i = 0; i < this.messages.length; ++i) {
                this.messages[i] = this.text.getMessage(i, false).getString();
            }
        }
    }
}

