/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.ItemSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  meteordevelopment.meteorclient.utils.player.FindItemResult
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  net.minecraft.util.Hand
 *  net.minecraft.entity.decoration.ItemFrameEntity
 *  net.minecraft.entity.ItemEntity
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Position
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.ShulkerBoxBlock
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.block.BlockState
 *  net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
 *  net.minecraft.util.hit.BlockHitResult
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.modules.WalkModule;

import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.ItemQty;
import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.util.Hand;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.network.packet.Packet;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;

public class AutoKit extends WalkModule {
    private final Setting<Integer> scanRange = sgGeneral.add(new IntSetting.Builder()
        .name("扫描范围")
        .description("检测展示框和箱子的范围")
        .defaultValue(50)
        .min(1)
        .sliderMax(100)
        .build());
    private final Setting<Integer> qty = sgGeneral.add(new IntSetting.Builder()
        .name("操作数")
        .description("每次操作的物品数量")
        .min(1)
        .sliderMax(9)
        .defaultValue(9)
        .build());
    private final Setting<Item> emptyKitItem = sgGeneral.add(new ItemSetting.Builder()
        .name("空盒标识")
        .description("空盒箱子展示框上的物品")
        .defaultValue(Items.WHITE_SHULKER_BOX)
        .build());
    private final Setting<Item> finishedKitItem = sgGeneral.add(new ItemSetting.Builder()
        .name("成品标识")
        .description("成品kit箱子展示框上的物品")
        .defaultValue(Items.LIGHT_BLUE_SHULKER_BOX)
        .build());
    private final Setting<Item> miscKitItem = sgGeneral.add(new ItemSetting.Builder()
        .name("杂盒标识")
        .description("杂盒箱子展示框上的物品")
        .defaultValue(Items.BLUE_SHULKER_BOX)
        .build());
    private final Setting<Boolean> initBtn = sgGeneral.add(new BoolSetting.Builder()
        .name("初始化")
        .description("使用前需要先初始化，保存所有箱子位置并读取主手盒子作为模板")
        .defaultValue(false)
        .onChanged(this::init)
        .build());
    private final Map<ItemBo, StoragePos> itemPosMap;
    private StoragePos emptyKitPos;
    private StoragePos finishedKitPos;
    private StoragePos miscKitPos;
    private final List<ItemQty> templateItemQtyList;
    private final Map<ItemBo, Integer> needItemMap;
    private int itemIndex;
    private ItemBo currentItem;
    private StoragePos curOperationPos;
    private int putKitIndex;

    public AutoKit() {
        super("自动Kit", "自动装配Kit, 需要在<简易仓库>中使用。由资深猎人<dadou>友情赞助开发。");
        this.itemPosMap = new LinkedHashMap<ItemBo, StoragePos>();
        this.templateItemQtyList = new ArrayList<ItemQty>(27);
        this.needItemMap = new HashMap<ItemBo, Integer>();
        this.itemIndex = 0;
        this.putKitIndex = -1;
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

    @Override
    protected boolean useQuickStopKeybind() {
        return true;
    }

    @Override
    protected boolean allowQuickStop() {
        return this.step != Steps.WALKING;
    }

    private void init(Boolean enabled) {
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }
        this.initBtn.set(false);
        if (this.mc.player == null || this.mc.world == null) {
            this.warning("玩家或世界未加载", new Object[0]);
            return;
        }
        this.clear();
        boolean scanPositionsSuccess = this.scanPositions();
        if (!scanPositionsSuccess) {
            this.clear();
            return;
        }
        boolean readTemplateSuccess = this.readTemplateFromMainHand();
        if (!readTemplateSuccess) {
            this.clear();
            return;
        }
        boolean missItem = false;
        for (ItemQty itemQty : this.templateItemQtyList) {
            if (this.itemPosMap.containsKey(itemQty.getItem())) continue;
            this.warning("未找到物品<" + itemQty.getItem().getName() + ">的存储位置", new Object[0]);
            missItem = true;
        }
        if (missItem) {
            this.clear();
            return;
        }
        String itemNames = this.itemPosMap.keySet().stream().map(ItemBo::getName).collect(Collectors.joining(","));
        this.info("成功识别: " + itemNames, new Object[0]);
        this.info("初始化完毕！", new Object[0]);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (this.itemPosMap.isEmpty() || this.emptyKitPos == null || this.finishedKitPos == null) {
            this.warning("启动前没有初始化, 自动初始化", new Object[0]);
            this.init(false);
            if (this.itemPosMap.isEmpty() || this.emptyKitPos == null || this.finishedKitPos == null) {
                this.warning("自动初始化失败", new Object[0]);
                this.toggle();
                return;
            }
        }
        this.itemIndex = 0;
        this.currentItem = null;
        this.curOperationPos = null;
        this.nextStep();
    }

