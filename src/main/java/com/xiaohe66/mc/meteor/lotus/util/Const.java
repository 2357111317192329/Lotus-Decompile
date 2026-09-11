/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.systems.modules.Category
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.item.ItemConvertible
 */
package com.xiaohe66.mc.meteor.lotus.util;


import java.nio.file.Path;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;

public class Const {
    public static final String LOTUS_MOD_ID = "lotus";
    public static final String METEOR_MOD_ID = "meteor";
    public static final String OTHER_MOD_ID = "other";
    public static final Category CATEGORY = new Category("Lotus", new ItemStack((ItemConvertible)Items.LILY_PAD));
    public static final Path LOTUS_DIR = FabricLoader.getInstance().getGameDir().resolve("Lotus");
}

