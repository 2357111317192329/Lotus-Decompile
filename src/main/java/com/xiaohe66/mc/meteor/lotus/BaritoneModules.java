package com.xiaohe66.mc.meteor.lotus;

import com.xiaohe66.mc.meteor.lotus.modules.AutoClearUp;
import com.xiaohe66.mc.meteor.lotus.modules.AutoKit;
import com.xiaohe66.mc.meteor.lotus.modules.VillagerBookRoller;
import com.xiaohe66.mc.meteor.lotus.modules.VillagerTrader;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class BaritoneModules {
    private BaritoneModules() {
    }

    public static void add(Modules modules) {
        modules.add(new AutoClearUp());
        modules.add(new AutoKit());
        modules.add(new VillagerBookRoller());
        modules.add(new VillagerTrader());
    }
}