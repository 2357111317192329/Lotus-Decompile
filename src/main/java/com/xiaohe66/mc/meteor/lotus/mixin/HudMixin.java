/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.systems.hud.Hud
 *  meteordevelopment.meteorclient.systems.hud.HudElement
 *  meteordevelopment.meteorclient.systems.hud.XAnchor
 *  meteordevelopment.meteorclient.systems.hud.YAnchor
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.modules.i18n.I18nManager;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Hud.class}, remap=false)
public class HudMixin {
    @Inject(method={"add(Lmeteordevelopment/meteorclient/systems/hud/HudElement;IILmeteordevelopment/meteorclient/systems/hud/XAnchor;Lmeteordevelopment/meteorclient/systems/hud/YAnchor;)V"}, at={@At(value="HEAD")})
    private void onAdd(HudElement hudElement, int x, int y, XAnchor xAnchor, YAnchor yAnchor, CallbackInfo ci) {
        I18nManager.INSTANCE.register(hudElement);
    }

    @Inject(method={"remove(Lmeteordevelopment/meteorclient/systems/hud/HudElement;)V"}, at={@At(value="HEAD")})
    private void onRemove(HudElement hudElement, CallbackInfo ci) {
        I18nManager.INSTANCE.unregister(hudElement.settings);
    }
}

