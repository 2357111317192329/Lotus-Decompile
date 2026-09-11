/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.item.FilledMapItem
 *  net.minecraft.world.World
 *  net.minecraft.item.map.MapState
 */
package com.xiaohe66.mc.meteor.lotus.util;

import java.util.Arrays;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.FilledMapItem;
import net.minecraft.world.World;
import net.minecraft.item.map.MapState;

public class HeItemUtils {
    public static final List<Item> CARPETS = Arrays.asList(Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET, Items.LIGHT_BLUE_CARPET, Items.YELLOW_CARPET, Items.LIME_CARPET, Items.PINK_CARPET, Items.GRAY_CARPET, Items.LIGHT_GRAY_CARPET, Items.CYAN_CARPET, Items.PURPLE_CARPET, Items.BLUE_CARPET, Items.BROWN_CARPET, Items.GREEN_CARPET, Items.RED_CARPET, Items.BLACK_CARPET);

    public static boolean isShulkerBox(Item item) {
        return item == Items.SHULKER_BOX || item == Items.WHITE_SHULKER_BOX || item == Items.ORANGE_SHULKER_BOX || item == Items.MAGENTA_SHULKER_BOX || item == Items.LIGHT_BLUE_SHULKER_BOX || item == Items.YELLOW_SHULKER_BOX || item == Items.LIME_SHULKER_BOX || item == Items.PINK_SHULKER_BOX || item == Items.GRAY_SHULKER_BOX || item == Items.LIGHT_GRAY_SHULKER_BOX || item == Items.CYAN_SHULKER_BOX || item == Items.PURPLE_SHULKER_BOX || item == Items.BLUE_SHULKER_BOX || item == Items.BROWN_SHULKER_BOX || item == Items.GREEN_SHULKER_BOX || item == Items.RED_SHULKER_BOX || item == Items.BLACK_SHULKER_BOX;
    }

    public static boolean isCarpet(Item item) {
        return CARPETS.contains(item);
    }

    public static boolean isMapLocked(ItemStack itemStack, boolean locked) {
        if (itemStack.getItem() != Items.FILLED_MAP) {
            return false;
        }
        MapState mapState = FilledMapItem.getMapState((ItemStack)itemStack, (World)MeteorClient.mc.world);
        return mapState.locked == locked;
    }
}

