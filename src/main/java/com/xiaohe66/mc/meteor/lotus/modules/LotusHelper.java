/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;

import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class LotusHelper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public LotusHelper() {
        super(Const.CATEGORY, "Lotus助手", "Lotus插件的辅助功能");
    }
}

