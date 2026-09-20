/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  */
package com.xiaohe66.mc.meteor.lotus.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModsCheck {
    private static final Logger log = LoggerFactory.getLogger(ModsCheck.class);
    private static Boolean litematica;
    private static Boolean baritone;
    private static Boolean caffeine;

    public static boolean hasLitematica() {
        if (litematica == null) {
            try {
                Class.forName("fi.dy.masa.litematica.world.SchematicWorldHandler");
                litematica = true;
            }
            catch (ClassNotFoundException e) {
                log.warn("缺少<投影>");
                litematica = false;
            }
        }
        return litematica;
    }

    public static boolean hasBaritone() {
        if (baritone == null) {
            try {
                Class.forName("baritone.api.BaritoneAPI");
                baritone = true;
            }
            catch (ClassNotFoundException e) {
                log.warn("缺少<男中音>");
                baritone = false;
            }
        }
        return baritone;
    }

    public static boolean hasCaffeine() {
        if (caffeine == null) {
            try {
                Class.forName("com.github.benmanes.caffeine.cache.Caffeine");
                caffeine = true;
            }
            catch (ClassNotFoundException e) {
                log.warn("缺少<地图三件套>");
                caffeine = false;
            }
        }
        return caffeine;
    }
}