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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

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
        MapItemSavedData mapState = MapItem.getSavedData((ItemStack)itemStack, (Level)MeteorClient.mc.level);
        return mapState.locked == locked;
    }

    public static boolean isBundle(Item item) {
        return item == Items.BUNDLE || item == Items.WHITE_BUNDLE || item == Items.ORANGE_BUNDLE || item == Items.MAGENTA_BUNDLE || item == Items.LIGHT_BLUE_BUNDLE || item == Items.YELLOW_BUNDLE || item == Items.LIME_BUNDLE || item == Items.PINK_BUNDLE || item == Items.GRAY_BUNDLE || item == Items.LIGHT_GRAY_BUNDLE || item == Items.CYAN_BUNDLE || item == Items.PURPLE_BUNDLE || item == Items.BLUE_BUNDLE || item == Items.BROWN_BUNDLE || item == Items.GREEN_BUNDLE || item == Items.RED_BUNDLE || item == Items.BLACK_BUNDLE;
    }

    public static boolean allAir(Item... items) {
        for (Item item : items) {
            if (item != Items.AIR) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNetheriteArmor(Item item) {
        return item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS;
    }

    public static boolean isDiamondArmor(Item item) {
        return item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS;
    }
}

