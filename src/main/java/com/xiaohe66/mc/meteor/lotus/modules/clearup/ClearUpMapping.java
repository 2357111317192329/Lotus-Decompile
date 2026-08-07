package com.xiaohe66.mc.meteor.lotus.modules.clearup;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class ClearUpMapping implements ISerializable<ClearUpMapping> {
   private Item targetItem;
   private Set<Item> relatedItems;
   private boolean enabled;

   public ClearUpMapping(Item targetItem, Collection<Item> relatedItems, boolean enabled) {
      this.targetItem = targetItem;
      this.relatedItems = new HashSet<>(relatedItems);
      this.enabled = enabled;
   }

   public ClearUpMapping() {
      this(Items.AIR, new HashSet<>(), true);
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
      this.relatedItems = new HashSet<>(relatedItems);
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public NbtCompound toTag() {
      NbtCompound tag = new NbtCompound();
      tag.putString("targetItem", Registries.ITEM.getId(this.targetItem).toString());
      NbtList list = new NbtList();

      for (Item item : this.relatedItems) {
         NbtCompound itemTag = new NbtCompound();
         itemTag.putString("id", Registries.ITEM.getId(item).toString());
         list.add(itemTag);
      }

      tag.put("relatedItems", list);
      tag.putBoolean("enabled", this.enabled);
      return tag;
   }

   public ClearUpMapping fromTag(NbtCompound tag) {
      Identifier id = Identifier.tryParse(tag.getString("targetItem", ""));
      this.targetItem = id != null ? (Item)Registries.ITEM.get(id) : Items.AIR;
      this.relatedItems = new HashSet<>();

      for (NbtElement e : tag.getListOrEmpty("relatedItems")) {
         if (e.getType() == 10) {
            NbtCompound itemTag = (NbtCompound)e;
            Identifier itemId = Identifier.tryParse(itemTag.getString("id", ""));
            if (itemId != null) {
               this.relatedItems.add((Item)Registries.ITEM.get(itemId));
            }
         }
      }

      this.enabled = tag.getBoolean("enabled", true);
      return this;
   }
}
