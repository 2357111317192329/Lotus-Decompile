package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.util.Hand;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;

public class AutoKit extends WalkModule {
   private final Setting<Integer> scanRange = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("扫描范围")).description("检测展示框和箱子的范围")).defaultValue(50)).min(1).sliderMax(100).build());
   private final Setting<Integer> qty = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("操作数")).description("每次操作的物品数量")).min(1).sliderMax(9).defaultValue(9)).build());
   private final Setting<Item> emptyKitItem = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)new meteordevelopment.meteorclient.settings.ItemSetting.Builder()
                     .name("空盒标识"))
                  .description("空盒箱子展示框上的物品"))
               .defaultValue(Items.WHITE_SHULKER_BOX))
            .build()
      );
   private final Setting<Item> finishedKitItem = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)new meteordevelopment.meteorclient.settings.ItemSetting.Builder()
                     .name("成品标识"))
                  .description("成品kit箱子展示框上的物品"))
               .defaultValue(Items.LIGHT_BLUE_SHULKER_BOX))
            .build()
      );
   private final Setting<Item> miscKitItem = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)new meteordevelopment.meteorclient.settings.ItemSetting.Builder()
                     .name("杂盒标识"))
                  .description("杂盒箱子展示框上的物品"))
               .defaultValue(Items.BLUE_SHULKER_BOX))
            .build()
      );
   private final Setting<Boolean> initBtn = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("初始化"))
                     .description("使用前需要先初始化，保存所有箱子位置并读取主手盒子作为模板"))
                  .defaultValue(false))
               .onChanged(this::init))
            .build()
      );
   private final Map<ItemBo, StoragePos> itemPosMap = new LinkedHashMap<>();
   private StoragePos emptyKitPos;
   private StoragePos finishedKitPos;
   private StoragePos miscKitPos;
   private final List<ItemBo> templateItemList = new ArrayList<>(27);
   private int takeItemIndex = 0;
   private int putItemIndex = 0;
   private ItemBo currentItem;
   private StoragePos curOperationPos;
   private int putKitIndex = -1;

   public AutoKit() {
      super("自动Kit", "自动装配Kit, 需要在<简易仓库>中使用。由资深猎人<dadou>友情赞助开发。");
      this.addStep(Steps.NEXT, this::nextStep);
      this.addStep(Steps.PUT_KIT, this::putKit);
      this.addStep(Steps.PUT_ITEM, this::putItem);
      this.addStep(Steps.TAKE_ITEM, this::takeItem);
      this.addStep(Steps.PLACE_EMPTY_KIT, this::placeEmptyKit);
      this.addStep(Steps.TAKE_EMPTY_KIT, this::takeEmptyKit);
      this.addStep(Steps.PLACE_KIT, this::placeKit);
      this.addStep(Steps.TAKE_KIT, this::takeKit);
      this.addStep(Steps.BREAK_KIT, this::breakKit);
   }

   private void init(Boolean enabled) {
      if (Boolean.TRUE.equals(enabled)) {
         this.initBtn.set(false);
         if (this.mc.player != null && this.mc.world != null) {
            this.clear();
            boolean scanPositionsSuccess = this.scanPositions();
            if (!scanPositionsSuccess) {
               this.clear();
            } else {
               boolean readTemplateSuccess = this.readTemplateFromMainHand();
               if (!readTemplateSuccess) {
                  this.clear();
               } else {
                  boolean missItem = false;

                  for (ItemBo itemBo : this.templateItemList) {
                     if (!this.itemPosMap.containsKey(itemBo)) {
                        this.warning("未找到物品<" + itemBo.getName() + ">的存储位置", new Object[0]);
                        missItem = true;
                     }
                  }

                  if (missItem) {
                     this.clear();
                  } else {
                     String itemNames = this.itemPosMap.keySet().stream().map(ItemBo::getName).collect(Collectors.joining(","));
                     this.info("成功识别: " + itemNames, new Object[0]);
                     this.info("初始化完毕！", new Object[0]);
                  }
               }
            }
         } else {
            this.warning("玩家或世界未加载", new Object[0]);
         }
      }
   }

   @Override
   public void onActivate() {
      super.onActivate();
      if (!this.itemPosMap.isEmpty() && this.emptyKitPos != null && this.finishedKitPos != null) {
         this.takeItemIndex = 0;
         this.putItemIndex = 0;
         this.currentItem = null;
         this.curOperationPos = null;
         this.nextStep();
      } else {
         this.warning("请先进行初始化", new Object[0]);
         this.toggle();
      }
   }

   private void nextStep() {
      ItemStack nextKit = this.nextPlayerStack(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()));
      if (!nextKit.isEmpty()) {
         this.putKitIndex = this.getCurPlayerSlot();
         ShulkerBoxReader reader = new ShulkerBoxReader(nextKit);
         if (reader.matchesTemplate(this.templateItemList)) {
            this.curOperationPos = this.finishedKitPos;
            this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放成品kit");
         } else if (reader.isEmpty()) {
            this.curOperationPos = this.emptyKitPos;
            this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放空盒");
         } else {
            if (reader.isSameItem()) {
               ItemBo firstItemBo = reader.getFirstItemBo();
               if (this.itemPosMap.containsKey(firstItemBo)) {
                  this.curOperationPos = this.itemPosMap.get(firstItemBo);
                  this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放满盒");
                  return;
               }
            }

            this.curOperationPos = this.miscKitPos;
            this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放杂盒");
         }
      } else {
         List<ItemEntity> shulkerEntities = this.mc
            .world
            .getEntitiesByClass(ItemEntity.class, this.mc.player.getBoundingBox().expand(5.0), e -> HeItemUtils.isShulkerBox(e.getStack().getItem()));
         if (!shulkerEntities.isEmpty()) {
            BlockPos targetPos = shulkerEntities.getFirst().getBlockPos();
            if (targetPos.getY() != this.mc.player.getBlockY()) {
               BlockPos testPos = new BlockPos(targetPos.getX(), this.mc.player.getBlockY(), targetPos.getZ());
               BlockPos canStandPos = HeBlockUtils.getCanStandPos(testPos, 1);
               if (canStandPos != null) {
                  targetPos = canStandPos;
               }
            }

            this.gotoTargetIfNeed(targetPos, 0, Steps.NEXT, "捡kit");
         } else {
            this.step = Steps.PUT_ITEM;
         }
      }
   }

   private void putKit() {
      if (this.notInOperationRange(this.curOperationPos)) {
         this.gotoBtnPos(this.curOperationPos, "<存kit>距离不够，尝试移动", Steps.PUT_KIT);
      } else {
         ItemStack kitItemStack = this.getItemStack(this.putKitIndex);
         if (!HeItemUtils.isShulkerBox(kitItemStack.getItem())) {
            this.warning("放kit, 但身上没有...", new Object[0]);
            this.step = Steps.NEXT;
         } else {
            this.openChest(this.curOperationPos.getPutPos(), inventory -> {
               FindItemResult emptyResult = InvUtils.findEmpty();
               if (emptyResult.found()) {
                  InvUtils.shiftClick().slot(this.putKitIndex);
                  this.delayCloseNext(Steps.NEXT);
               } else {
                  this.breakStep("<存kit>箱满，无法存放");
               }
            });
         }
      }
   }

   private void takeEmptyKit() {
      if (this.notInOperationRange(this.emptyKitPos)) {
         this.gotoBtnPos(this.emptyKitPos, "<拿空盒>距离不够，尝试移动", Steps.TAKE_EMPTY_KIT);
      } else {
         this.openChest(this.emptyKitPos.getTakePos(), inventory -> {
            for (int slot = 0; slot < inventory.size(); slot++) {
               ItemStack itemStack = inventory.getStack(slot);
               if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                  ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
                  if (reader.isEmpty()) {
                     this.info("拿取空盒", new Object[0]);
                     InvUtils.shiftClick().slotId(slot);
                     this.delayCloseNext(Steps.PLACE_EMPTY_KIT);
                     return;
                  }
               }
            }

            this.breakStep("<空盒箱>缺少空潜影盒");
         });
      }
   }

   private void placeEmptyKit() {
      BlockState finishedKiBlockState = this.mc.world.getBlockState(this.finishedKitPos.getKitPos());
      if (finishedKiBlockState.getBlock() instanceof ShulkerBoxBlock) {
         this.step = Steps.PUT_ITEM;
      } else if (!finishedKiBlockState.isAir()) {
         this.breakStep("<位置>被占用: " + Names.get(this.finishedKitPos.getItem().getItem()));
      } else {
         FindItemResult findItemResult = HeInvUtils.findShulkerBox(Items.AIR);
         if (!findItemResult.found()) {
            this.step = Steps.TAKE_EMPTY_KIT;
         } else if (!findItemResult.isMainHand()) {
            HeInvUtils.swap(findItemResult.slot(), this.getMainSlot());
            this.setDelay();
         } else if (this.notInOperationRange(this.finishedKitPos)) {
            this.gotoTarget(this.finishedKitPos.getBtnPos(), 0, Steps.PLACE_EMPTY_KIT);
         } else {
            this.info("放置空盒", new Object[0]);
            HeBlockUtils.place(this.finishedKitPos.getKitPos(), this.getMainSlot(), true, Direction.DOWN);
            this.putItemIndex = 0;
            this.delayNext(Steps.PUT_ITEM);
         }
      }
   }

   private void takeKit() {
      if (this.notInOperationRange(this.curOperationPos)) {
         this.gotoBtnPos(this.curOperationPos, "<拿kit>距离不够，尝试移动", Steps.TAKE_KIT);
      } else {
         this.openChest(this.curOperationPos.getTakePos(), inventory -> {
            ItemStack nextScreenStack = this.nextScreenStack(itemStack -> {
               if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                  ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
                  return reader.hasItem(this.currentItem);
               } else {
                  return false;
               }
            });
            if (nextScreenStack.isEmpty()) {
               this.breakStep("<" + this.currentItem.getName() + ">库存不足");
            } else {
               this.info("拿取kit: " + this.currentItem.getName(), new Object[0]);
               InvUtils.shiftClick().slotId(this.getCurScreenSlot());
               this.delayCloseNext(Steps.PLACE_KIT);
            }
         });
      }
   }

   private void placeKit() {
      BlockState finishedKitBlockState = this.mc.world.getBlockState(this.curOperationPos.getKitPos());
      if (!finishedKitBlockState.isAir()) {
         if (finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock) {
            this.step = Steps.TAKE_ITEM;
         } else {
            this.breakStep("<kit位置>被占用");
         }
      } else {
         ItemStack nextItemStack = this.nextPlayerStack(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()));
         if (nextItemStack.isEmpty()) {
            this.step = Steps.TAKE_KIT;
         } else if (this.notInOperationRange(this.curOperationPos)) {
            this.gotoBtnPos(this.curOperationPos, "去拿kit", Steps.PLACE_KIT);
         } else if (!this.swapToMainHand(this.getCurPlayerSlot())) {
            this.info("放置盒子:" + Names.get(this.curOperationPos.getItem().getItem()), new Object[0]);
            HeBlockUtils.place(this.curOperationPos.getKitPos(), this.getMainSlot(), true, Direction.DOWN);
            this.delayNext(Steps.TAKE_ITEM);
         }
      }
   }

   private void takeItem() {
      if (this.takeItemIndex < this.templateItemList.size() && this.takeItemIndex - this.putItemIndex < (Integer)this.qty.get()) {
         BlockState finishedKitBlockState = this.mc.world.getBlockState(this.curOperationPos.getKitPos());
         if (finishedKitBlockState.isAir()) {
            this.step = Steps.PLACE_KIT;
         } else if (!(finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock)) {
            this.breakStep("<kit位置>被占用");
         } else if (this.notInOperationRange(this.curOperationPos)) {
            this.gotoTarget(this.curOperationPos.getBtnPos(), 0, Steps.TAKE_ITEM);
         } else {
            this.openKit(
               this.curOperationPos.getKitPos(),
               screenHandler -> {
                  ItemStack nextItemStack = this.nextScreenStack(
                     itemStack -> this.currentItem.isSameItem(itemStack) && itemStack.getCount() == this.currentItem.getItem().getMaxCount()
                  );
                  if (nextItemStack.isEmpty()) {
                     this.info("kit已空，挖掉重放: " + this.currentItem.getName(), new Object[0]);
                     this.delayCloseNext(Steps.BREAK_KIT);
                  } else {
                     this.info("拿取物品: " + this.currentItem.getName(), new Object[0]);
                     InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                     this.takeItemIndex++;
                     if (this.takeItemIndex >= this.templateItemList.size() || this.takeItemIndex - this.putItemIndex >= (Integer)this.qty.get()) {
                        this.delayCloseNext(Steps.PUT_ITEM);
                     } else if (this.templateItemList.get(this.takeItemIndex).equals(this.currentItem)) {
                        this.setDelay();
                     } else {
                        this.setTakeItem(this.takeItemIndex);
                     }
                  }
               }
            );
         }
      } else {
         this.step = Steps.PUT_ITEM;
      }
   }

   private void putItem() {
      ItemBo putItem = this.templateItemList.get(this.putItemIndex);
      ItemStack nextItemStack = this.nextPlayerStack(itemStack -> putItem.isSameItem(itemStack) && itemStack.getCount() == itemStack.getMaxCount());
      if (nextItemStack.isEmpty()) {
         this.closeScreen();
         if (this.putItemIndex > this.takeItemIndex) {
            this.takeItemIndex = this.putItemIndex;
         }

         this.setTakeItem(putItem);
      } else {
         BlockState finishedKitBlockState = this.mc.world.getBlockState(this.finishedKitPos.getKitPos());
         if (finishedKitBlockState.isAir()) {
            this.curOperationPos = this.finishedKitPos;
            this.closeScreen();
            this.step = Steps.PLACE_EMPTY_KIT;
         } else if (!(finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock)) {
            this.closeScreen();
            this.breakStep("<成品位置>被占用");
         } else if (this.notInOperationRange(this.finishedKitPos)) {
            this.closeScreen();
            this.gotoBtnPos(this.finishedKitPos, "<放" + putItem.getName() + ">距离不够, 尝试移动", Steps.PUT_ITEM);
         } else {
            this.openKit(this.finishedKitPos.getKitPos(), screenHandler -> {
               if (this.hasScreenFull()) {
                  this.setBreakFullKit();
               } else {
                  ItemStack itemStack = screenHandler.getSlot(this.putItemIndex).getStack();
                  if (itemStack.isEmpty()) {
                     InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                  }

                  this.putItemIndex++;
                  if (this.putItemIndex >= this.templateItemList.size()) {
                     this.setBreakFullKit();
                  } else if (!itemStack.isEmpty()) {
                     this.setDelay();
                  }
               }
            });
         }
      }
   }

   private void breakKit() {
      if (this.notInOperationRange(this.curOperationPos)) {
         this.gotoBtnPos(this.curOperationPos, "<挖盒子>距离不够, 尝试移动", Steps.BREAK_KIT);
      } else {
         Vec3d hitPos = Vec3d.ofCenter(this.curOperationPos.getBtnPos()).add(0.0, -0.5, 0.0);
         BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, this.curOperationPos.getBtnPos(), false);
         PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0);
         this.mc.getNetworkHandler().sendPacket(packet);
         this.step = Steps.NEXT;
         this.setDelay(40);
      }
   }

   private boolean scanPositions() {
      List<ItemFrameEntity> itemFrames = this.mc
         .world
         .getEntitiesByClass(
            ItemFrameEntity.class,
            this.mc.player.getBoundingBox().expand(((Integer)this.scanRange.get()).intValue()),
            framex -> !framex.getHeldItemStack().isEmpty()
         );
      int playerY = this.mc.player.getBlockPos().getY();
      Vec3d playerPos = this.mc.player.getEntityPos();

      for (ItemFrameEntity frame : itemFrames) {
         BlockPos attachedBlockPos = frame.getAttachedBlockPos();
         if (attachedBlockPos.getY() >= playerY && attachedBlockPos.getY() <= playerY + 4) {
            double distance = attachedBlockPos.toCenterPos().distanceTo(playerPos);
            if (!(distance > ((Integer)this.scanRange.get()).intValue())) {
               ItemStack frameHeldItemStack = frame.getHeldItemStack();
               ItemBo itemBo = new ItemBo(frameHeldItemStack);
               Item item = itemBo.getItem();
               StoragePos pos = this.checkAndBuildStoragePos(frame, StorageItem.valueOf(itemBo));
               if (pos != null) {
                  if (item == this.emptyKitItem.get()) {
                     this.emptyKitPos = this.keepNearer(pos, this.emptyKitPos, playerPos);
                  } else if (item == this.finishedKitItem.get()) {
                     this.finishedKitPos = this.keepNearer(pos, this.finishedKitPos, playerPos);
                  } else if (item == this.miscKitItem.get()) {
                     this.miscKitPos = this.keepNearer(pos, this.miscKitPos, playerPos);
                  } else if (!HeItemUtils.isShulkerBox(item)) {
                     StoragePos oldPos = this.itemPosMap.get(itemBo);
                     StoragePos nearerPos = this.keepNearer(pos, oldPos, playerPos);
                     if (nearerPos == pos) {
                        this.itemPosMap.put(itemBo, pos);
                     }
                  }
               }
            }
         }
      }

      if (this.emptyKitPos == null) {
         this.warning("未检测到<空盒位置>，请在对应展示框放置" + Names.get((Item)this.emptyKitItem.get()), new Object[0]);
         return false;
      } else if (this.finishedKitPos == null) {
         this.warning("未检测到<成品位置>，请在对应展示框放置" + Names.get((Item)this.finishedKitItem.get()), new Object[0]);
         return false;
      } else if (this.miscKitPos == null) {
         this.warning("未检测到<杂盒位置>，请在对应展示框放置" + Names.get((Item)this.miscKitItem.get()), new Object[0]);
         return false;
      } else if (this.itemPosMap.isEmpty()) {
         this.warning("未检测到<物品>位置>", new Object[0]);
         return false;
      } else {
         return true;
      }
   }

   private boolean readTemplateFromMainHand() {
      ItemStack mainHandStack = this.mc.player.getMainHandStack();
      if (!HeItemUtils.isShulkerBox(mainHandStack.getItem())) {
         this.warning("主手未持有潜影盒，请手持模板盒子后重新初始化", new Object[0]);
         return false;
      }

      ShulkerBoxReader reader = new ShulkerBoxReader(mainHandStack);
      if (reader.isEmpty()) {
         this.warning("主手潜影盒为空，请放入模板物品后重新初始化", new Object[0]);
         return false;
      }

      this.templateItemList.clear();

      for (ItemStack itemStack : reader) {
         if (!itemStack.isEmpty()) {
            this.templateItemList.add(new ItemBo(itemStack));
         }
      }

      if (this.templateItemList.isEmpty()) {
         this.warning("未能从主手盒子读取到模板物品", new Object[0]);
         return false;
      } else {
         return true;
      }
   }

   private StoragePos keepNearer(StoragePos newPos, StoragePos oldPos, Vec3d playerPos) {
      if (oldPos == null) {
         return newPos;
      }

      double newDist = newPos.getBtnPos().getSquaredDistance(playerPos);
      double oldDist = oldPos.getBtnPos().getSquaredDistance(playerPos);
      return newDist < oldDist ? newPos : oldPos;
   }

   private void setTakeItem(int index) {
      this.currentItem = this.templateItemList.get(index);
      this.curOperationPos = this.itemPosMap.get(this.currentItem);
      this.delayCloseNext(Steps.TAKE_ITEM);
   }

   private void setTakeItem(ItemBo putItem) {
      this.currentItem = putItem;
      this.curOperationPos = this.itemPosMap.get(putItem);
      this.delayCloseNext(Steps.TAKE_ITEM);
   }

   private void setBreakFullKit() {
      this.info("kit已满，挖掉存放", new Object[0]);
      this.takeItemIndex = 0;
      this.putItemIndex = 0;
      this.curOperationPos = this.finishedKitPos;
      this.delayCloseNext(Steps.BREAK_KIT);
   }

   private void clear() {
      this.itemPosMap.clear();
      this.emptyKitPos = null;
      this.finishedKitPos = null;
      this.miscKitPos = null;
   }

   @Override
   public void onDeactivate() {
      super.onDeactivate();
      this.takeItemIndex = 0;
      this.putItemIndex = 0;
      this.currentItem = null;
      this.curOperationPos = null;
      HeInvUtils.closeCurScreen();
   }
}
