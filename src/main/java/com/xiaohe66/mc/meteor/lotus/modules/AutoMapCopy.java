package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AutoMapCopy extends StepModule {
    private int bundleToCopy = -1;
    private int copiedBundle1 = -1;
    private int copiedBundle2 = -1;

    public AutoMapCopy() {
        super("M地图画复制", "复制收纳袋中的地图画。要求: 背包有3个收纳袋(1个待复制袋+2个已复制袋); 空地图数量足够。", 3);
        this.addStep(Steps.NEXT, this::checkCopy);
        this.addStep(Steps.USE, this::craftCopy);
    }

    @Override
    protected boolean useQuickStopKeybind() {
        return true;
    }

    @Override
    public void onActivate() {
        this.bundleToCopy = -1;
        this.copiedBundle1 = -1;
        this.copiedBundle2 = -1;
        this.validate();
    }

    private void validate() {
        if (!this.identifyBundles()) {
            this.toggle();
            return;
        }
        PlayerInventory inventory = this.getPlayerInventory();
        int singleMaps = 0;
        int emptyMaps = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == Items.MAP) {
                emptyMaps += stack.getCount();
            } else if (stack.getItem() == Items.FILLED_MAP) {
                if (stack.getCount() == 1) {
                    ++singleMaps;
                } else if (stack.getCount() > 2) {
                    this.warning("背包中存在数量大于2的地图画, 请先整理背包", new Object[0]);
                    this.toggle();
                    return;
                }
            }
        }
        int needed = this.filledMapCountInBundle(this.bundleToCopy) + singleMaps;
        if (needed == 0) {
            this.warning("没有需要复制的地图画", new Object[0]);
            this.toggle();
        } else if (emptyMaps < needed) {
            this.warning("空地图数量不足, 需要[%s]个, 只有[%s]个", new Object[]{needed, emptyMaps});
            this.toggle();
        } else if (this.getFirstEmptySlot() == -1) {
            this.warning("背包需要至少1个空位", new Object[0]);
            this.toggle();
        } else {
            this.warning("检查完成, 待复制[%s]张地图画", new Object[]{needed});
            if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
                this.step = Steps.NEXT;
            } else {
                HeInvUtils.closeCurScreen();
                this.delayNext(Steps.NEXT);
            }
        }
    }

    private void checkCopy() {
        ItemStack doubleStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.FILLED_MAP && itemStack.getCount() == 2);
        if (!doubleStack.isEmpty()) {
            this.distributeMaps(this.getCurPlayerSlot());
            return;
        }
        ItemStack singleStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.FILLED_MAP && itemStack.getCount() == 1);
        if (!singleStack.isEmpty()) {
            this.step = Steps.USE;
            return;
        }
        int remaining = this.filledMapCountInBundle(this.bundleToCopy);
        if (remaining <= 0) {
            this.warning("地图画全部复制完成", new Object[0]);
            this.finish();
            return;
        }
        int emptySlot = this.getFirstEmptySlot();
        if (emptySlot == -1) {
            this.warning("背包没有空位, 复制中断", new Object[0]);
            this.finish();
            return;
        }
        this.moveOneOut(emptySlot);
    }

    private void craftCopy() {
        if (this.ensureInventoryScreen()) {
            PlayerScreenHandler menu = this.mc.player.playerScreenHandler;
            ItemStack result = menu.getSlot(0).getStack();
            if (!result.isEmpty()) {
                InvUtils.shiftClick().slotId(0);
            } else if (menu.getSlot(1).getStack().isEmpty()) {
                ItemStack singleStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.FILLED_MAP && itemStack.getCount() == 1);
                if (singleStack.isEmpty()) {
                    this.step = Steps.NEXT;
                    return;
                }
                this.moveToContainerSlot(this.getCurPlayerSlot(), 1);
            } else if (menu.getSlot(2).getStack().isEmpty()) {
                ItemStack emptyMap = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.MAP);
                if (emptyMap.isEmpty()) {
                    this.warning("空地图不足, 复制中断", new Object[0]);
                    this.finish();
                    return;
                }
                this.moveToContainerSlot(this.getCurPlayerSlot(), 2);
            }
            this.setDelay();
        }
    }

    private void moveToContainerSlot(int slot, int containerSlotId) {
        int syncId = this.mc.player.currentScreenHandler.syncId;
        this.mc.interactionManager.clickSlot(syncId, SlotUtils.indexToId(slot), 0, SlotActionType.PICKUP, this.mc.player);
        this.mc.interactionManager.clickSlot(syncId, containerSlotId, 0, SlotActionType.PICKUP, this.mc.player);
    }

    private void distributeMaps(int slot) {
        if (this.ensureInventoryScreen()) {
            if (this.bundleSize(this.copiedBundle1) < 64 && this.bundleSize(this.copiedBundle2) < 64) {
                int syncId = this.mc.player.currentScreenHandler.syncId;
                int slotId = SlotUtils.indexToId(slot);
                this.mc.interactionManager.clickSlot(syncId, slotId, 1, SlotActionType.PICKUP, this.mc.player);
                this.mc.interactionManager.clickSlot(syncId, SlotUtils.indexToId(this.copiedBundle1), 0, SlotActionType.PICKUP, this.mc.player);
                this.mc.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.PICKUP, this.mc.player);
                this.mc.interactionManager.clickSlot(syncId, SlotUtils.indexToId(this.copiedBundle2), 0, SlotActionType.PICKUP, this.mc.player);
                this.setDelay();
            } else {
                this.warning("已复制袋已满, 复制中断", new Object[0]);
                this.finish();
            }
        }
    }

    private void moveOneOut(int slot) {
        if (this.ensureInventoryScreen()) {
            HeInvUtils.moveOneFromSlot(SlotUtils.indexToId(this.bundleToCopy), SlotUtils.indexToId(slot));
            this.setDelay();
        }
    }

    private boolean ensureInventoryScreen() {
        if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            if (!(this.mc.currentScreen instanceof InventoryScreen)) {
                this.mc.setScreen(new InventoryScreen(this.mc.player));
            }
            return true;
        }
        HeInvUtils.closeCurScreen();
        this.setDelay();
        return false;
    }

    private boolean identifyBundles() {
        PlayerInventory inventory = this.getPlayerInventory();
        ArrayList<Integer> bundleSlots = new ArrayList<>();
        for (int i = 0; i < 36; ++i) {
            if (inventory.getStack(i).getItem() instanceof BundleItem) {
                bundleSlots.add(i);
            }
        }
        // 排除非空且存在不是地图的物品的收纳袋
        bundleSlots.removeIf(slot -> this.isExcludedBundle(inventory.getStack(slot)));
        if (bundleSlots.size() < 3) {
            this.warning("需要3个收纳袋(1个待复制袋+2个已复制袋), 当前有[%s]个", new Object[]{bundleSlots.size()});
            return false;
        }
        if (bundleSlots.size() == 3) {
            return this.matchBundles(inventory, bundleSlots.get(0), bundleSlots.get(1), bundleSlots.get(2));
        }
        if (!this.autoMatchBundles(inventory, bundleSlots)) {
            this.warning("收纳袋过多[%s]个, 未找到[2个包含相同地图+1个空]的收纳袋组合", new Object[]{bundleSlots.size()});
            return false;
        }
        return true;
    }

    private boolean isExcludedBundle(ItemStack stack) {
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (ItemStack itemStack : contents.stream().toList()) {
            Item item = itemStack.getItem();
            if (item != Items.FILLED_MAP && item != Items.MAP) {
                return true;
            }
        }
        return false;
    }

    private boolean matchBundles(PlayerInventory inventory, int slot1, int slot2, int slot3) {
        Set<Object> set1 = this.bundleSet(inventory.getStack(slot1));
        Set<Object> set2 = this.bundleSet(inventory.getStack(slot2));
        Set<Object> set3 = this.bundleSet(inventory.getStack(slot3));
        int overlap12 = this.intersectionCount(set1, set2);
        int overlap13 = this.intersectionCount(set1, set3);
        int overlap23 = this.intersectionCount(set2, set3);
        if (overlap12 <= 0 && overlap13 <= 0 && overlap23 <= 0) {
            boolean empty1 = set1.isEmpty();
            boolean empty2 = set2.isEmpty();
            boolean empty3 = set3.isEmpty();
            int emptyCount = (empty1 ? 1 : 0) + (empty2 ? 1 : 0) + (empty3 ? 1 : 0);
            if (emptyCount == 2) {
                if (!empty1) {
                    this.bundleToCopy = slot1;
                    this.copiedBundle1 = slot2;
                    this.copiedBundle2 = slot3;
                } else if (!empty2) {
                    this.bundleToCopy = slot2;
                    this.copiedBundle1 = slot1;
                    this.copiedBundle2 = slot3;
                } else {
                    this.bundleToCopy = slot3;
                    this.copiedBundle1 = slot1;
                    this.copiedBundle2 = slot2;
                }
                return true;
            }
            this.warning("无法判断收纳袋的类型, 请确保2个已复制袋中有相同的地图画", new Object[0]);
            return false;
        }
        int pair1;
        int pair2;
        int third;
        if (overlap12 >= overlap13 && overlap12 >= overlap23) {
            pair1 = slot1;
            pair2 = slot2;
            third = slot3;
        } else if (overlap13 >= overlap23) {
            pair1 = slot1;
            pair2 = slot3;
            third = slot2;
        } else {
            pair1 = slot2;
            pair2 = slot3;
            third = slot1;
        }
        if (this.bundleSet(inventory.getStack(third)).isEmpty()) {
            // 2个包含相同地图的收纳袋 + 1个空收纳袋: 从其中一个相同地图袋取出地图复制, 空袋变成第3个已复制袋
            this.bundleToCopy = pair1;
            this.copiedBundle1 = pair2;
            this.copiedBundle2 = third;
        } else {
            this.copiedBundle1 = pair1;
            this.copiedBundle2 = pair2;
            this.bundleToCopy = third;
        }
        return true;
    }

    private boolean autoMatchBundles(PlayerInventory inventory, ArrayList<Integer> bundleSlots) {
        int bestPair1 = -1;
        int bestPair2 = -1;
        int bestEmpty = -1;
        int bestOverlap = 0;
        for (int i = 0; i < bundleSlots.size(); ++i) {
            int a = bundleSlots.get(i);
            Set<Object> setA = this.bundleSet(inventory.getStack(a));
            for (int j = i + 1; j < bundleSlots.size(); ++j) {
                int b = bundleSlots.get(j);
                int overlap = this.intersectionCount(setA, this.bundleSet(inventory.getStack(b)));
                if (overlap <= bestOverlap) {
                    continue;
                }
                for (int k = 0; k < bundleSlots.size(); ++k) {
                    int c = bundleSlots.get(k);
                    if (c == a || c == b) {
                        continue;
                    }
                    if (this.bundleSet(inventory.getStack(c)).isEmpty()) {
                        bestOverlap = overlap;
                        bestPair1 = a;
                        bestPair2 = b;
                        bestEmpty = c;
                    }
                }
            }
        }
        if (bestPair1 == -1) {
            return false;
        }
        // 从其中一个相同地图袋取出地图复制, 另一个相同地图袋和空袋作为已复制袋
        this.bundleToCopy = bestPair1;
        this.copiedBundle1 = bestPair2;
        this.copiedBundle2 = bestEmpty;
        return true;
    }

    private Set<Object> bundleSet(ItemStack stack) {
        HashSet<Object> set = new HashSet<>();
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents != null) {
            for (ItemStack itemStack : contents.stream().toList()) {
                set.add(itemStack.getItem() == Items.FILLED_MAP ? itemStack.get(DataComponentTypes.MAP_ID) : itemStack.getItem());
            }
        }
        return set;
    }

    private int intersectionCount(Set<Object> set1, Set<Object> set2) {
        int count = 0;
        for (Object object : set1) {
            if (set2.contains(object)) {
                ++count;
            }
        }
        return count;
    }

    private int filledMapCountInBundle(int slot) {
        BundleContentsComponent contents = this.getItemStack(slot).get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack itemStack : contents.stream().toList()) {
            if (itemStack.getItem() == Items.FILLED_MAP) {
                count += itemStack.getCount();
            }
        }
        return count;
    }

    private int bundleSize(int slot) {
        BundleContentsComponent contents = this.getItemStack(slot).get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack itemStack : contents.stream().toList()) {
            count += itemStack.getCount();
        }
        return count;
    }

    private void finish() {
        HeInvUtils.closeCurScreen();
        if (this.mc.currentScreen != null) {
            this.mc.currentScreen.close();
            this.mc.setScreen(null);
        }
        this.toggle();
    }

    private static String getName(ItemStack stack) {
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        return customName == null ? Names.get(stack) : customName.getString();
    }
}
