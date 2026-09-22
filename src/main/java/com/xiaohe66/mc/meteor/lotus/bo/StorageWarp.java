/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package com.xiaohe66.mc.meteor.lotus.bo;

import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class StorageWarp {
    private final ItemStack[] stacks;
    private final ShulkerBoxReader[] readers;
    private final Set<ItemBo> items = new HashSet<ItemBo>();
    private Map<ItemBo, Integer> itemCounts;
    private Map<Item, Map<ItemBo, ItemStack>> shulkerStacks;
    private ItemBo bestItem;
    private ItemStack cachedStack;

    public StorageWarp(ItemStack[] stacks) {
        this.stacks = stacks;
        this.readers = new ShulkerBoxReader[stacks.length];
    }

    public int getSize() {
        return this.stacks.length;
    }

    public ItemStack getStack(int index) {
        return this.stacks[index];
    }

    public ItemStack getStack() {
        if (this.cachedStack != null) {
            return this.cachedStack;
        }
        ItemBo itemBo = this.getBestItem();
        ItemStack itemStack = ItemStack.EMPTY;
        Item item = itemBo.getItem();
        if (HeItemUtils.isShulkerBox(item)) {
            this.initShulkerBoxes();
            Map<ItemBo, ItemStack> map = this.shulkerStacks.get(item);
            double bestRatio = 0.0;
            Map.Entry<ItemBo, ItemStack> entry = null;
            for (Map.Entry<ItemBo, ItemStack> entry2 : map.entrySet()) {
                double ratio = (double)entry2.getValue().getCount() * 1.0 / (double)entry2.getKey().getItem().getMaxCount();
                if (!(ratio > bestRatio)) continue;
                bestRatio = ratio;
                entry = entry2;
            }
            if (entry != null) {
                itemStack = (ItemStack)entry.getValue();
            }
        } else {
            for (ItemStack itemStack2 : this.stacks) {
                ItemBo candidate;
                if (itemStack2.isEmpty() || !itemBo.equals(candidate = new ItemBo(itemStack2))) continue;
                if (itemStack.isEmpty()) {
                    itemStack = itemStack2.copy();
                    continue;
                }
                itemStack.setCount(itemStack.getCount() + itemStack2.getCount());
            }
        }
        this.cachedStack = itemStack;
        return this.cachedStack;
    }

    public boolean contains(ItemBo itemBo) {
        this.initItemCounts();
        this.initShulkerBoxes();
        return this.items.contains(itemBo);
    }

    public ItemBo getBestItem() {
        if (this.bestItem == null) {
            this.initItemCounts();
            if (this.itemCounts.isEmpty()) {
                return null;
            }
            double bestRatio = 0.0;
            Map.Entry<ItemBo, Integer> entry = null;
            for (Map.Entry<ItemBo, Integer> entry2 : this.itemCounts.entrySet()) {
                double ratio = entry2.getValue().doubleValue() / (double)entry2.getKey().getItem().getMaxCount();
                if (!(ratio > bestRatio)) continue;
                bestRatio = ratio;
                entry = entry2;
            }
            this.bestItem = entry == null ? null : (ItemBo)entry.getKey();
        }
        return this.bestItem;
    }

    private void initShulkerBoxes() {
        if (this.shulkerStacks != null) {
            return;
        }
        this.shulkerStacks = new HashMap<Item, Map<ItemBo, ItemStack>>();
        for (int i = 0; i < this.stacks.length; ++i) {
            ItemStack itemStack = this.stacks[i];
            Item shulkerItem = itemStack.getItem();
            if (!HeItemUtils.isShulkerBox(shulkerItem)) continue;
            Map map = this.shulkerStacks.computeIfAbsent(shulkerItem, key -> new HashMap());
            if (this.readers[i] == null) {
                this.readers[i] = new ShulkerBoxReader(itemStack);
            }
            ShulkerBoxReader shulkerBoxReader = this.readers[i];
            for (ItemStack condensedStack : shulkerBoxReader.getCondensed()) {
                ItemBo itemBo = new ItemBo(condensedStack);
                if (map.containsKey(itemBo)) {
                    ItemStack existingStack = (ItemStack)map.get(itemBo);
                    existingStack.setCount(existingStack.getCount() + condensedStack.getCount());
                    continue;
                }
                map.put(itemBo, condensedStack.copy());
                this.items.add(itemBo);
            }
        }
    }

    private void initItemCounts() {
        if (this.itemCounts != null) {
            return;
        }
        this.itemCounts = new LinkedHashMap<ItemBo, Integer>();
        for (ItemStack itemStack : this.stacks) {
            if (itemStack.isEmpty()) continue;
            ItemBo itemBo = new ItemBo(itemStack);
            this.itemCounts.merge(itemBo, itemStack.getCount(), Integer::sum);
            this.items.add(itemBo);
        }
    }
}