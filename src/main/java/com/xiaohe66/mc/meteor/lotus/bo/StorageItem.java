package com.xiaohe66.mc.meteor.lotus.bo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class StorageItem {
   private static final Map<ItemBo, StorageItem> itemCache = new HashMap<>();
   private static final Map<Set<ItemBo>, StorageItem> itemsCache = new HashMap<>();
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
      return valueOf(new ItemBo(item));
   }

   public static StorageItem valueOf(Item... items) {
      Set<ItemBo> itemBoSet = Arrays.stream(items).map(ItemBo::new).collect(Collectors.toSet());
      return itemBoSet.size() == 1 ? valueOf(itemBoSet.iterator().next()) : itemsCache.computeIfAbsent(itemBoSet, k -> new StorageItem(itemBoSet));
   }

   public static StorageItem valueOf(ItemBo... itemBos) {
      Set<ItemBo> itemBoSet = Set.of(itemBos);
      return itemBoSet.size() == 1 ? valueOf(itemBoSet.iterator().next()) : itemsCache.computeIfAbsent(itemBoSet, k -> new StorageItem(itemBoSet));
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         StorageItem that = (StorageItem)o;
         return this.items.equals(that.items);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.items.hashCode();
   }
}