    private void nextStep() {
        ItemStack nextKit = this.nextPlayerStack((ItemStack itemStack) -> HeItemUtils.isShulkerBox(itemStack.getItem()));
        if (!nextKit.isEmpty()) {
            this.putKitIndex = this.getCurPlayerSlot();
            ShulkerBoxReader reader = new ShulkerBoxReader(nextKit);
            if (reader.matchesQtyTemplate(this.templateItemQtyList)) {
                this.curOperationPos = this.finishedKitPos;
                this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放成品kit");
            } else if (reader.isEmpty()) {
                this.curOperationPos = this.emptyKitPos;
                this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放空盒");
            } else {
                ItemBo firstItemBo;
                if (reader.isSameItem() && this.itemPosMap.containsKey(firstItemBo = reader.getFirstItemBo())) {
                    this.curOperationPos = this.itemPosMap.get(firstItemBo);
                    this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放满盒");
                    return;
                }
                this.curOperationPos = this.miscKitPos;
                this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放杂盒");
            }
            return;
        }
        List<ItemEntity> shulkerEntities = this.mc.world.getEntitiesByClass(ItemEntity.class, this.mc.player.getBoundingBox().expand(5.0), e -> HeItemUtils.isShulkerBox(e.getStack().getItem()));
        if (!shulkerEntities.isEmpty()) {
            BlockPos testPos;
            BlockPos canStandPos;
            BlockPos targetPos = shulkerEntities.getFirst().getBlockPos();
            if (targetPos.getY() != this.mc.player.getBlockY() && (canStandPos = HeBlockUtils.getCanStandPos(testPos = new BlockPos(targetPos.getX(), this.mc.player.getBlockY(), targetPos.getZ()), 1)) != null) {
                targetPos = canStandPos;
            }
            this.gotoTargetIfNeed(targetPos, 0, Steps.NEXT, "捡kit");
            return;
        }
        this.step = Steps.PUT_ITEM;
    }

    private void putKit() {
        if (this.notInOperationRange(this.curOperationPos)) {
            this.gotoBtnPos(this.curOperationPos, "<存kit>距离不够，尝试移动", Steps.PUT_KIT);
            return;
        }
        ItemStack kitItemStack = this.getItemStack(this.putKitIndex);
        if (!HeItemUtils.isShulkerBox(kitItemStack.getItem())) {
            this.warning("放kit, 但身上没有...", new Object[0]);
            this.step = Steps.NEXT;
            return;
        }
        this.openChest(this.curOperationPos.getPutPos(), (ScreenHandler inventory) -> {
            FindItemResult emptyResult = InvUtils.findEmpty();
            if (emptyResult.found()) {
                InvUtils.shiftClick().slot(this.putKitIndex);
                this.delayCloseNext(Steps.NEXT);
            } else {
                this.breakStep("<存kit>箱满，无法存放");
            }
        });
    }

