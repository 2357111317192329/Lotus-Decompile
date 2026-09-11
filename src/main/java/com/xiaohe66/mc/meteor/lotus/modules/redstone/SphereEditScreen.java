/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.gui.GuiTheme
 *  meteordevelopment.meteorclient.gui.WindowScreen
 */
package com.xiaohe66.mc.meteor.lotus.modules.redstone;

import com.xiaohe66.mc.meteor.lotus.modules.redstone.SphereConfig;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;

public class SphereEditScreen extends WindowScreen {
    private final SphereConfig sphereConfig;

    public SphereEditScreen(GuiTheme guiTheme, SphereConfig sphereConfig) {
        super(guiTheme, "球体配置");
        this.sphereConfig = sphereConfig;
    }

    public void initWidgets() {
        this.add(this.theme.settings(this.sphereConfig.settings)).expandX();
    }
}
