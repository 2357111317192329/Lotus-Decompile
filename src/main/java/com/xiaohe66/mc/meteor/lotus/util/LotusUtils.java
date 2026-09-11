/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  meteordevelopment.meteorclient.systems.modules.combat.KillAura
 *  meteordevelopment.meteorclient.systems.modules.render.FreeLook
 *  meteordevelopment.meteorclient.systems.modules.render.FreeLook$Mode
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 */
package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.modules.LitematicaPrinter;

import com.xiaohe66.mc.meteor.lotus.mixin.KillAuraAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class LotusUtils {
    private LotusUtils() {
    }

    public static void enableKillAura() {
        KillAura killAura = Modules.get().get(KillAura.class);
        if (!killAura.isActive()) {
            ChatUtils.info("开启杀戮", (Object[])new Object[0]);
            KillAuraAccessor killAuraAccessor = (KillAuraAccessor)killAura;
            killAuraAccessor.getAutoSwitch().set(true);
            killAuraAccessor.getSwapBack().set(false);
            killAura.toggle();
        }
    }

    public static void disableKillAura() {
        KillAura killAura = (KillAura)Modules.get().get(KillAura.class);
        if (killAura.isActive()) {
            ChatUtils.info("关闭杀戮", (Object[])new Object[0]);
            killAura.toggle();
        }
    }

    public static void enableFreeLook() {
        FreeLook freeLook = Modules.get().get(FreeLook.class);
        if (!freeLook.isActive()) {
            if (freeLook.mode.get() != FreeLook.Mode.Camera) {
                freeLook.mode.set(FreeLook.Mode.Camera);
            }
            freeLook.toggle();
        }
    }

    public static void disableFreeLook() {
        FreeLook freeLook = Modules.get().get(FreeLook.class);
        if (freeLook.isActive()) {
            freeLook.toggle();
        }
    }

    public static void startPrinter() {
        LitematicaPrinter printer = (LitematicaPrinter)Modules.get().get(LitematicaPrinter.class);
        printer.startPrinting();
    }

    public static void stopPrinter() {
        LitematicaPrinter printer = (LitematicaPrinter)Modules.get().get(LitematicaPrinter.class);
        if (printer.isActive()) {
            printer.toggle();
        }
    }
}
