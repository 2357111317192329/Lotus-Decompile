/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.Setting
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import meteordevelopment.meteorclient.settings.Setting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={Setting.class}, remap=false)
public interface SettingAccessor {
    @Mutable
    @Accessor(value="title")
    public void setTitle(String title);

    @Mutable
    @Accessor(value="description")
    public void setDescription(String description);
}

