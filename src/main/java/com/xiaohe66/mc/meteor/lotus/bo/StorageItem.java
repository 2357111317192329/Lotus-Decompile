/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraft.item.Items
 */
package com.xiaohe66.mc.meteor.lotus.bo;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class StorageItem {
    private static final Map<ItemBo, StorageItem> itemCache = new HashMap<ItemBo, StorageItem>();
    private static final Map<Set<ItemBo>, StorageItem> itemsCache = new HashMap<Set<ItemBo>, StorageItem>();
    private final Set<ItemBo> items;

    private StorageItem(Set<ItemBo> items) {
        this.items = Set.copyOf(items);
    }

    public Item getItem() {
        return this.items.stream().findFirst().map(ItemBo::getItem).orElse(Items.AIR);
    }

    public Set<ItemBo> getItems() {
        return this.items;
    }

    public static StorageItem valueOf(ItemBo itemBo) {
        return itemCache.computeIfAbsent(itemBo, k -> new StorageItem(Collections.singleton(itemBo)));
    }

    public static StorageItem valueOf(Item item) {
        return StorageItem.valueOf(new ItemBo(item));
    }

    public static StorageItem valueOf(Item ... items) {
        Set<ItemBo> itemBoSet = Arrays.stream(items).map(ItemBo::new).collect(Collectors.toSet());
        if (itemBoSet.size() == 1) {
            return StorageItem.valueOf((ItemBo)itemBoSet.iterator().next());
        }
        return itemsCache.computeIfAbsent(itemBoSet, k -> new StorageItem(itemBoSet));
    }

    public static StorageItem valueOf(ItemBo ... itemBos) {
        Set<ItemBo> itemBoSet = Set.of(itemBos);
        if (itemBoSet.size() == 1) {
            return StorageItem.valueOf(itemBoSet.iterator().next());
        }
        return itemsCache.computeIfAbsent(itemBoSet, k -> new StorageItem(itemBoSet));
    }

    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        StorageItem that = (StorageItem)o;
        return this.items.equals(that.items);
    }

    public int hashCode() {
        return this.items.hashCode();
    }
}
