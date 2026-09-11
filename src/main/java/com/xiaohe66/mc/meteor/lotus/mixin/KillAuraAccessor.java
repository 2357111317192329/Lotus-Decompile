/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.systems.modules.combat.KillAura
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={KillAura.class}, remap=false)
public interface KillAuraAccessor {
    @Mutable
    @Accessor(value="autoSwitch")
    public Setting<Boolean> getAutoSwitch();

    @Mutable
    @Accessor(value="swapBack")
    public Setting<Boolean> getSwapBack();
}

