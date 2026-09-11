/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.utils.misc.ISerializable
 *  net.minecraft.item.Item
 *  net.minecraft.item.Items
 *  net.minecraft.nbt.NbtCompound
 *  net.minecraft.nbt.NbtList
 *  net.minecraft.nbt.NbtElement
 *  net.minecraft.util.Identifier
 *  net.minecraft.registry.Registries
 */
package com.xiaohe66.mc.meteor.lotus.modules.clearup;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ClearUpMapping implements ISerializable<ClearUpMapping> {
    private Item targetItem;
    private Set<Item> relatedItems;
    private boolean enabled;

    public ClearUpMapping(Item targetItem, Collection<Item> relatedItems, boolean enabled) {
        this.targetItem = targetItem;
        this.relatedItems = new HashSet<Item>(relatedItems);
        this.enabled = enabled;
    }

    public ClearUpMapping() {
        this(Items.AIR, new HashSet<Item>(), true);
    }

    public Item getTargetItem() {
        return this.targetItem;
    }

    public void setTargetItem(Item targetItem) {
        this.targetItem = targetItem;
    }

    public Set<Item> getRelatedItems() {
        return this.relatedItems;
    }

    public void setRelatedItems(Collection<Item> relatedItems) {
        this.relatedItems = new HashSet<Item>(relatedItems);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("targetItem", BuiltInRegistries.ITEM.getKey(this.targetItem).toString());
        ListTag list = new ListTag();
        for (Item item : this.relatedItems) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
            list.add(itemTag);
        }
        tag.put("relatedItems", (Tag)list);
        tag.putBoolean("enabled", this.enabled);
        return tag;
    }

    public ClearUpMapping fromTag(CompoundTag tag) {
        Identifier id = Identifier.tryParse((String)tag.getStringOr("targetItem", ""));
        this.targetItem = id != null ? (Item)BuiltInRegistries.ITEM.getValue(id) : Items.AIR;
        this.relatedItems = new HashSet<Item>();
        ListTag list = tag.getListOrEmpty("relatedItems");
        for (Tag e : list) {
            CompoundTag itemTag;
            Identifier itemId;
            if (e.getId() != 10 || (itemId = Identifier.tryParse((String)(itemTag = (CompoundTag)e).getStringOr("id", ""))) == null) continue;
            this.relatedItems.add((Item)BuiltInRegistries.ITEM.getValue(itemId));
        }
        this.enabled = tag.getBooleanOr("enabled", true);
        return this;
    }
}

