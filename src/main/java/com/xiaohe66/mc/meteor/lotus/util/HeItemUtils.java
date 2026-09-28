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
    public static final List<Item> CARPETS = Items.CARPET.asList();

    public static boolean isShulkerBox(Item item) {
        return item == Items.SHULKER_BOX || Items.DYED_SHULKER_BOX.asList().contains(item);
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
        return item == Items.BUNDLE || Items.DYED_BUNDLE.asList().contains(item);
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

