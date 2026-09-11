/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={Module.class}, remap=false)
public interface ModuleAccessor {
    @Mutable
    @Accessor(value="title")
    public void setTitle(String title);

    @Mutable
    @Accessor(value="description")
    public void setDescription(String description);
}

