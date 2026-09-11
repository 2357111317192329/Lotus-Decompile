/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.mixin.ContainerComponentAccessor
 *  meteordevelopment.meteorclient.utils.player.FindItemResult
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.meteorclient.utils.player.SlotUtils
 *  net.minecraft.inventory.Inventory
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.entity.player.PlayerInventory
 *  net.minecraft.screen.GenericContainerScreenHandler
 *  net.minecraft.screen.slot.SlotActionType
 *  net.minecraft.screen.PlayerScreenHandler
 *  net.minecraft.screen.slot.Slot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.util.collection.DefaultedList
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
 *  net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.registry.RegistryKey
 *  net.minecraft.component.ComponentMap
 *  net.minecraft.component.DataComponentTypes
 */
package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class HeInvUtils {
    public static final Minecraft mc = Minecraft.getInstance();
    public static final int MAX_SLOT = 36;

    public static void closeCurScreen() {
        if (!(HeInvUtils.mc.player.containerMenu instanceof InventoryMenu)) {
            HeInvUtils.mc.player.connection.send((Packet)new ServerboundContainerClosePacket(HeInvUtils.mc.player.containerMenu.containerId));
            HeInvUtils.mc.player.closeContainer();
        }
    }

    public static FindItemResult findAndMoveHotbar(Item item) {
        FindItemResult enderChestResult = InvUtils.findInHotbar((Item[])new Item[]{item});
        if (enderChestResult.slot() != -1) {
            return enderChestResult;
        }
        enderChestResult = InvUtils.find((Item[])new Item[]{item});
        int enderChestSlot = enderChestResult.slot();
        if (enderChestSlot == -1) {
            return null;
        }
        int mainSlot = HeInvUtils.getMainSlot();
        InvUtils.move().from(enderChestResult.slot()).to(mainSlot);
        return InvUtils.findInHotbar((Item[])new Item[]{item});
    }

    public static FindItemResult findShulkerBoxInHotBar(Item item) {
        return InvUtils.find(itemStack -> HeInvUtils.hasItem(item, itemStack), (int)0, (int)9);
    }

    public static FindItemResult findShulkerBoxNotEmpty() {
        return InvUtils.find(itemStack -> {
            if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                ShulkerBoxReader reader = new ShulkerBoxReader((ItemStack)itemStack);
                return !reader.isEmpty();
            }
            return false;
        }, (int)0, (int)36);
    }

    public static FindItemResult findShulkerBox() {
        return InvUtils.find(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()), (int)0, (int)36);
    }

    public static FindItemResult findShulkerBox(Item item) {
        return InvUtils.find(itemStack -> HeInvUtils.hasItem(item, itemStack), (int)0, (int)36);
    }

    public static List<ItemStack> findAndMargeShulkerBox() {
        List<ItemStack> kitItemStackList = HeInvUtils.mc.player.containerMenu.slots.stream().map(Slot::getItem).filter(Objects::nonNull).filter(itemStackx -> HeItemUtils.isShulkerBox(itemStackx.getItem())).toList();
        LinkedHashMap<String, ItemStack> map = new LinkedHashMap<String, ItemStack>();
        for (ItemStack kitItemStack : kitItemStackList) {
            DataComponentMap components = kitItemStack.getComponents();
            ItemContainerContents container = components.get(DataComponents.CONTAINER);
            NonNullList<ItemStack> stacks = NonNullList.create();
            String key = stacks.toString();
            if (map.containsKey(key)) {
                ItemStack itemStack = (ItemStack)map.get(key);
                itemStack.setCount(itemStack.getCount() + 1);
                continue;
            }
            map.put(key, kitItemStack.copy());
        }
        ArrayList<ItemStack> itemStacks = new ArrayList<ItemStack>(map.values());
        itemStacks.sort((o1, o2) -> Integer.compare(o2.getCount(), o1.getCount()));
        return itemStacks;
    }

    public static boolean isInHotbar(Item item) {
        int slot = HeInvUtils.findItemSlot(item);
        return slot >= 0 && slot <= 8;
    }

    public static boolean hasItem(Item item) {
        return HeInvUtils.findItemSlot(item) != -1;
    }

    private static boolean hasItem(Item item, ItemStack itemStack) {
        if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
            ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
            if (item == Items.AIR) {
                return reader.isEmpty();
            }
            return reader.hasItem(new ItemBo(item));
        }
        return false;
    }

    public static boolean isHotbar(int slot) {
        return slot >= 0 && slot <= 8;
    }

    public static int findBookSlot(ResourceKey<Enchantment> enchantment) {
        Inventory playerInventory = HeInvUtils.mc.player.getInventory();
        for (int i = 0; i < 36; ++i) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet;
            ItemStack bookItemStack = playerInventory.getItem(i);
            if (bookItemStack == null || bookItemStack.getItem() != Items.ENCHANTED_BOOK || !(bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack)).contains(enchantment)) continue;
            return i;
        }
        return -1;
    }

    public static int findBookSlot(ResourceKey<Enchantment> enchantment, ResourceKey<Enchantment> enchantment2) {
        Inventory playerInventory = HeInvUtils.mc.player.getInventory();
        for (int i = 0; i < 36; ++i) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet;
            ItemStack bookItemStack = playerInventory.getItem(i);
            if (bookItemStack == null || bookItemStack.getItem() != Items.ENCHANTED_BOOK || !(bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack)).contains(enchantment) || !bookEnchantMentSet.contains(enchantment2)) continue;
            return i;
        }
        return -1;
    }

    public static int findBookSlot(ResourceKey<Enchantment> ... enchantments) {
        HashSet<ResourceKey<Enchantment>> enchantmentSet = new HashSet<ResourceKey<Enchantment>>(Arrays.asList(enchantments));
        return HeInvUtils.findBookSlot(enchantmentSet);
    }

    public static int findBookSlot(Set<ResourceKey<Enchantment>> enchantmentSet) {
        Inventory playerInventory = HeInvUtils.mc.player.getInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack bookItemStack = playerInventory.getItem(i);
            if (bookItemStack == null || bookItemStack.getItem() != Items.ENCHANTED_BOOK) continue;
            Set<ResourceKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            boolean exist = true;
            for (ResourceKey<Enchantment> enchantment : enchantmentSet) {
                if (bookEnchantMentSet.contains(enchantment)) continue;
                exist = false;
                break;
            }
            if (!exist) continue;
            return i;
        }
        return -1;
    }

    public static int findBookSlotInChest(ChestMenu screenHandler, ResourceKey<Enchantment> enchantment) {
        Container inventory = screenHandler.getContainer();
        int size = inventory.getContainerSize();
        for (int i = 0; i < size; ++i) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet;
            ItemStack bookItemStack = screenHandler.getSlot(i).getItem();
            if (bookItemStack == null || bookItemStack.getItem() != Items.ENCHANTED_BOOK || !(bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack)).contains(enchantment)) continue;
            return i;
        }
        return -1;
    }

    public static int findEquipSlotInChest(Container inventory, Item item) {
        int size = inventory.getContainerSize();
        for (int i = 0; i < size; ++i) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet;
            ItemStack bookItemStack = inventory.getItem(i);
            if (bookItemStack == null || bookItemStack.getItem() != item || !(bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack)).isEmpty()) continue;
            return i;
        }
        return -1;
    }

    public static int findItemSlot(Item item) {
        if (item == Items.AIR) {
            return -1;
        }
        for (int i = 0; i < 45; ++i) {
            Item itemInSlot = HeInvUtils.mc.player.getInventory().getItem(i).getItem();
            if (itemInSlot != item) continue;
            return i;
        }
        return -1;
    }

    public static int findFullItemSlot(Item item) {
        Inventory inventory = HeInvUtils.mc.player.getInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem() != item || itemStack.getCount() != itemStack.getMaxStackSize()) continue;
            return i;
        }
        return -1;
    }

    public static boolean findItemAndSwitch(Item item) {
        int slot = HeInvUtils.findItemSlot(item);
        if (slot < 0) {
            return false;
        }
        HeInvUtils.swapToSlot(slot);
        return true;
    }

    public static void swapToSelectedSlot(int slot) {
        int selectedSlot = HeInvUtils.mc.player.getInventory().getSelectedSlot();
        if (slot != selectedSlot) {
            int slotId = SlotUtils.indexToId((int)slot);
            HeInvUtils.mc.gameMode.handleContainerInput(HeInvUtils.mc.player.containerMenu.containerId, slotId, selectedSlot, ContainerInput.SWAP, (Player)MeteorClient.mc.player);
        }
    }

    public static void swapItemToSelectedSlot(Item item) {
        int slot = HeInvUtils.findItemSlot(item);
        HeInvUtils.swapToSelectedSlot(slot);
    }

    @Deprecated
    public static void swapToSlot(int slot) {
        if (HeInvUtils.mc.player.getInventory().getSelectedSlot() != slot) {
            HeInvUtils.mc.player.getInventory().setSelectedSlot(slot);
            mc.getConnection().send((Packet)new ServerboundSetCarriedItemPacket(slot));
        }
    }

    @Deprecated
    public static void swapMainHand(int slot) {
        if (HeInvUtils.isHotbar(slot)) {
            HeInvUtils.swapToSlot(slot);
        } else {
            boolean wasHoldingItem = !InvUtils.testInMainHand((Item[])new Item[]{Items.AIR});
            InvUtils.move().from(slot).to(HeInvUtils.getMainSlot());
            if (wasHoldingItem) {
                InvUtils.click().to(slot);
            }
        }
    }

    public static void swap(int fromSlot, int toSlot) {
        if (HeInvUtils.isHotbar(fromSlot)) {
            HeInvUtils.swapToSlot(fromSlot);
        } else {
            ItemStack itemStack = HeInvUtils.mc.player.getInventory().getItem(fromSlot);
            boolean wasNotEmpty = !itemStack.isEmpty();
            InvUtils.move().from(fromSlot).to(toSlot);
            if (wasNotEmpty) {
                InvUtils.click().to(fromSlot);
            }
        }
    }

    public static void sendCloseScreenPacket() {
        HeInvUtils.mc.player.connection.send((Packet)new ServerboundContainerClosePacket(HeInvUtils.mc.player.containerMenu.containerId));
    }

    public static void moveOneFromIndex(int fromIndex, int toSlotId) {
        int slotId = SlotUtils.indexToId((int)fromIndex);
        HeInvUtils.moveOneFromSlot(slotId, toSlotId);
    }

    public static void moveHalfFromIndex(int fromIndex, int toSlotId) {
        int slotId = SlotUtils.indexToId((int)fromIndex);
        HeInvUtils.moveHalfFromSlot(slotId, toSlotId);
    }

    public static void moveOneFromSlot(int fromSlotId, int toSlotId) {
        int syncId = HeInvUtils.mc.player.containerMenu.containerId;
        HeInvUtils.mc.gameMode.handleContainerInput(syncId, fromSlotId, 0, ContainerInput.PICKUP, (Player)HeInvUtils.mc.player);
        HeInvUtils.mc.gameMode.handleContainerInput(syncId, toSlotId, 1, ContainerInput.PICKUP, (Player)HeInvUtils.mc.player);
        HeInvUtils.mc.gameMode.handleContainerInput(syncId, fromSlotId, 0, ContainerInput.PICKUP, (Player)HeInvUtils.mc.player);
    }

    public static void moveHalfFromSlot(int fromSlotId, int toSlotId) {
        int syncId = HeInvUtils.mc.player.containerMenu.containerId;
        HeInvUtils.mc.gameMode.handleContainerInput(syncId, fromSlotId, 1, ContainerInput.PICKUP, (Player)HeInvUtils.mc.player);
        HeInvUtils.mc.gameMode.handleContainerInput(syncId, toSlotId, 0, ContainerInput.PICKUP, (Player)HeInvUtils.mc.player);
    }

    public static boolean isKitInMainHand() {
        return InvUtils.testInMainHand((Item[])new Item[]{Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX});
    }

    public static int getMainSlot() {
        return HeInvUtils.mc.player.getInventory().getSelectedSlot();
    }
}

