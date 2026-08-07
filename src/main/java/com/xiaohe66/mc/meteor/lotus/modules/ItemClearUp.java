package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.event.MouseClickEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseDragEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseReleaseEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenCloseEvent;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class ItemClearUp extends BaseModule {
   private static final Comparator<ItemStack> ITEM_ID_COMPARATOR = Comparator.comparingInt(stack -> BuiltInRegistries.ITEM.getId(stack.getItem()));
   private static final Comparator<ItemStack> ITEM_COUNT_COMPARATOR = (a, b) -> Integer.compare(b.getCount(), a.getCount());
   private static final Comparator<ItemStack> ITEM_ID_COUNT_COMPARATOR = ITEM_ID_COMPARATOR.thenComparing(ITEM_COUNT_COMPARATOR);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> scrollMove = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("拖拽操作")).description("按住绑定的 键位 拖拽鼠标，整组移动经过的物品")).defaultValue(true)).build());
   private final Setting<Keybind> scrollMoveKey = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                        .name("拖拽操作-键位"))
                     .description("按住绑定的 键位 拖拽鼠标，整组移动经过的物品"))
                  .defaultValue(Keybind.fromKey(340)))
               .visible(this.scrollMove::get))
            .build()
      );
   private final Setting<Boolean> sameClickMove = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("同类操作")).description("按住绑定的 键位 点击物品移动同类型，拖拽到界面外丢弃同类型")).defaultValue(true)).build());
   private final Setting<Keybind> sameClickMoveKey = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("同类操作-键位"))
                  .description("按住绑定的 键位 点击物品移动同类型，拖拽到界面外丢弃同类型"))
               .defaultValue(Keybind.fromKey(342)))
            .build()
      );
   private final Setting<Boolean> sortEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("物品排序")).description("按下绑定键位对当前区域物品进行排序")).defaultValue(true)).build());
   private final Setting<Keybind> sortKeybind = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                        .name("排序键位"))
                     .description("按下后对当前区域物品进行排序（容器/背包）"))
                  .defaultValue(Keybind.fromKey(82)))
               .visible(this.sortEnabled::get))
            .action(() -> {
               if ((Boolean)this.sortEnabled.get()) {
                  this.startSort();
               }
            })
            .build()
      );
   private final LinkedHashSet<Integer> taskSet = new LinkedHashSet<>();
   private final List<Integer> sortClickTaskList = new ArrayList<>();
   private Item drapItem;
   private boolean isDrapType;

   public ItemClearUp() {
      super("物品整理", "物品快捷操作：shift 拖拽移动、alt 同类移动/丢弃、R 排序", 0);
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.checkAndDecrement()) {
         if (!this.sortClickTaskList.isEmpty()) {
            Integer slotId = this.sortClickTaskList.removeFirst();
            InvUtils.click().slotId(slotId);
            this.setDelay();
         } else if (!this.taskSet.isEmpty()) {
            Integer moveSlotId = this.taskSet.removeFirst();
            ItemStack itemStack = this.getItemStackBySlotId(moveSlotId);
            if (itemStack.isEmpty()) {
               this.warning("想移动, 但格子空了 : " + moveSlotId, new Object[0]);
            } else {
               if (this.isContainer(moveSlotId)) {
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
                  InvUtils.drop().slotId(moveSlotId);
               } else {
                  InvUtils.shiftClick().slotId(moveSlotId);
               }

               this.setDelay();
            }
         }
      }
   }

   private void startSort() {
      if (this.mc.screen instanceof AbstractContainerScreen) {
         AbstractContainerMenu handler = this.mc.player.containerMenu;
         boolean containerOpen = this.isContainerOpen();
         if (!containerOpen) {
            this.warning("暂不支持对背包排序", new Object[0]);
         } else {
            int size = this.getScreenMainSize();
            List<ItemStack> tmpItemStackList = new ArrayList<>();

            for (int i = 0; i < size; i++) {
               tmpItemStackList.add(handler.getSlot(i).getItem().copy());
            }

            List<ItemStack> correctItemStackList = this.mergeAndSortItemStacks(tmpItemStackList);
            this.clearQueue();

            for (int targetSlotId = 0; targetSlotId < correctItemStackList.size(); targetSlotId++) {
               ItemStack targetSlotCorrect = correctItemStackList.get(targetSlotId);
               ItemStack targetSlotTmp = tmpItemStackList.get(targetSlotId);
               if (!isSameCount(targetSlotTmp, targetSlotCorrect)) {
                  int sourceSlotId;
                  for (sourceSlotId = size - 1; sourceSlotId >= 0; sourceSlotId--) {
                     ItemStack sourceSlotTmp = tmpItemStackList.get(sourceSlotId);
                     if (isSame(sourceSlotTmp, targetSlotCorrect)) {
                        if (sourceSlotId >= correctItemStackList.size()) {
                           break;
                        }

                        ItemStack sourceSlotCorrect = correctItemStackList.get(sourceSlotId);
                        if (!isSame(sourceSlotTmp, sourceSlotCorrect)) {
                           break;
                        }
                     }
                  }

                  if (sourceSlotId >= 0 && sourceSlotId != targetSlotId) {
                     this.sortClickTaskList.add(sourceSlotId);
                     this.sortClickTaskList.add(targetSlotId);
                     ItemStack sourceSlotTmp = tmpItemStackList.get(sourceSlotId);
                     tmpItemStackList.set(sourceSlotId, ItemStack.EMPTY);
                     ItemStack nextSourceSlotTmp;
                     if (canMarge(targetSlotTmp, sourceSlotTmp)) {
                        int supCount = targetSlotTmp.getMaxStackSize() - targetSlotTmp.getCount() - sourceSlotTmp.getCount();
                        if (supCount >= 0) {
                           ItemStack itemStack = tmpItemStackList.get(targetSlotId);
                           itemStack.grow(sourceSlotTmp.getCount());
                           targetSlotId--;
                           continue;
                        }

                        nextSourceSlotTmp = sourceSlotTmp.copyWithCount(-supCount);
                        ItemStack itemStack = tmpItemStackList.get(targetSlotId);
                        itemStack.setCount(itemStack.getMaxStackSize());
                     } else {
                        nextSourceSlotTmp = targetSlotTmp;
                        tmpItemStackList.set(targetSlotId, sourceSlotTmp);
                        targetSlotId--;
                     }

                     while (!nextSourceSlotTmp.isEmpty()) {
                        int nextTargetSlotId;
                        for (nextTargetSlotId = targetSlotId + 1; nextTargetSlotId < correctItemStackList.size(); nextTargetSlotId++) {
                           ItemStack nextTargetSlotCorrect = correctItemStackList.get(nextTargetSlotId);
                           if (nextTargetSlotCorrect.getItem() == nextSourceSlotTmp.getItem()) {
                              ItemStack nextTargetSlotTmp = tmpItemStackList.get(nextTargetSlotId);
                              if (nextTargetSlotTmp.getItem() != nextTargetSlotCorrect.getItem()) {
                                 this.sortClickTaskList.add(nextTargetSlotId);
                                 tmpItemStackList.set(nextTargetSlotId, nextSourceSlotTmp);
                                 nextSourceSlotTmp = nextTargetSlotTmp;
                                 break;
                              }

                              if (canMarge(nextTargetSlotTmp, nextSourceSlotTmp)) {
                                 this.sortClickTaskList.add(nextTargetSlotId);
                                 int supCount = nextTargetSlotTmp.getMaxStackSize() - nextTargetSlotTmp.getCount() - nextSourceSlotTmp.getCount();
                                 if (supCount < 0) {
                                    nextTargetSlotTmp.setCount(nextTargetSlotTmp.getMaxStackSize());
                                    nextSourceSlotTmp.setCount(-supCount);
                                 } else {
                                    nextTargetSlotTmp.grow(nextSourceSlotTmp.getCount());
                                    nextSourceSlotTmp = ItemStack.EMPTY;
                                    targetSlotId--;
                                 }
                                 break;
                              }
                           }
                        }

                        if (nextTargetSlotId >= correctItemStackList.size()) {
                           this.warning("错误的状态2", new Object[0]);
                           break;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private List<ItemStack> mergeAndSortItemStacks(List<ItemStack> items) {
      Map<ItemBo, ItemStack> itemStackCountMap = new HashMap<>();

      for (ItemStack itemStack : items) {
         if (!itemStack.isEmpty()) {
            ItemBo key = new ItemBo(itemStack, true);
            ItemStack sumItemStack = itemStackCountMap.get(key);
            if (sumItemStack == null) {
               itemStackCountMap.put(key, itemStack.copy());
            } else {
               int newCount = sumItemStack.getCount() + itemStack.getCount();
               sumItemStack.setCount(newCount);
            }
         }
      }

      List<ItemStack> result = new ArrayList<>();

      for (Entry<ItemBo, ItemStack> entry : itemStackCountMap.entrySet()) {
         ItemStack sumItemStack = entry.getValue();
         int maxStackCount = sumItemStack.getMaxStackSize();

         int supCount;
         for (supCount = sumItemStack.getCount(); supCount >= maxStackCount; supCount -= maxStackCount) {
            ItemStack copy = sumItemStack.copyWithCount(maxStackCount);
            result.add(copy);
         }

         if (supCount > 0) {
            ItemStack copy = sumItemStack.copyWithCount(supCount);
            result.add(copy);
         }
      }

      result.sort(ITEM_ID_COUNT_COMPARATOR);
      return result;
   }

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      if (this.mc.screen instanceof AbstractContainerScreen) {
         if (event.button == 0) {
            Slot sourceSlot = this.getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY);
            if (sourceSlot != null && sourceSlot.hasItem()) {
               boolean heldAlt = this.isHeldAlt();
               boolean heldShift = this.isHeldShift();
               if (!heldAlt && !heldShift) {
                  this.drapItem = sourceSlot.getItem().getItem();
               } else {
                  if (heldAlt) {
                     boolean container = this.isContainer(sourceSlot.index);
                     this.addTaskSameItem(container, sourceSlot.getItem());
                     event.cancel();
                  } else if (heldShift) {
                  }
               }
            }
         }
      }
   }

   private void addTaskSameItem(boolean isContainer, ItemStack sourceStack) {
      NonNullList<Slot> slots = this.mc.player.containerMenu.slots;
      int start;
      int end;
      if (isContainer) {
         start = 0;
         end = this.getScreenMainSize();
      } else {
         start = this.getScreenMainSize();
         end = start + this.getPlayerMainSize();
      }

      Item sourceItem = sourceStack.getItem();
      if (HeItemUtils.isShulkerBox(sourceItem)) {
         ShulkerBoxReader reader = new ShulkerBoxReader(sourceStack);
         Set<Item> sourceItemSet = reader.getItemSet();

         for (int i = start; i < end; i++) {
            Slot slot = (Slot)slots.get(i);
            ItemStack itemStack = slot.getItem();
            if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
               ShulkerBoxReader targetReader = new ShulkerBoxReader(itemStack);
               Set<Item> targetItemSet = targetReader.getItemSet();
               if (sourceItemSet.equals(targetItemSet)) {
                  this.addTask(slot);
               }
            }
         }
      } else {
         for (int i = start; i < end; i++) {
            Slot slot = (Slot)slots.get(i);
            ItemStack itemStack = slot.getItem();
            if (itemStack.getItem() == sourceItem) {
               this.addTask(slot);
            }
         }
      }
   }

   @EventHandler
   private void onMouseDrag(MouseDragEvent event) {
      if ((Boolean)this.scrollMove.get() && this.mc.screen instanceof AbstractContainerScreen) {
         if (this.isHeldShift()) {
            Slot slot = this.getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY);
            if (slot != null && slot.hasItem()) {
               this.addTask(slot);
               event.cancel();
            }
         }
      }
   }

   @EventHandler
   private void onMouseRelease(MouseReleaseEvent event) {
      if (event.button == 0) {
         if (this.isHeldAlt()) {
            Slot currentSlot = this.getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY);
            if (currentSlot == null && this.drapItem != null && !this.isContainerOpen()) {
               Item item = this.drapItem;
               this.clearQueue();
               this.isDrapType = true;
               ItemStack tempStack = item.getDefaultInstance();
               this.addTaskSameItem(false, tempStack);
               return;
            }
         }

         this.isDrapType = false;
      }
   }

   @EventHandler
   private void onScreenClose(ScreenCloseEvent event) {
      this.clearQueue();
   }

   private boolean isContainerOpen() {
      return this.mc.screen instanceof AbstractContainerScreen ? !(this.mc.player.containerMenu instanceof InventoryMenu) : false;
   }

   private Slot getSlotAt(double mouseX, double mouseY, int screenX, int screenY) {
      AbstractContainerMenu handler = this.mc.player.containerMenu;

      for (Slot slot : handler.slots) {
         int slotX = slot.x + screenX;
         int slotY = slot.y + screenY;
         boolean isMouseOverSlot = mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18;
         if (isMouseOverSlot) {
            return slot;
         }
      }

      return null;
   }

   private void addTask(Slot slot) {
      this.taskSet.add(slot.index);
   }

   private void clearQueue() {
      this.isDrapType = false;
      this.drapItem = null;
      this.taskSet.clear();
      this.sortClickTaskList.clear();
   }

   private boolean isHeldShift() {
      long handle = this.mc.getWindow().handle();
      return (Boolean)this.scrollMove.get() && GLFW.glfwGetKey(handle, ((Keybind)this.scrollMoveKey.get()).getValue()) == 1;
   }

   private boolean isHeldAlt() {
      long handle = this.mc.getWindow().handle();
      return (Boolean)this.sameClickMove.get() && GLFW.glfwGetKey(handle, ((Keybind)this.sameClickMoveKey.get()).getValue()) == 1;
   }

   public static boolean isSame(ItemStack left, ItemStack right) {
      return left.getItem() != right.getItem() ? false : isSameName(left, right);
   }

   public static boolean isSameCount(ItemStack left, ItemStack right) {
      if (left.getItem() != right.getItem()) {
         return false;
      } else {
         return left.getCount() != right.getCount() ? false : isSameName(left, right);
      }
   }

   public static boolean canMarge(ItemStack left, ItemStack right) {
      if (left.getItem() != right.getItem()) {
         return false;
      } else {
         return left.getCount() >= left.getMaxStackSize() ? false : isSameName(left, right);
      }
   }

   private static boolean isSameName(ItemStack left, ItemStack right) {
      Component leftName = left.getCustomName();
      Component rightName = right.getCustomName();
      if (leftName == null && rightName == null) {
         return true;
      } else {
         return leftName != null && rightName != null ? leftName.getString().equals(rightName.getString()) : false;
      }
   }

   public void onDeactivate() {
      this.clearQueue();
   }
}
