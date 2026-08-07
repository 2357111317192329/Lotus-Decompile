package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.world.item.component.ItemContainerContents;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class HeInvUtils {
   public static final Minecraft mc = Minecraft.getInstance();
   public static final int MAX_SLOT = 36;

   public static void closeCurScreen() {
      if (!(mc.player.containerMenu instanceof InventoryMenu)) {
         mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
         mc.player.closeContainer();
      }
   }

   public static FindItemResult findAndMoveHotbar(Item item) {
      FindItemResult enderChestResult = InvUtils.findInHotbar(new Item[]{item});
      if (enderChestResult.slot() != -1) {
         return enderChestResult;
      }

      enderChestResult = InvUtils.find(new Item[]{item});
      int enderChestSlot = enderChestResult.slot();
      if (enderChestSlot == -1) {
         return null;
      }

      int mainSlot = getMainSlot();
      InvUtils.move().from(enderChestResult.slot()).to(mainSlot);
      return InvUtils.findInHotbar(new Item[]{item});
   }

   public static FindItemResult findShulkerBoxInHotBar(Item item) {
      return InvUtils.find(itemStack -> hasItem(item, itemStack), 0, 9);
   }

   public static FindItemResult findShulkerBoxNotEmpty() {
      return InvUtils.find(itemStack -> {
         if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
            ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
            return !reader.isEmpty();
         } else {
            return false;
         }
      }, 0, 36);
   }

   public static FindItemResult findShulkerBox() {
      return InvUtils.find(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()), 0, 36);
   }

   public static FindItemResult findShulkerBox(Item item) {
      return InvUtils.find(itemStack -> hasItem(item, itemStack), 0, 36);
   }

   public static List<ItemStack> findAndMargeShulkerBox() {
      List<ItemStack> kitItemStackList = mc.player
         .containerMenu
         .slots
         .stream()
         .<ItemStack>map(Slot::getItem)
         .filter(Objects::nonNull)
         .filter(itemStackx -> HeItemUtils.isShulkerBox(itemStackx.getItem()))
         .toList();
      Map<String, ItemStack> map = new LinkedHashMap<>();

      for (ItemStack kitItemStack : kitItemStackList) {
         DataComponentMap components = kitItemStack.getComponents();
         ItemContainerContents container = components.get(DataComponents.CONTAINER);
         NonNullList<ItemStack> stacks = NonNullList.create();
         container.copyInto(stacks);
         String key = stacks.toString();
         if (map.containsKey(key)) {
            ItemStack itemStack = map.get(key);
            itemStack.setCount(itemStack.getCount() + 1);
         } else {
            map.put(key, kitItemStack.copy());
         }
      }

      List<ItemStack> itemStacks = new ArrayList<>(map.values());
      itemStacks.sort((o1, o2) -> Integer.compare(o2.getCount(), o1.getCount()));
      return itemStacks;
   }

   private static boolean hasItem(Item item, ItemStack itemStack) {
      if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
         ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
         return item == Items.AIR ? reader.isEmpty() : reader.hasItem(new ItemBo(item));
      } else {
         return false;
      }
   }

   public static boolean isHotbar(int slot) {
      return slot >= 0 && slot <= 8;
   }

   public static int findBookSlot(ResourceKey<Enchantment> enchantment) {
      Inventory playerInventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack bookItemStack = playerInventory.getItem(i);
         if (bookItemStack != null && bookItemStack.getItem() == Items.ENCHANTED_BOOK) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            if (bookEnchantMentSet.contains(enchantment)) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findBookSlot(ResourceKey<Enchantment> enchantment, ResourceKey<Enchantment> enchantment2) {
      Inventory playerInventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack bookItemStack = playerInventory.getItem(i);
         if (bookItemStack != null && bookItemStack.getItem() == Items.ENCHANTED_BOOK) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            if (bookEnchantMentSet.contains(enchantment) && bookEnchantMentSet.contains(enchantment2)) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findBookSlot(ResourceKey<Enchantment>... enchantmentArr) {
      Set<ResourceKey<Enchantment>> enchantmentSet = new HashSet<>(Arrays.asList(enchantmentArr));
      return findBookSlot(enchantmentSet);
   }

   public static int findBookSlot(Set<ResourceKey<Enchantment>> enchantmentSet) {
      Inventory playerInventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack bookItemStack = playerInventory.getItem(i);
         if (bookItemStack != null && bookItemStack.getItem() == Items.ENCHANTED_BOOK) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            boolean exist = true;

            for (ResourceKey<Enchantment> enchantment : enchantmentSet) {
               if (!bookEnchantMentSet.contains(enchantment)) {
                  exist = false;
                  break;
               }
            }

            if (exist) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findBookSlotInChest(ChestMenu screenHandler, ResourceKey<Enchantment> enchantment) {
      Container inventory = screenHandler.getContainer();
      int slotId = 0;

      for (int n = inventory.getContainerSize(); slotId < n; slotId++) {
         ItemStack bookItemStack = screenHandler.getSlot(slotId).getItem();
         if (bookItemStack != null && bookItemStack.getItem() == Items.ENCHANTED_BOOK) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            if (bookEnchantMentSet.contains(enchantment)) {
               return slotId;
            }
         }
      }

      return -1;
   }

   public static int findEquipSlotInChest(Container inventory, Item item) {
      int slotId = 0;

      for (int n = inventory.getContainerSize(); slotId < n; slotId++) {
         ItemStack bookItemStack = inventory.getItem(slotId);
         if (bookItemStack != null && bookItemStack.getItem() == item) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            if (bookEnchantMentSet.isEmpty()) {
               return slotId;
            }
         }
      }

      return -1;
   }

   public static int findItemSlot(Item item) {
      Inventory inventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack itemStack = inventory.getItem(i);
         if (itemStack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public static int findFullItemSlot(Item item) {
      Inventory inventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack itemStack = inventory.getItem(i);
         if (itemStack.getItem() == item && itemStack.getCount() == itemStack.getMaxStackSize()) {
            return i;
         }
      }

      return -1;
   }

   public static boolean findItemAndSwitch(Item item) {
      int slot = findItemSlot(item);
      if (slot < 0) {
         return false;
      }

      swapToSlot(slot);
      return true;
   }

   public static void swapToSlot(int slot) {
      if (mc.player.getInventory().getSelectedSlot() != slot) {
         mc.player.getInventory().setSelectedSlot(slot);
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
      }
   }

   public static void swapMainHand(int formSlot) {
      if (isHotbar(formSlot)) {
         swapToSlot(formSlot);
      } else {
         boolean needSwap = !InvUtils.testInMainHand(new Item[]{Items.AIR});
         InvUtils.move().from(formSlot).to(getMainSlot());
         if (needSwap) {
            InvUtils.click().to(formSlot);
         }
      }
   }

   public static void swap(int formSlot, int toSlot) {
      if (isHotbar(formSlot)) {
         swapToSlot(formSlot);
      } else {
         ItemStack itemStack = mc.player.getInventory().getItem(formSlot);
         boolean needSwap = !itemStack.isEmpty();
         InvUtils.move().from(formSlot).to(toSlot);
         if (needSwap) {
            InvUtils.click().to(formSlot);
         }
      }
   }

   public static boolean swap(Item item) {
      FindItemResult enderChestResult = InvUtils.findInHotbar(new Item[]{item});
      if (enderChestResult.slot() != -1) {
         return InvUtils.swap(enderChestResult.slot(), true);
      }

      enderChestResult = InvUtils.find(new Item[]{item});
      int enderChestSlot = enderChestResult.slot();
      if (enderChestSlot == -1) {
         return false;
      }

      int mainSlot = getMainSlot();
      InvUtils.move().from(enderChestResult.slot()).to(mainSlot);
      return true;
   }

   public static boolean isKitInMainHand() {
      return InvUtils.testInMainHand(
         new Item[]{
            Items.SHULKER_BOX,
            Items.WHITE_SHULKER_BOX,
            Items.ORANGE_SHULKER_BOX,
            Items.MAGENTA_SHULKER_BOX,
            Items.LIGHT_BLUE_SHULKER_BOX,
            Items.YELLOW_SHULKER_BOX,
            Items.LIME_SHULKER_BOX,
            Items.PINK_SHULKER_BOX,
            Items.GRAY_SHULKER_BOX,
            Items.LIGHT_GRAY_SHULKER_BOX,
            Items.CYAN_SHULKER_BOX,
            Items.PURPLE_SHULKER_BOX,
            Items.BLUE_SHULKER_BOX,
            Items.BROWN_SHULKER_BOX,
            Items.GREEN_SHULKER_BOX,
            Items.RED_SHULKER_BOX,
            Items.BLACK_SHULKER_BOX
         }
      );
   }

   public static int getMainSlot() {
      return mc.player.getInventory().getSelectedSlot();
   }
}
