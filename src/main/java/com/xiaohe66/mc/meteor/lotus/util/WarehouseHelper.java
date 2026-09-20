package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.clearup.ClearUpMapping;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WarehouseHelper {
    private Set<Integer> lockedSlots = Collections.emptySet();
    private Map<Item, ClearUpMapping> mappings;
    private Map<ItemBo, StoragePos> allPositions;
    private final Map<ItemBo, StoragePos> mappedPositions = new HashMap<>();
    private final Map<Item, Item> mappingItems = new HashMap<>();
    private final Map<ItemBo, StoragePos> unmappedPositions = new HashMap<>();
    private final Map<Item, StoragePos> mappedItemPositions = new HashMap<>();

    public void init(int range, Map<Item, ClearUpMapping> mappings) {
        this.mappings = mappings;
        Map<ItemBo, StoragePos> scanResult = HePosUtils.scanFrames(range, 4);
        this.mappedPositions.clear();
        for (Map.Entry<ItemBo, StoragePos> entry : scanResult.entrySet()) {
            ItemBo itemBo = entry.getKey();
            if (HeItemUtils.isShulkerBox(itemBo.getItem())) {
                this.mappedPositions.put(itemBo, entry.getValue());
            }
        }
        for (ItemBo itemBo : this.mappedPositions.keySet()) {
            scanResult.remove(itemBo);
        }
        this.mappedItemPositions.clear();
        this.mappingItems.clear();
        for (ClearUpMapping mapping : mappings.values()) {
            if (mapping.isEnabled()) {
                StoragePos pos = scanResult.get(ItemBo.of(mapping.getTargetItem()));
                if (pos != null) {
                    for (Item related : mapping.getRelatedItems()) {
                        this.mappedItemPositions.put(related, pos);
                        this.mappingItems.put(related, mapping.getTargetItem());
                    }
                }
            }
        }
        this.unmappedPositions.clear();
        for (Map.Entry<ItemBo, StoragePos> entry : scanResult.entrySet()) {
            ItemBo itemBo = entry.getKey();
            if (!mappings.containsKey(itemBo.getItem())) {
                this.unmappedPositions.put(itemBo, entry.getValue());
            }
        }
        this.allPositions = scanResult;
        this.lockedSlots = LotusUtils.getLockedSlots();
    }

    /** Find the nearest unmapped position whose item is in the given set. */
    public ItemBo findNearestUnmapped(Set<ItemBo> itemBos) {
        var playerPos = MeteorClient.mc.player.position();
        double minDistance = Double.MAX_VALUE;
        ItemBo nearest = null;
        for (ItemBo itemBo : itemBos) {
            StoragePos pos = this.unmappedPositions.get(itemBo);
            if (pos != null) {
                double distance = playerPos.distanceTo(pos.getBtnPos().getCenter());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = itemBo;
                }
            }
        }
        return nearest;
    }

    /** Find the nearest mapped position whose related item is in the given set. */
    public ItemBo findNearestMapped(Set<ItemBo> itemBos) {
        var playerPos = MeteorClient.mc.player.position();
        double minDistance = Double.MAX_VALUE;
        ItemBo nearest = null;
        for (ItemBo itemBo : itemBos) {
            StoragePos pos = this.mappedItemPositions.get(itemBo.getItem());
            if (pos != null) {
                double distance = playerPos.distanceTo(pos.getBtnPos().getCenter());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = itemBo;
                }
            }
        }
        return nearest;
    }

    public StoragePos getUnmappedPosition(ItemBo itemBo) {
        return this.unmappedPositions.get(itemBo);
    }

    public StoragePos getMappedPosition(ItemBo itemBo) {
        return this.mappedItemPositions.get(itemBo.getItem());
    }

    public Set<Item> getRelatedItems(Item item) {
        return this.mappings.get(this.mappingItems.get(item)).getRelatedItems();
    }

    public boolean isMapped(ItemBo itemBo) {
        return this.unmappedPositions.containsKey(itemBo) || this.mappedItemPositions.containsKey(itemBo.getItem());
    }

    public boolean isUnlockedSlot(int slot) {
        return !this.lockedSlots.contains(slot);
    }

    public boolean hasConfig() {
        return this.allPositions != null && !this.allPositions.isEmpty();
    }

    public void printIdentified() {
        String names = this.unmappedPositions.keySet().stream().map(ItemBo::getName).collect(Collectors.joining(","));
        ChatUtils.info("成功识别到盒装物品: %s", names);
    }

    public Set<Integer> getLockedSlots() {
        return this.lockedSlots;
    }

    public Map<Item, ClearUpMapping> getMappings() {
        return this.mappings;
    }

    public Map<ItemBo, StoragePos> getAllPositions() {
        return this.allPositions;
    }

    public Map<ItemBo, StoragePos> getMappedPositions() {
        return this.mappedPositions;
    }

    public Map<Item, Item> getMappingItems() {
        return this.mappingItems;
    }

    public Map<ItemBo, StoragePos> getUnmappedPositions() {
        return this.unmappedPositions;
    }

    public Map<Item, StoragePos> getMappedItemPositions() {
        return this.mappedItemPositions;
    }

    public void clear() {
        this.lockedSlots.clear();
        this.mappings = null;
        this.allPositions = null;
        this.mappedPositions.clear();
        this.mappingItems.clear();
        this.unmappedPositions.clear();
        this.mappedItemPositions.clear();
    }
}
