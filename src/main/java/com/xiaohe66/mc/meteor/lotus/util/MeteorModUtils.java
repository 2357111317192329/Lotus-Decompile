/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.addons.MeteorAddon
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  net.fabricmc.loader.api.FabricLoader
 *  net.fabricmc.loader.api.entrypoint.EntrypointContainer
 *  net.fabricmc.loader.api.metadata.ModMetadata
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.util;


import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteorModUtils {
    private static final Logger log = LoggerFactory.getLogger(MeteorModUtils.class);
    private static final Map<String, String> packageToModIdMap = new HashMap<String, String>();
    private static final Map<Class<?>, String> classToModIdMap = new HashMap();

    public static String getModId(Module module) {
        if (module == null) {
            return "meteor";
        }
        return MeteorModUtils.getModId(module.addon);
    }

    public static String getModId(MeteorAddon meteorAddon) {
        if (meteorAddon == null) {
            return "meteor";
        }
        return MeteorModUtils.getModId(meteorAddon.getPackage());
    }

    public static String getModId(String packageName) {
        if (packageToModIdMap.isEmpty()) {
            MeteorModUtils.initModIdMap();
        }
        return packageToModIdMap.getOrDefault(packageName, "meteor");
    }

    public static String getModId(Class<?> entrypointClass) {
        return classToModIdMap.computeIfAbsent(entrypointClass, clazz -> {
            for (EntrypointContainer entrypointContainer : FabricLoader.getInstance().getEntrypointContainers("meteor", clazz)) {
                try {
                    return entrypointContainer.getProvider().getMetadata().getId();
                }
                catch (Exception e) {
                    log.warn("Failed to map addon to mod id", (Throwable)e);
                }
            }
            return "other";
        });
    }

    private static void initModIdMap() {
        for (EntrypointContainer entrypointContainer : FabricLoader.getInstance().getEntrypointContainers("meteor", MeteorAddon.class)) {
            try {
                MeteorAddon meteorAddon = (MeteorAddon)entrypointContainer.getEntrypoint();
                if ("meteordevelopment.meteorclient".equals(meteorAddon.getPackage())) continue;
                ModMetadata modMetadata = entrypointContainer.getProvider().getMetadata();
                packageToModIdMap.put(meteorAddon.getPackage(), modMetadata.getId());
            }
            catch (Exception e) {
                log.warn("Failed to map addon to mod id", (Throwable)e);
            }
        }
    }
}
