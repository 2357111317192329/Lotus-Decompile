package com.xiaohe66.mc.meteor.lotus;

import com.xiaohe66.mc.meteor.lotus.modules.AutoPrinterMap;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class BaritoneSchematicModules {
    private BaritoneSchematicModules() {
    }

    public static void add(Modules modules) {
        modules.add(new AutoPrinterMap());
    }
}