    private void takeEmptyKit() {
        if (this.notInOperationRange(this.emptyKitPos)) {
            this.gotoBtnPos(this.emptyKitPos, "<拿空盒>距离不够，尝试移动", Steps.TAKE_EMPTY_KIT);
            return;
        }
        this.openChest(this.emptyKitPos.getTakePos(), (ScreenHandler inventory) -> {
            ItemStack nextScreenStack = this.nextScreenStack((ItemStack itemStack) -> {
                if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                    ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
                    return reader.isEmpty();
                }
                return false;
            });
            if (!nextScreenStack.isEmpty()) {
                this.info("拿取空盒", new Object[0]);
                InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                this.delayCloseNext(Steps.PLACE_EMPTY_KIT);
                return;
            }
            this.breakStep("<空盒箱>缺少空潜影盒");
        });
    }

    private void placeEmptyKit() {
        BlockState finishedKiBlockState = this.mc.world.getBlockState(this.finishedKitPos.getKitPos());
        if (finishedKiBlockState.getBlock() instanceof ShulkerBoxBlock) {
            this.step = Steps.PUT_ITEM;
            return;
        }
        if (!finishedKiBlockState.isAir()) {
            this.breakStep("<位置>被占用: " + Names.get(this.finishedKitPos.getItem().getItem()));
            return;
        }
        FindItemResult findItemResult = HeInvUtils.findShulkerBox(Items.AIR);
        if (!findItemResult.found()) {
            this.step = Steps.TAKE_EMPTY_KIT;
            return;
        }
        if (!findItemResult.isMainHand()) {
            HeInvUtils.swap(findItemResult.slot(), this.getMainSlot());
            this.setDelay();
            return;
        }
        if (this.notInOperationRange(this.finishedKitPos)) {
            this.gotoTarget(this.finishedKitPos.getBtnPos(), 0, Steps.PLACE_EMPTY_KIT);
            return;
        }
        this.info("放置空盒", new Object[0]);
        HeBlockUtils.place(this.finishedKitPos.getKitPos(), this.getMainSlot(), true, Direction.DOWN);
        this.itemIndex = 0;
        this.delayNext(Steps.PUT_ITEM);
    }

    private void takeKit() {
        if (this.notInOperationRange(this.curOperationPos)) {
            this.gotoBtnPos(this.curOperationPos, "<拿kit>距离不够，尝试移动", Steps.TAKE_KIT);
            return;
        }
        this.openChest(this.curOperationPos.getTakePos(), (ScreenHandler inventory) -> {
            ItemStack nextScreenStack = this.nextScreenStack((ItemStack itemStack) -> {
                if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                    ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
                    return reader.hasItem(this.currentItem);
                }
                return false;
            });
            if (nextScreenStack.isEmpty()) {
                this.breakStep("<" + Names.get(this.currentItem.getItem()) + ">库存不足");
                return;
            }
            this.info("拿取kit: " + Names.get(this.currentItem.getItem()), new Object[0]);
            InvUtils.shiftClick().slotId(this.getCurScreenSlot());
            this.delayCloseNext(Steps.PLACE_KIT);
        });
    }

    private void placeKit() {
        BlockState finishedKitBlockState = this.mc.world.getBlockState(this.curOperationPos.getKitPos());
        if (!finishedKitBlockState.isAir()) {
            if (finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock) {
                this.step = Steps.TAKE_ITEM;
            } else {
                this.breakStep("<kit位置>被占用");
            }
            return;
        }
        ItemStack nextItemStack = this.nextPlayerStack((ItemStack itemStack) -> HeItemUtils.isShulkerBox(itemStack.getItem()));
        if (nextItemStack.isEmpty()) {
            this.step = Steps.TAKE_KIT;
            return;
        }
        if (this.notInOperationRange(this.curOperationPos)) {
            this.gotoBtnPos(this.curOperationPos, "去拿kit", Steps.PLACE_KIT);
            return;
        }
        if (this.swapToMainHand(this.getCurPlayerSlot())) {
            return;
        }
        this.info("放置盒子:" + Names.get(this.curOperationPos.getItem().getItem()), new Object[0]);
        HeBlockUtils.place(this.curOperationPos.getKitPos(), this.getMainSlot(), true, Direction.DOWN);
        this.delayNext(Steps.TAKE_ITEM);
    }

    private void takeItem() {
        ItemBo itemBo;
        ItemBo nearestItemBo;
        if (this.needItemMap.isEmpty()) {
            ItemQty itemQty;
            int endIndex = Math.min(this.templateItemQtyList.size(), this.itemIndex + this.qty.get());
            for (int index = this.itemIndex; index < endIndex; ++index) {
                itemQty = this.templateItemQtyList.get(index);
                this.needItemMap.merge(itemQty.getItem(), itemQty.getCount(), Integer::sum);
            }
            if (!this.needItemMap.isEmpty()) {
                for (int slot = 0; slot < 36; ++slot) {
                    Integer remainCount;
                    ItemStack itemStack = this.getItemStack(slot);
                    nearestItemBo = new ItemBo(itemStack);
                    remainCount = this.needItemMap.get(nearestItemBo);
                    if (remainCount == null) continue;
                    if (itemStack.getCount() >= remainCount) {
                        this.needItemMap.remove(nearestItemBo);
                        continue;
                    }
                    this.needItemMap.put(nearestItemBo, remainCount - itemStack.getCount());
                }
            }
            if (this.needItemMap.isEmpty()) {
                HeInvUtils.closeCurScreen();
                this.step = Steps.PUT_ITEM;
                return;
            }
        }
        Vec3d playerPos = this.mc.player.getEntityPos();
        double nearestDistance = Double.MAX_VALUE;
        nearestItemBo = null;
        for (ItemBo needItemBo : this.needItemMap.keySet()) {
            StoragePos storagePos = this.itemPosMap.get(needItemBo);
            double distance = playerPos.distanceTo(storagePos.getBtnPos().toCenterPos());
            if (!(distance < nearestDistance)) continue;
            nearestDistance = distance;
            nearestItemBo = needItemBo;
        }
        this.curOperationPos = this.itemPosMap.get(nearestItemBo);
        BlockState kitBlockState = this.mc.world.getBlockState(this.curOperationPos.getKitPos());
        if (kitBlockState.isAir()) {
            this.currentItem = nearestItemBo;
            this.step = Steps.PLACE_KIT;
            return;
        }
        if (!(kitBlockState.getBlock() instanceof ShulkerBoxBlock)) {
            this.breakStep("<kit位置>被占用");
            return;
        }
        if (this.notInOperationRange(this.curOperationPos)) {
            HeInvUtils.closeCurScreen();
            this.gotoTarget(this.curOperationPos.getBtnPos(), 0, Steps.TAKE_ITEM);
            return;
        }
        itemBo = nearestItemBo;
        this.openChest(this.curOperationPos.getKitPos(), (ScreenHandler screenHandler) -> {
            Integer needCount = this.needItemMap.get(itemBo);
            int takeCount = Math.min(needCount, itemBo.getItem().getMaxCount());
            ItemStack nextItemStack = this.nextScreenStack((ItemStack itemStack) -> AutoKit.matchesItem(itemBo, takeCount, itemStack));
            if (nextItemStack.isEmpty()) {
                this.info("kit已空，挖掉重放: " + Names.get(itemBo.getItem()), new Object[0]);
                this.currentItem = itemBo;
                this.delayCloseNext(Steps.BREAK_KIT);
                return;
            }
            int gotCount = nextItemStack.getCount();
            this.info("拿取物品: " + Names.get(itemBo.getItem()), new Object[0]);
            InvUtils.shiftClick().slotId(this.getCurScreenSlot());
            if (needCount <= gotCount) {
                this.needItemMap.remove(itemBo);
            } else {
                this.needItemMap.put(itemBo, needCount - gotCount);
            }
            this.setDelay();
        });
    }

    private void putItem() {
        ItemQty itemQty = this.templateItemQtyList.get(this.itemIndex);
        ItemStack nextItemStack = this.nextPlayerStack((ItemStack itemStack) -> AutoKit.matchesQty(itemQty, itemStack));
        if (nextItemStack.isEmpty()) {
            this.closeScreen();
            this.needItemMap.clear();
            this.step = Steps.TAKE_ITEM;
            return;
        }
        BlockState finishedKitBlockState = this.mc.world.getBlockState(this.finishedKitPos.getKitPos());
        if (finishedKitBlockState.isAir()) {
            this.curOperationPos = this.finishedKitPos;
            this.closeScreen();
            this.step = Steps.PLACE_EMPTY_KIT;
            return;
        }
        if (!(finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock)) {
            this.closeScreen();
            this.breakStep("<成品位置>被占用");
            return;
        }
        if (this.notInOperationRange(this.finishedKitPos)) {
            this.closeScreen();
            this.gotoBtnPos(this.finishedKitPos, "<放" + itemQty.getItem().getName() + ">距离不够, 尝试移动", Steps.PUT_ITEM);
            return;
        }
        this.openChest(this.finishedKitPos.getKitPos(), (ScreenHandler screenHandler) -> {
            if (this.hasScreenFull()) {
                this.setBreakFullKit();
                return;
            }
            ItemStack itemStack = screenHandler.getSlot(this.itemIndex).getStack();
            boolean isEmpty = itemStack.isEmpty();
            if (isEmpty) {
                int playerSlot = this.getCurPlayerSlot();
                if (itemQty.getCount() == itemStack.getCount()) {
                    InvUtils.shiftClick().slot(playerSlot);
                } else if (itemQty.getCount() == 1) {
                    HeInvUtils.moveOneFromIndex(playerSlot, this.itemIndex);
                } else {
                    HeInvUtils.moveHalfFromIndex(playerSlot, this.itemIndex);
                }
            }
            ++this.itemIndex;
            if (this.itemIndex >= this.templateItemQtyList.size()) {
                this.setBreakFullKit();
            } else if (!isEmpty) {
                this.setDelay();
            }
        });
    }

    private void breakKit() {
        if (this.notInOperationRange(this.curOperationPos)) {
            this.gotoBtnPos(this.curOperationPos, "<挖盒子>距离不够, 尝试移动", Steps.BREAK_KIT);
            return;
        }
        Vec3d hitPos = Vec3d.ofCenter(this.curOperationPos.getBtnPos()).add(0.0, -0.5, 0.0);
        BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, this.curOperationPos.getBtnPos(), false);
        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0);
        this.mc.getNetworkHandler().sendPacket(packet);
        this.step = Steps.NEXT;
        this.setDelay(40);
    }

    private boolean scanPositions() {
        List<ItemFrameEntity> itemFrames = this.mc.world.getEntitiesByClass(ItemFrameEntity.class, this.mc.player.getBoundingBox().expand((double)this.scanRange.get()), framex -> !framex.getHeldItemStack().isEmpty());
        int playerY = this.mc.player.getBlockPos().getY();
        Vec3d playerPos = this.mc.player.getEntityPos();
        for (ItemFrameEntity frame : itemFrames) {
            StoragePos oldPos;
            StoragePos nearerPos;
            double distance;
            BlockPos attachedBlockPos = frame.getAttachedBlockPos();
            if (attachedBlockPos.getY() < playerY || attachedBlockPos.getY() > playerY + 4 || (distance = attachedBlockPos.toCenterPos().distanceTo(playerPos)) > (double)this.scanRange.get()) continue;
            ItemStack frameHeldItemStack = frame.getHeldItemStack();
            ItemBo itemBo = new ItemBo(frameHeldItemStack);
            Item item = itemBo.getItem();
            StoragePos pos = this.checkAndBuildStoragePos(frame, com.xiaohe66.mc.meteor.lotus.bo.StorageItem.valueOf(itemBo));
            if (pos == null) continue;
            if (item == this.emptyKitItem.get()) {
                this.emptyKitPos = this.keepNearer(pos, this.emptyKitPos, playerPos);
                continue;
            }
            if (item == this.finishedKitItem.get()) {
                this.finishedKitPos = this.keepNearer(pos, this.finishedKitPos, playerPos);
                continue;
            }
            if (item == this.miscKitItem.get()) {
                this.miscKitPos = this.keepNearer(pos, this.miscKitPos, playerPos);
                continue;
            }
            if (HeItemUtils.isShulkerBox(item) || (nearerPos = this.keepNearer(pos, oldPos = this.itemPosMap.get(itemBo), playerPos)) != pos) continue;
            this.itemPosMap.put(itemBo, pos);
        }
        if (this.emptyKitPos == null) {
            this.warning("未检测到<空盒位置>，请在对应展示框放置" + Names.get(this.emptyKitItem.get()), new Object[0]);
            return false;
        }
        if (this.finishedKitPos == null) {
            this.warning("未检测到<成品位置>，请在对应展示框放置" + Names.get(this.finishedKitItem.get()), new Object[0]);
            return false;
        }
        if (this.miscKitPos == null) {
            this.warning("未检测到<杂盒位置>，请在对应展示框放置" + Names.get(this.miscKitItem.get()), new Object[0]);
            return false;
        }
        if (this.itemPosMap.isEmpty()) {
            this.warning("未检测到<物品>位置>", new Object[0]);
            return false;
        }
        return true;
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
        this.templateItemQtyList.clear();
        Iterator<ItemStack> iterator = reader.iterator();
        while (iterator.hasNext()) {
            ItemStack itemStack = iterator.next();
            if (itemStack.isEmpty()) continue;
            ItemBo itemBo = new ItemBo(itemStack);
            int count = itemStack.getCount();
            if (count < itemStack.getMaxCount()) {
                int halfMaxCount = itemStack.getMaxCount() / 2;
                count = count >= halfMaxCount ? halfMaxCount : 1;
            }
            this.templateItemQtyList.add(new ItemQty(itemBo, count));
        }
        if (this.templateItemQtyList.isEmpty()) {
            this.warning("未能从主手盒子读取到模板物品", new Object[0]);
            return false;
        }
        return true;
    }

    private StoragePos keepNearer(StoragePos newPos, StoragePos oldPos, Vec3d playerPos) {
        double oldDistance;
        if (oldPos == null) {
            return newPos;
        }
        double newDistance = newPos.getBtnPos().getSquaredDistance((Position)playerPos);
        return newDistance < (oldDistance = oldPos.getBtnPos().getSquaredDistance((Position)playerPos)) ? newPos : oldPos;
    }

    private void setBreakFullKit() {
        this.info("kit已满，挖掉存放", new Object[0]);
        this.itemIndex = 0;
        this.curOperationPos = this.finishedKitPos;
        this.delayCloseNext(Steps.BREAK_KIT);
    }

    private static boolean matchesQty(ItemQty itemQty, ItemStack itemStack) {
        return itemQty.getItem().isSameItem(itemStack) && itemStack.getCount() % itemQty.getCount() == 0;
    }

    private static boolean matchesItem(ItemBo itemBo, int count, ItemStack itemStack) {
        return itemBo.isSameItem(itemStack) && itemStack.getCount() >= count;
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
        this.itemIndex = 0;
        this.currentItem = null;
        this.curOperationPos = null;
        HeInvUtils.closeCurScreen();
    }
}
