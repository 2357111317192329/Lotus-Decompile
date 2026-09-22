/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.world.TickEvent$Post
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.KeybindSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.StringSetting$Builder
 *  meteordevelopment.meteorclient.utils.misc.Keybind
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.PlayerScreenHandler
 *  net.minecraft.screen.slot.Slot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.collection.DefaultedList
 *  net.minecraft.text.Text
 *  net.minecraft.client.gui.screen.ingame.HandledScreen
 *  net.minecraft.registry.Registries
 *  org.lwjgl.glfw.GLFW
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.ScreenCloseEvent;
import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;

import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.event.MouseClickEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseDragEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseReleaseEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

public class ItemClearUp
extends BaseModule {
    private static final Comparator<ItemStack> ITEM_ID_COMPARATOR = Comparator.comparingInt(stack -> Registries.ITEM.getRawId(stack.getItem()));
    private static final Comparator<ItemStack> ITEM_COUNT_COMPARATOR = (a, b) -> Integer.compare(b.getCount(), a.getCount());
    private static final Comparator<ItemStack> ITEM_ID_COUNT_COMPARATOR = ITEM_ID_COMPARATOR.thenComparing(ITEM_COUNT_COMPARATOR);
    private final Setting<Boolean> scrollMove = sgGeneral.add(new BoolSetting.Builder()
        .name("拖拽操作")
        .description("按住绑定的 键位 拖拽鼠标，整组移动经过的物品")
        .defaultValue(true)
        .build());
    private final Setting<Keybind> scrollMoveKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("拖拽操作-键位")
        .description("按住绑定的 键位 拖拽鼠标，整组移动经过的物品")
        .defaultValue(Keybind.fromKey(340))
        .visible(() -> this.scrollMove.get())
        .build());
    private final Setting<Boolean> sameClickMove = sgGeneral.add(new BoolSetting.Builder()
        .name("同类操作")
        .description("按住绑定的 键位 点击物品移动同类型，拖拽到界面外丢弃同类型")
        .defaultValue(true)
        .build());
    private final Setting<Keybind> sameClickMoveKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("同类操作-键位")
        .description("按住绑定的 键位 点击物品移动同类型，拖拽到界面外丢弃同类型")
        .defaultValue(Keybind.fromKey(342))
        .build());
    private final Setting<Boolean> sortEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("物品排序")
        .description("按下绑定键位对当前区域物品进行排序")
        .defaultValue(true)
        .build());
    private final Setting<Keybind> sortKeybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("排序键位")
        .description("按下后对当前区域物品进行排序（容器/背包）")
        .defaultValue(Keybind.fromKey(82))
        .visible(() -> this.sortEnabled.get())
        .action(() -> {
            if (this.sortEnabled.get()) {
                this.startSort();
            }
        })
        .build());
    private final Setting<Boolean> sortHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("排序快捷栏")
        .description("排序背包时是否包含快捷栏")
        .defaultValue(false)
        .visible(() -> this.sortEnabled.get())
        .build());
    private final Setting<Boolean> scrollTransfer = sgGeneral.add(new BoolSetting.Builder()
        .name("滚轮移动")
        .description("启用后，在背包/容器界面中滚动鼠标滚轮可快速转移单个物品")
        .defaultValue(true)
        .build());
    private final LinkedHashSet<Integer> taskSet;
    private final List<Integer> sortClickTaskList;
    private Item drapItem;
    private boolean dragStartedInContainer;
    private boolean isDrapType;

    public ItemClearUp() {
        super("L快捷物品操作", "快捷物品操作：shift 拖拽移动、alt 同类移动/丢弃、R 排序", 0);
        this.taskSet = new LinkedHashSet();
        this.sortClickTaskList = new ArrayList<Integer>();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!this.checkAndDecrement()) {
            return;
        }
        if (!this.sortClickTaskList.isEmpty()) {
            Integer slotId = this.sortClickTaskList.removeFirst();
            InvUtils.click().slotId(slotId.intValue());
            this.setDelay();
            return;
        }
        if (this.taskSet.isEmpty()) {
            return;
        }
        Integer slotId = this.taskSet.removeFirst();
        ItemStack itemStack = this.getItemStackBySlotId(slotId);
        if (itemStack.isEmpty()) {
            this.warning("想移动, 但格子空了 : " + slotId, new Object[0]);
            return;
        }
        if (this.isContainer(slotId)) {
            if (this.isInvFull()) {
                this.warning("背包满了", new Object[0]);
                this.clearQueue();
                return;
            }
        } else if (this.isContainerFull()) {
            this.warning("容器满了", new Object[0]);
            this.clearQueue();
            return;
        }
        if (this.isDrapType) {
            InvUtils.drop().slotId(slotId.intValue());
        } else {
            InvUtils.shiftClick().slotId(slotId.intValue());
        }
        this.setDelay();
    }

    /*
     * Enabled aggressive block sorting
     */
    public void startSort() {
        if (!(this.mc.currentScreen instanceof HandledScreen)) {
            return;
        }
        ScreenHandler handler = this.mc.player.currentScreenHandler;
        List<Integer> sortSlotList = this.getSortableSlots();
        if (sortSlotList.isEmpty()) {
            return;
        }
        int slotCount = sortSlotList.size();
        ArrayList<ItemStack> currentStacks = new ArrayList<ItemStack>(slotCount);
        for (Integer slotId : sortSlotList) {
            currentStacks.add(handler.getSlot(slotId.intValue()).getStack().copy());
        }
        this.clearQueue();
        ArrayList<ItemStack> bundleStacks = new ArrayList<ItemStack>();
        ArrayList<ItemStack> normalStacks = new ArrayList<ItemStack>(slotCount);
        for (ItemStack stack : currentStacks) {
            if (!stack.isEmpty() && HeItemUtils.isBundle(stack.getItem())) {
                bundleStacks.add(stack.copy());
                continue;
            }
            normalStacks.add(stack);
        }
        List<ItemStack> sortedNormalStacks = this.mergeAndSortItemStacks(normalStacks);
        if (!bundleStacks.isEmpty()) {
            bundleStacks.sort(ITEM_ID_COUNT_COMPARATOR);
            this.placeBundleStacks(sortSlotList, currentStacks, bundleStacks, sortedNormalStacks.size());
        }
        List<Integer> sortSlots;
        List<ItemStack> sortStacks;
        if (bundleStacks.isEmpty()) {
            sortSlots = sortSlotList;
            sortStacks = currentStacks;
        } else {
            sortSlots = new ArrayList<Integer>(slotCount);
            sortStacks = new ArrayList<ItemStack>(slotCount);
            for (int i = 0; i < slotCount; ++i) {
                ItemStack stack = currentStacks.get(i);
                if (stack.isEmpty() || !HeItemUtils.isBundle(stack.getItem())) {
                    sortSlots.add(sortSlotList.get(i));
                    sortStacks.add(stack);
                }
            }
        }
        this.placeNormalStacks(sortSlots, sortStacks, sortedNormalStacks);
    }

    private void placeBundleStacks(List<Integer> sortSlotList, List<ItemStack> currentStacks, List<ItemStack> bundleStacks, int startIndex) {
        int slotCount = sortSlotList.size();
        for (int i = 0; i < bundleStacks.size(); ++i) {
            int targetPos = startIndex + i;
            ItemStack bundleStack = bundleStacks.get(i);
            ItemStack currentStack = currentStacks.get(targetPos);
            if (ItemClearUp.isSame(currentStack, bundleStack)) continue;
            int sourcePos = -1;
            for (int j = 0; j < startIndex; ++j) {
                if (!ItemClearUp.isSame(currentStacks.get(j), bundleStack)) continue;
                sourcePos = j;
                break;
            }
            if (sourcePos < 0) {
                for (int j = slotCount - 1; j > targetPos; --j) {
                    if (!ItemClearUp.isSame(currentStacks.get(j), bundleStack)) continue;
                    sourcePos = j;
                    break;
                }
            }
            if (sourcePos < 0) {
                this.warning("错误的状态3", new Object[0]);
                return;
            }
            int sourceSlot = sortSlotList.get(sourcePos);
            int targetSlot = sortSlotList.get(targetPos);
            ItemStack sourceStack = currentStacks.get(sourcePos);
            if (currentStack.isEmpty()) {
                this.sortClickTaskList.add(sourceSlot);
                this.sortClickTaskList.add(targetSlot);
                currentStacks.set(sourcePos, ItemStack.EMPTY);
                currentStacks.set(targetPos, sourceStack);
            } else {
                int emptySlot = this.findEmptySortSlot(sortSlotList, currentStacks);
                if (emptySlot < 0) {
                    this.warning("没有空格，跳过收纳袋排序", new Object[0]);
                    return;
                }
                this.sortClickTaskList.add(targetSlot);
                this.sortClickTaskList.add(emptySlot);
                this.sortClickTaskList.add(sourceSlot);
                this.sortClickTaskList.add(targetSlot);
                this.sortClickTaskList.add(emptySlot);
                this.sortClickTaskList.add(sourceSlot);
                currentStacks.set(sourcePos, currentStack);
                currentStacks.set(targetPos, sourceStack);
            }
        }
    }

    private int findEmptySortSlot(List<Integer> sortSlotList, List<ItemStack> currentStacks) {
        for (int i = 0; i < currentStacks.size(); ++i) {
            if (currentStacks.get(i).isEmpty()) {
                return sortSlotList.get(i);
            }
        }
        ScreenHandler handler = this.mc.player.currentScreenHandler;
        int start;
        int end;
        if (handler instanceof PlayerScreenHandler) {
            start = 9;
            end = 45;
        } else {
            start = 0;
            end = handler.slots.size();
        }
        for (int i = start; i < end; ++i) {
            if (handler.getSlot(i).getStack().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void placeNormalStacks(List<Integer> sortSlotList, List<ItemStack> currentStacks, List<ItemStack> sortedStacks) {
        int slotCount = sortSlotList.size();
        int index = 0;
        while (true) {
            block10: {
                ItemStack mergedStack;
                int n;
                ItemStack overflowStack;
                block13: {
                    ItemStack sourceStack;
                    ItemStack currentStack;
                    block11: {
                        block12: {
                            if (index >= sortedStacks.size()) {
                                return;
                            }
                            ItemStack targetStack = sortedStacks.get(index);
                            currentStack = currentStacks.get(index);
                            if (ItemClearUp.isSameCount(currentStack, targetStack)) break block10;
                            for (n = slotCount - 1; n >= 0 && (!ItemClearUp.isSame(sourceStack = currentStacks.get(n), targetStack) || n < sortedStacks.size() && ItemClearUp.isSame(sourceStack, overflowStack = sortedStacks.get(n))); --n) {
                            }
                            if (n < 0 || n == index) break block10;
                            this.sortClickTaskList.add(sortSlotList.get(n));
                            this.sortClickTaskList.add(sortSlotList.get(index));
                            sourceStack = currentStacks.get(n);
                            currentStacks.set(n, ItemStack.EMPTY);
                            if (!ItemClearUp.canMarge(currentStack, sourceStack)) break block11;
                            n = currentStack.getMaxCount() - currentStack.getCount() - sourceStack.getCount();
                            if (n >= 0) break block12;
                            overflowStack = sourceStack.copyWithCount(-n);
                            mergedStack = currentStacks.get(index);
                            mergedStack.setCount(mergedStack.getMaxCount());
                            break block13;
                        }
                        mergedStack = currentStacks.get(index);
                        mergedStack.increment(sourceStack.getCount());
                        --index;
                        break block10;
                    }
                    overflowStack = currentStack;
                    currentStacks.set(index, sourceStack);
                    --index;
                }
                while (!overflowStack.isEmpty()) {
                    for (n = index + 1; n < sortedStacks.size(); ++n) {
                        mergedStack = sortedStacks.get(n);
                        if (mergedStack.getItem() != overflowStack.getItem()) continue;
                        ItemStack nextStack = currentStacks.get(n);
                        if (nextStack.getItem() != mergedStack.getItem()) {
                            this.sortClickTaskList.add(sortSlotList.get(n));
                            currentStacks.set(n, overflowStack);
                            overflowStack = nextStack;
                            break;
                        }
                            if (!ItemClearUp.canMarge(nextStack, overflowStack)) continue;
                        this.sortClickTaskList.add(sortSlotList.get(n));
                        int diff = nextStack.getMaxCount() - nextStack.getCount() - overflowStack.getCount();
                        if (diff < 0) {
                            nextStack.setCount(nextStack.getMaxCount());
                            overflowStack.setCount(-diff);
                            break;
                        }
                        nextStack.increment(overflowStack.getCount());
                        overflowStack = ItemStack.EMPTY;
                        --index;
                        break;
                    }
                    if (n < sortedStacks.size()) continue;
                    this.warning("错误的状态2", new Object[0]);
                    break;
                }
            }
            ++index;
        }
    }

    private List<Integer> getSortableSlots() {
        int endSlot;
        int mainEndSlot;
        int startSlot;
        boolean includeHotbar;
        if (this.isContainerOpen()) {
            includeHotbar = false;
            startSlot = 0;
            endSlot = mainEndSlot = this.getScreenMainSize();
        } else {
            includeHotbar = true;
            startSlot = 9;
            mainEndSlot = 36;
            endSlot = 45;
        }
        Set<Integer> lockedSlots = LotusUtils.getLockedSlots();
        ArrayList<Integer> sortableSlots = new ArrayList<Integer>();
        int sortIndex = 0;
        int slotId = startSlot;
        while (slotId < mainEndSlot) {
            if (!lockedSlots.contains(sortIndex)) {
                sortableSlots.add(slotId);
            }
            ++slotId;
            ++sortIndex;
        }
        if (includeHotbar && this.sortHotbar.get()) {
            slotId = mainEndSlot;
            while (slotId < endSlot) {
                if (!lockedSlots.contains(sortIndex)) {
                    sortableSlots.add(slotId);
                }
                ++slotId;
                ++sortIndex;
            }
        }
        return sortableSlots;
    }

    private List<ItemStack> mergeAndSortItemStacks(List<ItemStack> items) {
        int count;
        ItemStack sumStack;
        HashMap<ItemBo, ItemStack> itemSumMap = new HashMap<ItemBo, ItemStack>();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            ItemBo itemBo = new ItemBo(stack, true);
            sumStack = itemSumMap.get(itemBo);
            if (sumStack == null) {
                itemSumMap.put(itemBo, stack.copy());
                continue;
            }
            count = sumStack.getCount() + stack.getCount();
            sumStack.setCount(count);
        }
        ArrayList resultList = new ArrayList();
        for (Map.Entry entry : itemSumMap.entrySet()) {
            ItemStack copy;
            int maxCount;
            sumStack = (ItemStack)entry.getValue();
            maxCount = sumStack.getMaxCount();
            for (count = sumStack.getCount(); count >= maxCount; count -= maxCount) {
                copy = sumStack.copyWithCount(maxCount);
                resultList.add(copy);
            }
            if (count <= 0) continue;
            copy = sumStack.copyWithCount(count);
            resultList.add(copy);
        }
        resultList.sort(ITEM_ID_COUNT_COMPARATOR);
        return resultList;
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (!(this.mc.currentScreen instanceof HandledScreen)) {
            return;
        }
        if (event.button != 0) {
            return;
        }
        Slot sourceSlot = this.getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY);
        if (sourceSlot == null || !sourceSlot.hasStack()) {
            return;
        }
        boolean heldAlt = this.isHeldAlt();
        boolean heldShift = this.isHeldShift();
        if (!heldAlt && !heldShift) {
            this.drapItem = sourceSlot.getStack().getItem();
            this.dragStartedInContainer = this.isContainer(sourceSlot.id);
            return;
        }
        if (heldAlt) {
            boolean isContainerSlot = this.isContainer(sourceSlot.id);
            this.addTaskSameItem(isContainerSlot, sourceSlot.getStack());
            event.cancel();
        } else if (heldShift) {
            // empty if block
        }
    }

    private void addTaskSameItem(boolean startFromContainer, ItemStack sourceStack) {
        int startSlot;
        int endSlot;
        DefaultedList slots = this.mc.player.currentScreenHandler.slots;
        if (startFromContainer) {
            startSlot = 0;
            endSlot = this.getScreenMainSize();
        } else {
            startSlot = this.getScreenMainSize();
            endSlot = startSlot + this.getPlayerMainSize();
        }
        Item sourceItem = sourceStack.getItem();
        if (HeItemUtils.isShulkerBox(sourceItem)) {
            ShulkerBoxReader shulkerReader = new ShulkerBoxReader(sourceStack);
            Set<Item> itemSet = shulkerReader.getItemSet();
            for (int i = startSlot; i < endSlot; ++i) {
                ShulkerBoxReader otherReader;
                Set<Item> otherItemSet;
                Slot slot = (Slot)slots.get(i);
                ItemStack itemStack = slot.getStack();
                if (!HeItemUtils.isShulkerBox(itemStack.getItem()) || !itemSet.equals(otherItemSet = (otherReader = new ShulkerBoxReader(itemStack)).getItemSet())) continue;
                this.addTask(slot);
            }
            return;
        }
        for (int i = startSlot; i < endSlot; ++i) {
            Slot slot = (Slot)slots.get(i);
            ItemStack itemStack = slot.getStack();
            if (itemStack.getItem() != sourceItem) continue;
            this.addTask(slot);
        }
    }

    @EventHandler
    private void onMouseDrag(MouseDragEvent event) {
        if (!this.scrollMove.get() || !(this.mc.currentScreen instanceof HandledScreen)) {
            return;
        }
        if (!this.isHeldShift()) {
            return;
        }
        Slot slot = this.getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY);
        if (slot == null || !slot.hasStack()) {
            return;
        }
        this.addTask(slot);
        event.cancel();
    }

    @EventHandler
    private void onMouseRelease(MouseReleaseEvent event) {
        Slot currentSlot;
        if (event.button != 0) {
            return;
        }
        if (this.isHeldAlt() && (currentSlot = this.getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY)) == null && this.drapItem != null) {
            Item item = this.drapItem;
            boolean startedInContainer = this.dragStartedInContainer;
            this.clearQueue();
            this.isDrapType = true;
            ItemStack tempStack = item.getDefaultStack();
            if (!this.isContainerOpen()) {
                this.addTaskSameItem(false, tempStack);
            } else {
                this.addTaskSameItem(startedInContainer, tempStack);
            }
            return;
        }
        this.isDrapType = false;
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (!this.scrollTransfer.get()) {
            return;
        }
        if (!(this.mc.currentScreen instanceof HandledScreen)) {
            return;
        }
        Slot slot = this.getSlotAt(event.getMouseX(), event.getMouseY(), event.getScreenX(), event.getScreenY());
        if (slot == null || !slot.hasStack()) {
            return;
        }
        double scrollAmount = event.getVerticalAmount();
        if (scrollAmount == 0.0) {
            return;
        }
        boolean scrollUp = scrollAmount > 0.0;
        boolean isInventorySlot = this.isInventoryArea(slot.id);
        boolean isHotbarSlot = this.isHotbarArea(slot.id);
        ItemStack stack = slot.getStack();
        int fromSlot = -1;
        int toSlot = -1;
        if (scrollUp) {
            if (isInventorySlot) {
                fromSlot = this.findSourceSlot(stack, false);
                if (this.isSameSlot(slot, stack)) {
                    toSlot = slot.id;
                }
            } else if (isHotbarSlot) {
                fromSlot = slot.id;
                toSlot = this.findTargetSlot(stack, true);
            }
        } else if (isInventorySlot) {
            fromSlot = slot.id;
            toSlot = this.findTargetSlot(stack, false);
        } else if (isHotbarSlot) {
            fromSlot = this.findSourceSlot(stack, true);
            if (this.isSameSlot(slot, stack)) {
                toSlot = slot.id;
            }
        }
        if (fromSlot != -1 && toSlot != -1) {
            HeInvUtils.moveOneFromSlot(fromSlot, toSlot);
            event.cancel();
        }
    }

    @EventHandler
    private void onScreenClose(ScreenCloseEvent event) {
        this.clearQueue();
    }

    private boolean isContainerOpen() {
        if (this.mc.currentScreen instanceof HandledScreen) {
            return !(this.mc.player.currentScreenHandler instanceof PlayerScreenHandler);
        }
        return false;
    }

    private Slot getSlotAt(double mouseX, double mouseY, int screenX, int screenY) {
        ScreenHandler handler = this.mc.player.currentScreenHandler;
        for (Slot slot : handler.slots) {
            int slotLeft = slot.x + screenX;
            int slotTop = slot.y + screenY;
            boolean inside = mouseX >= slotLeft && mouseX < slotLeft + 18 && mouseY >= slotTop && mouseY < slotTop + 18;
            if (!inside) continue;
            return slot;
        }
        return null;
    }

    private void addTask(Slot slot) {
        this.taskSet.add(slot.id);
    }

    private void clearQueue() {
        this.isDrapType = false;
        this.drapItem = null;
        this.dragStartedInContainer = false;
        this.taskSet.clear();
        this.sortClickTaskList.clear();
    }

    private boolean isSameSlot(Slot slot, ItemStack stack) {
        if (!slot.hasStack()) {
            return true;
        }
        return ItemClearUp.canMarge(slot.getStack(), stack);
    }

    private boolean isInventoryArea(int slotId) {
        if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            return slotId >= 9 && slotId < 36;
        }
        return this.isContainer(slotId);
    }

    private boolean isHotbarArea(int slotId) {
        if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            return slotId >= 36 && slotId < 45;
        }
        return !this.isContainer(slotId);
    }

    private int findTargetSlot(ItemStack stack, boolean inventoryArea) {
        ScreenHandler handler = this.mc.player.currentScreenHandler;
        for (Slot slot : handler.slots) {
            if (inventoryArea && !this.isInventoryArea(slot.id) || !inventoryArea && !this.isHotbarArea(slot.id) || !slot.hasStack() || !ItemClearUp.canMarge(slot.getStack(), stack)) continue;
            return slot.id;
        }
        for (Slot slot : handler.slots) {
            if (inventoryArea && !this.isInventoryArea(slot.id) || !inventoryArea && !this.isHotbarArea(slot.id) || slot.hasStack()) continue;
            return slot.id;
        }
        return -1;
    }

    private int findSourceSlot(ItemStack stack, boolean inventoryArea) {
        ScreenHandler handler = this.mc.player.currentScreenHandler;
        for (Slot slot : handler.slots) {
            if (inventoryArea && !this.isInventoryArea(slot.id) || !inventoryArea && !this.isHotbarArea(slot.id) || !slot.hasStack() || !ItemClearUp.isSame(slot.getStack(), stack)) continue;
            return slot.id;
        }
        return -1;
    }

    private boolean isHeldShift() {
        long handle = this.mc.getWindow().getHandle();
        return this.scrollMove.get() != false && GLFW.glfwGetKey(handle, this.scrollMoveKey.get().getValue()) == 1;
    }

    private boolean isHeldAlt() {
        long handle = this.mc.getWindow().getHandle();
        return this.sameClickMove.get() != false && GLFW.glfwGetKey(handle, this.sameClickMoveKey.get().getValue()) == 1;
    }

    public static boolean isSame(ItemStack left, ItemStack right) {
        if (left.getItem() != right.getItem()) {
            return false;
        }
        return ItemClearUp.isSameName(left, right);
    }

    public static boolean isSameCount(ItemStack left, ItemStack right) {
        if (left.getItem() != right.getItem()) {
            return false;
        }
        if (left.getCount() != right.getCount()) {
            return false;
        }
        return ItemClearUp.isSameName(left, right);
    }

    public static boolean canMarge(ItemStack left, ItemStack right) {
        Item item = left.getItem();
        if (item != right.getItem()) {
            return false;
        }
        if (left.getCount() >= left.getMaxCount()) {
            return false;
        }
        if (item == Items.FILLED_MAP) {
            MapIdComponent leftId = left.get(DataComponentTypes.MAP_ID);
            MapIdComponent rightId = right.get(DataComponentTypes.MAP_ID);
            if (leftId == null || rightId == null || leftId.id() != rightId.id()) {
                return false;
            }
        }
        return ItemClearUp.isSameName(left, right);
    }

    private static boolean isSameName(ItemStack left, ItemStack right) {
        Text leftName = left.getCustomName();
        Text rightName = right.getCustomName();
        if (leftName == null && rightName == null) {
            return true;
        }
        if (leftName == null || rightName == null) {
            return false;
        }
        return leftName.getString().equals(rightName.getString());
    }

    public boolean isSortingInProgress() {
        return !this.sortClickTaskList.isEmpty();
    }

    public void onDeactivate() {
        this.clearQueue();
    }
}
