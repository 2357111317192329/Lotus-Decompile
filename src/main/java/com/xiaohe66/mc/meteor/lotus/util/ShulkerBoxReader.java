package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.mixin.ContainerComponentAccessor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;

public class ShulkerBoxReader implements Iterable<ItemStack> {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final ItemStack boxItemStack;
   private ItemStack[] cacheItemStackArr;
   private boolean empty = true;
   private List<ItemStack> condensedList;
   private Set<ItemBo> itemBoSet;

   public ShulkerBoxReader(ItemStack boxItemStack) {
      if (boxItemStack != null && HeItemUtils.isShulkerBox(boxItemStack.getItem())) {
         this.boxItemStack = boxItemStack;
      } else {
         throw new IllegalArgumentException("ItemStack is not a ShulkerBox");
      }
   }

   public boolean isEmpty() {
      this.readItemStackArr();
      return this.empty;
   }

   public boolean hasItem(ItemBo itemBo) {
      for (ItemStack itemStack : this.readItemStackArr()) {
         if (new ItemBo(itemStack).equals(itemBo)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasItemAll(Set<ItemBo> itemBos) {
      if (this.itemBoSet == null) {
         List<ItemStack> condensed = this.getCondensed();
         this.itemBoSet = condensed.stream().map(ItemBo::new).collect(Collectors.toSet());
      }

      for (ItemBo itemBo : itemBos) {
         if (!this.itemBoSet.contains(itemBo)) {
            return false;
         }
      }

      return true;
   }

   public ItemBo getFirstItemBo() {
      for (ItemStack itemStack : this.readItemStackArr()) {
         if (!itemStack.isEmpty()) {
            return new ItemBo(itemStack);
         }
      }

      return new ItemBo(Items.AIR);
   }

   @Deprecated
   public Item getFirstItem() {
      for (ItemStack itemStack : this.readItemStackArr()) {
         if (!itemStack.isEmpty()) {
            return itemStack.getItem();
         }
      }

      return Items.AIR;
   }

   public boolean isSameItem() {
      if (this.isEmpty()) {
         return false;
      }

      Map<ItemBo, Integer> itemQtyMap = this.readItemQtyMap();
      return itemQtyMap.size() == 1;
   }

   public Set<ItemBo> getItemBoSet() {
      return Arrays.stream(this.readItemStackArr()).map(ItemBo::new).collect(Collectors.toSet());
   }

   @Deprecated
   public Set<Item> getItemSet() {
      return Arrays.stream(this.readItemStackArr()).<Item>map(ItemStack::getItem).collect(Collectors.toSet());
   }

   public ItemStack getMaximumItem() {
      Map<ItemBo, Integer> itemQtyMap = this.readItemQtyMap();
      double max = 0.0;
      Entry<ItemBo, Integer> maxentry = null;

      for (Entry<ItemBo, Integer> entry : itemQtyMap.entrySet()) {
         ItemBo newItemBo = entry.getKey();
         double value = entry.getValue().doubleValue() / newItemBo.getItem().getMaxCount();
         if (value > max) {
            max = value;
            maxentry = entry;
         }
      }

      if (maxentry == null) {
         return Items.AIR.getDefaultStack();
      }

      ItemBo targetItemBo = maxentry.getKey();

      for (ItemStack itemStack : this.readItemStackArr()) {
         if (!itemStack.isEmpty() && new ItemBo(itemStack).equals(targetItemBo)) {
            ItemStack result = itemStack.copy();
            result.setCount(maxentry.getValue());
            return result;
         }
      }

      return Items.AIR.getDefaultStack();
   }

   public List<ItemStack> getCondensed() {
      if (this.condensedList == null) {
         ItemStack[] itemStackArr = this.readItemStackArr();
         Map<String, ItemStack> map = new LinkedHashMap<>();

         for (ItemStack itemStack : itemStackArr) {
            if (!itemStack.isEmpty()) {
               ComponentMap nbtElement = itemStack.getComponents();
               String key = itemStack + "_" + nbtElement.toString();
               ItemStack mapItemStack = map.computeIfAbsent(key, k -> itemStack.copyAndEmpty());
               int count = mapItemStack.getCount() + itemStack.getCount();
               mapItemStack.setCount(count);
            }
         }

         this.condensedList = new ArrayList<>(map.values());
      }

      return this.condensedList;
   }

   private Map<ItemBo, Integer> readItemQtyMap() {
      ItemStack[] itemStackArr = this.readItemStackArr();
      Map<ItemBo, Integer> map = new LinkedHashMap<>();

      for (ItemStack itemStack : itemStackArr) {
         if (!itemStack.isEmpty()) {
            ItemBo itemBo = new ItemBo(itemStack);
            Integer qty = map.computeIfAbsent(itemBo, k -> 0);
            map.put(itemBo, qty + itemStack.getCount());
         }
      }

      return map;
   }

   private ItemStack[] readItemStackArr() {
      if (this.cacheItemStackArr == null) {
         ItemStack[] itemStackArr = new ItemStack[27];
         Arrays.fill(itemStackArr, Items.AIR.getDefaultStack());
         ComponentMap components = this.boxItemStack.getComponents();
         if (components.contains(DataComponentTypes.CONTAINER)) {
            ContainerComponentAccessor container = (ContainerComponentAccessor)(Object)components.get(DataComponentTypes.CONTAINER);
            DefaultedList<ItemStack> stacks = container.meteor$getStacks();

            for (int i = 0; i < stacks.size(); i++) {
               ItemStack stack = (ItemStack)stacks.get(i);
               itemStackArr[i] = stack.copy();
               this.empty = false;
            }
         }

         this.cacheItemStackArr = itemStackArr;
      }

      return this.cacheItemStackArr;
   }

   public int getOriginItemStackCount() {
      return this.boxItemStack.getCount();
   }

   public List<ItemStack> getStacks() {
      return Arrays.asList(this.readItemStackArr());
   }

   public int getColor() {
      Item item = this.boxItemStack.getItem();
      if (item == Items.WHITE_SHULKER_BOX) {
         return -393218;
      } else if (item == Items.ORANGE_SHULKER_BOX) {
         return -425955;
      } else if (item == Items.MAGENTA_SHULKER_BOX) {
         return -3715395;
      } else if (item == Items.LIGHT_BLUE_SHULKER_BOX) {
         return -12930086;
      } else if (item == Items.YELLOW_SHULKER_BOX) {
         return -75715;
      } else if (item == Items.LIME_SHULKER_BOX) {
         return -8337633;
      } else if (item == Items.PINK_SHULKER_BOX) {
         return -816214;
      } else if (item == Items.GRAY_SHULKER_BOX) {
         return -12103854;
      } else if (item == Items.LIGHT_GRAY_SHULKER_BOX) {
         return -6447721;
      } else if (item == Items.CYAN_SHULKER_BOX) {
         return -15295332;
      } else if (item == Items.PURPLE_SHULKER_BOX) {
         return -7785800;
      } else if (item == Items.BLUE_SHULKER_BOX) {
         return -12827478;
      } else if (item == Items.BROWN_SHULKER_BOX) {
         return -8170446;
      } else if (item == Items.GREEN_SHULKER_BOX) {
         return -10585066;
      } else if (item == Items.RED_SHULKER_BOX) {
         return -5231066;
      } else {
         return item == Items.BLACK_SHULKER_BOX ? -14869215 : -6728784;
      }
   }

   public boolean matchesTemplate(List<ItemBo> templateItems) {
      ItemStack[] boxItems = this.readItemStackArr();
      int size = Math.min(templateItems.size(), boxItems.length);

      for (int i = 0; i < size; i++) {
         ItemBo templateItemBo = templateItems.get(i);
         ItemBo boxItemBo = new ItemBo(boxItems[i]);
         if (!Objects.equals(templateItemBo, boxItemBo)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public Iterator<ItemStack> iterator() {
      this.readItemStackArr();
      return new Iterator<ItemStack>() {
         int i = 0;

         @Override
         public boolean hasNext() {
            return this.i < ShulkerBoxReader.this.cacheItemStackArr.length;
         }

         public ItemStack next() {
            return ShulkerBoxReader.this.cacheItemStackArr[this.i++].copy();
         }
      };
   }
}
