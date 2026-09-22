package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.mixin.KillAuraAccessor;
import com.xiaohe66.mc.meteor.lotus.modules.Printer;
import com.xiaohe66.mc.meteor.lotus.modules.LotusHelper;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.ServerInfo;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public class LotusUtils {
    private static String serverId = "none";
    private static String worldId = "none";
    private static Set<Integer> lockedSlots;

    private LotusUtils() {
    }

    public static String getWorldId() {
        if (MeteorClient.mc.world != null) {
            worldId = MeteorClient.mc.world.getRegistryKey().getValue().toString();
        }
        return worldId;
    }

    public static String getServerId() {
        return serverId;
    }

    public static void setServerId(boolean useAddress) {
        ServerInfo serverData = MeteorClient.mc.getCurrentServerEntry();
        if (serverData != null) {
            if (useAddress) {
                String invalidChars = "[\\\\/:*?\"<>|]";
                serverId = serverData.address.replaceAll(invalidChars, "_");
            } else {
                serverId = serverData.name;
            }
        } else if (MeteorClient.mc.getServer() != null) {
            serverId = MeteorClient.mc.getServer().getSaveProperties().getLevelName();
        } else {
            serverId = "other";
        }
    }

    public static void openFolder(File folder) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"explorer.exe", folder.getAbsolutePath()});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", folder.getAbsolutePath()});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", folder.getAbsolutePath()});
            }
        } catch (IOException e) {
            ChatUtils.error("打开文件夹失败 : %s", e.getMessage());
        }
    }

    public static Set<Integer> getLockedSlots() {
        if (lockedSlots == null) {
            LotusHelper lotusHelper = Modules.get().get(LotusHelper.class);
            lockedSlots = lotusHelper.getLockedSlots();
        }
        return lockedSlots;
    }

    public static boolean isLockedSlot(int slot) {
        return getLockedSlots().contains(slot);
    }

    public static void enableKillAura() {
        KillAura killAura = Modules.get().get(KillAura.class);
        if (!killAura.isActive()) {
            ChatUtils.info("开启杀戮", new Object[0]);
            KillAuraAccessor killAuraAccessor = (KillAuraAccessor)killAura;
            killAuraAccessor.getAutoSwitch().set(true);
            killAuraAccessor.getSwapBack().set(false);
            killAura.toggle();
        }
    }

    public static void disableKillAura() {
        KillAura killAura = Modules.get().get(KillAura.class);
        if (killAura.isActive()) {
            ChatUtils.info("关闭杀戮", new Object[0]);
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
        Printer printer = Modules.get().get(Printer.class);
        printer.startPrinting();
    }

    public static void stopPrinter() {
        Printer printer = Modules.get().get(Printer.class);
        if (printer.isActive()) {
            printer.toggle();
        }
    }
}
