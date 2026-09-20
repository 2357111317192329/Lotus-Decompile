package com.xiaohe66.mc.meteor.lotus;

import com.xiaohe66.mc.meteor.lotus.modules.Printer;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class SchematicModules {
    private SchematicModules() {
    }

    public static void add(Modules modules) {
        modules.add(new Printer());
    }
}