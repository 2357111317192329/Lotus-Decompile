/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.modules.spiral;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.modules.spiral.MosquitoCoilData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MosquitoCoilStateManager {
    private static final Logger log = LoggerFactory.getLogger(MosquitoCoilStateManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save(String serverIdentifier, String dimensionId, MosquitoCoilData data) {
        if (data == null || serverIdentifier == null || serverIdentifier.isBlank() || dimensionId == null || dimensionId.isBlank()) {
            return;
        }
        try {
            Path dir = MosquitoCoilStateManager.getSaveDir(serverIdentifier);
            Files.createDirectories(dir, new FileAttribute[0]);
            Path file = dir.resolve(MosquitoCoilStateManager.sanitizeFileName(dimensionId) + ".json");
            Files.writeString(file, GSON.toJson(data), new OpenOption[0]);
        }
        catch (IOException e) {
            log.error("保存蚊香扫图状态失败", (Throwable)e);
        }
    }

    public static MosquitoCoilData load(String serverIdentifier, String dimensionId) {
        if (serverIdentifier == null || serverIdentifier.isBlank() || dimensionId == null || dimensionId.isBlank()) {
            return null;
        }
        try {
            Path file = MosquitoCoilStateManager.getSaveDir(serverIdentifier).resolve(MosquitoCoilStateManager.sanitizeFileName(dimensionId) + ".json");
            if (!Files.exists(file, new LinkOption[0])) {
                return null;
            }
            String json = Files.readString(file);
            return (MosquitoCoilData)GSON.fromJson(json, MosquitoCoilData.class);
        }
        catch (IOException e) {
            log.error("加载蚊香扫图状态失败", (Throwable)e);
            return null;
        }
    }

    public static void delete(String serverIdentifier, String dimensionId) {
        if (serverIdentifier == null || serverIdentifier.isBlank() || dimensionId == null || dimensionId.isBlank()) {
            return;
        }
        try {
            Path file = MosquitoCoilStateManager.getSaveDir(serverIdentifier).resolve(MosquitoCoilStateManager.sanitizeFileName(dimensionId) + ".json");
            Files.deleteIfExists(file);
        }
        catch (IOException e) {
            log.error("删除蚊香扫图状态失败", (Throwable)e);
        }
    }

    private static Path getSaveDir(String serverIdentifier) {
        String sanitized = serverIdentifier.replaceAll("[\\\\/:*?\"<>|]", "_");
        return Const.LOTUS_DIR.resolve("search").resolve(sanitized);
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
