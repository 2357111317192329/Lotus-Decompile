package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.ItemQty;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import com.xiaohe66.mc.meteor.lotus.util.WarehouseHelper;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoKit extends WarehouseModule {
    private final Setting<Integer> scanRange;
    private final Setting<Integer> operationCount;
    private final Setting<Integer> takeEmptyCount;
    private final Setting<Item> emptyBoxMarker;
    private final Setting<Item> finishBoxMarker;
    private final Setting<Item> miscBoxMarker;
    private final Setting<Boolean> initSetting;
    private final WarehouseHelper warehouseHelper = new WarehouseHelper();
    private StoragePos emptyPos;
    private StoragePos finishPos;
    private StoragePos miscPos;
    private final List<ItemQty> template = new ArrayList<>(27);
    private final Map<ItemBo, Integer> needed = new HashMap<>();
    private int templateIndex;
    private ItemBo currentItem;
    private StoragePos currentPos;
    private StoragePos breakPos;

    public AutoKit() {
        super("S自动Kit", "自动装配Kit, 需要在<简易仓库>中使用。由资深猎人<dadou>友情赞助开发。");
        this.scanRange = sgGeneral.add(new IntSetting.Builder().name("扫描范围").description("检测展示框和箱子的范围").defaultValue(50).min(1).sliderMax(100).build());
        this.operationCount = sgGeneral.add(new IntSetting.Builder().name("操作数").description("每次操作的物品数量").min(1).sliderMax(9).defaultValue(9).build());
        this.takeEmptyCount = sgGeneral.add(new IntSetting.Builder().name("拿取空盒数").description("拿取空盒时, 需要拿的空盒数量").min(1).sliderMax(9).defaultValue(2).build());
        this.emptyBoxMarker = sgGeneral.add(new ItemSetting.Builder().name("空盒标识").description("空盒箱子展示框上的物品").filter(HeItemUtils::isShulkerBox).defaultValue(Items.SHULKER_BOX).build());
        this.finishBoxMarker = sgGeneral.add(new ItemSetting.Builder().name("成品标识").description("成品kit箱子展示框上的物品").filter(HeItemUtils::isShulkerBox).defaultValue(Items.LIGHT_BLUE_SHULKER_BOX).build());
        this.miscBoxMarker = sgGeneral.add(new ItemSetting.Builder().name("杂盒标识").description("杂盒箱子展示框上的物品").filter(HeItemUtils::isShulkerBox).defaultValue(Items.WHITE_SHULKER_BOX).build());
        this.initSetting = sgGeneral.add(new BoolSetting.Builder().name("初始化").description("使用前需要先初始化，保存所有箱子位置并读取主手盒子作为模板").defaultValue(false).onChanged(this::doInit).build());
        this.templateIndex = 0;
        this.addStep(Steps.NEXT, this::dispatch);
        this.addStep(Steps.PUT_ITEM, this::putItem);
        this.addStep(Steps.TAKE_ITEM, this::takeItem);
        this.addStep(Steps.PUT_EMPTY_KIT, () -> this.putKit(this.emptyPos));
        this.addStep(Steps.TAKE_EMPTY_KIT, () -> this.takeEmptyKit(this.emptyPos, this.takeEmptyCount.get()));
        this.addStep(Steps.PLACE_EMPTY_KIT, () -> {
            this.placeEmptyKit(this.finishPos);
            this.templateIndex = 0;
        });
        this.addStep(Steps.PLACE_KIT, () -> this.placeKit(this.currentPos, box -> box.readItemQtyMap().containsKey(this.currentItem)));
        this.addStep(Steps.TAKE_KIT, () -> this.takeKit(this.currentPos, box -> box.hasItem(this.currentItem)));
        this.addStep(Steps.PUT_KIT, this::putFinishKit);
        this.addStep(Steps.PUT_MISC_KIT, this::putMiscKit);
        this.addStep(Steps.BREAK_KIT, () -> this.breakKit(this.breakPos));
    }

    @Override
    protected boolean useQuickStopKeybind() {
        return true;
    }

    @Override
    protected boolean allowQuickStop() {
        return this.step != Steps.NEXT;
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (!this.warehouseHelper.hasConfig()) {
            this.warning("启动前没有初始化, 自动初始化");
            this.doInit(true);
            if (!this.warehouseHelper.hasConfig()) {
                this.warning("自动初始化失败");
                this.toggle();
                return;
            }
        }
        this.templateIndex = 0;
        this.currentItem = null;
        this.currentPos = null;
        this.dispatch();
    }

    private void doInit(Boolean enabled) {
        if (Boolean.TRUE.equals(enabled)) {
            this.initSetting.set(false);
            if (this.mc.player != null && this.mc.world != null) {
                try {
                    boolean success = this.initWarehouse();
                    if (!success) {
                        this.clearKit();
                    }
                } catch (Exception e) {
                    this.error("初始化发生异常 : %s", e.getMessage());
                }
            } else {
                this.warning("玩家或世界未加载");
            }
        }
    }

    private boolean initWarehouse() {
        this.clearKit();
        AutoClearUp autoClearUp = Modules.get().get(AutoClearUp.class);
        this.warehouseHelper.init(this.scanRange.get(), autoClearUp.getEnabledMappings());
        Map<ItemBo, StoragePos> scanResult = this.warehouseHelper.getMappedPositions();
        this.finishPos = scanResult.remove(ItemBo.of(this.finishBoxMarker.get()));
        this.emptyPos = scanResult.remove(ItemBo.of(this.emptyBoxMarker.get()));
        this.miscPos = scanResult.remove(ItemBo.of(this.miscBoxMarker.get()));
        if (this.emptyPos == null) {
            this.warning("未检测到<空盒>位置，请在对应展示框放置" + Names.get(this.emptyBoxMarker.get()));
            return false;
        } else if (this.finishPos == null) {
            this.warning("未检测到<成品盒>位置，请在对应展示框放置" + Names.get(this.finishBoxMarker.get()));
            return false;
        } else if (this.miscPos == null) {
            this.warning("未检测到<杂盒>位置，请在对应展示框放置" + Names.get(this.miscBoxMarker.get()));
            return false;
        } else {
            ItemStack mainHand = this.mc.player.getMainHandStack();
            if (!HeItemUtils.isShulkerBox(mainHand.getItem())) {
                this.warning("主手未持有潜影盒，请手持模板盒子后重新初始化");
                return false;
            }
            ShulkerBoxReader reader = new ShulkerBoxReader(mainHand);
            if (reader.isEmpty()) {
                this.warning("主手潜影盒为空，请放入模板物品后重新初始化");
                return false;
            }
            this.template.clear();
            for (ItemStack stack : reader) {
                if (stack.isEmpty()) {
                    this.template.add(null);
                } else {
                    ItemBo itemBo = new ItemBo(stack);
                    int count = stack.getCount();
                    if (count < stack.getMaxCount()) {
                        int half = stack.getMaxCount() / 2;
                        count = count >= half ? half : 1;
                    }
                    this.template.add(new ItemQty(itemBo, count));
                }
            }
            if (this.template.isEmpty()) {
                this.warning("未能从主手盒子读取到模板物品");
                return false;
            }
            this.warehouseHelper.printIdentified();
            this.info("初始化完毕！");
            return true;
        }
    }

    private void dispatch() {
        List<ItemEntity> itemEntities = this.mc.world.getEntitiesByClass(ItemEntity.class, this.mc.player.getBoundingBox().expand(5.0F), entity -> HeItemUtils.isShulkerBox(entity.getStack().getItem()));
        if (!itemEntities.isEmpty()) {
            ItemEntity itemEntity = itemEntities.getFirst();
            BlockPos targetPos = null;
            if (itemEntity.getY() != (double) this.mc.player.getBlockY()) {
                targetPos = HePosUtils.getBlockPos(itemEntity.getEntityPos());
            }
            if (targetPos == null) {
                targetPos = itemEntity.getBlockPos();
            }
            this.gotoTargetIfNeed(targetPos, 0, Steps.NEXT, "捡kit");
        } else {
            int emptyCount = 0;
            int firstSlot = -1;
            while (true) {
                ShulkerBoxReader box = this.findShulkerInPlayer(boxReader -> true);
                if (box == null) {
                    break;
                }
                if (!box.isEmpty()) {
                    if (box.matchesQtyTemplate(this.template)) {
                        this.step = Steps.PUT_KIT;
                    } else {
                        this.step = Steps.PUT_MISC_KIT;
                    }
                    return;
                }
                int slot = this.getCurPlayerSlot();
                if (firstSlot == slot) {
                    break;
                }
                if (firstSlot == -1) {
                    firstSlot = slot;
                }
                ++emptyCount;
                if (emptyCount >= this.takeEmptyCount.get()) {
                    this.step = Steps.PUT_EMPTY_KIT;
                    return;
                }
            }
            this.needed.clear();
            int end = Math.min(this.template.size(), this.templateIndex + this.operationCount.get());
            for (int i = this.templateIndex; i < end; ++i) {
                ItemQty qty = this.template.get(i);
                this.needed.merge(qty.getItem(), qty.getCount(), Integer::sum);
            }
            if (this.needed.isEmpty()) {
                this.kitFull();
            } else {
                Set<ItemBo> neededSet = this.needed.keySet();
                Map<ItemBo, Integer> owned = HeInvUtils.countItems(this.warehouseHelper.getLockedSlots(), neededSet);
                HashSet<ItemBo> satisfied = new HashSet<>();
                for (Map.Entry<ItemBo, Integer> entry : this.needed.entrySet()) {
                    ItemBo itemBo = entry.getKey();
                    Integer have = owned.get(itemBo);
                    if (have != null) {
                        Integer need = entry.getValue();
                        if (have >= need) {
                            satisfied.add(itemBo);
                        } else {
                            this.needed.put(itemBo, need - have);
                        }
                    }
                }
                if (satisfied.equals(this.needed)) {
                    this.step = Steps.PUT_ITEM;
                } else {
                    for (ItemBo itemBo : satisfied) {
                        this.needed.remove(itemBo);
                    }
                    this.step = Steps.TAKE_ITEM;
                }
            }
        }
    }

    private void putFinishKit() {
        this.putKit(this.finishPos, box -> box.matchesQtyTemplate(this.template));
    }

    private void putMiscKit() {
        this.putKit(this.miscPos, box -> !box.isEmpty() && !box.matchesQtyTemplate(this.template));
    }

    private void takeItem() {
        if (this.needed.isEmpty()) {
            this.info("拿够操作数, 去放");
            this.closeNext(Steps.PUT_ITEM);
        } else {
            ItemBo itemBo = this.warehouseHelper.findNearestUnmapped(this.needed.keySet());
            if (itemBo == null) {
                this.warning("缺少盒装kit: %s", itemBo.getName());
                this.toggle();
            } else {
                this.currentPos = this.warehouseHelper.getUnmappedPosition(itemBo);
                this.currentItem = itemBo;
                BlockState state = this.mc.world.getBlockState(this.currentPos.getKitPos());
                if (state.isAir()) {
                    this.info("需要放kit");
                    this.closeNext(Steps.PLACE_KIT);
                } else if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
                    this.breakStep("<kit位置>被占用");
                } else if (this.notInOperationRange(this.currentPos)) {
                    this.info("[%s]距离不够", this.currentItem.getName());
                    HeInvUtils.closeCurScreen();
                    this.gotoTarget(this.currentPos.getBtnPos(), 0, Steps.TAKE_ITEM);
                } else {
                    this.openChest(this.currentPos.getKitPos(), container -> {
                        Integer need = this.needed.get(itemBo);
                        int takeCount = Math.min(need, itemBo.getItem().getMaxCount());
                        ItemStack stack = this.findScreenStack(s -> itemBo.isSameItem(s) && s.getCount() >= takeCount);
                        if (stack.isEmpty()) {
                            this.breakPos = this.currentPos;
                            this.closeNext(Steps.BREAK_KIT);
                        } else {
                            int count = stack.getCount();
                            this.info("拿取物品: " + Names.get(itemBo.getItem()));
                            InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                            if (need <= count) {
                                this.needed.remove(itemBo);
                            } else {
                                this.needed.put(itemBo, need - count);
                            }
                            this.setDelay();
                        }
                    });
                }
            }
        }
    }

    private void putItem() {
        if (this.templateIndex >= this.template.size()) {
            this.kitFull();
        } else {
            ItemQty qty = this.template.get(this.templateIndex);
            ItemStack stack = this.findPlayerStack(s -> this.warehouseHelper.isUnlockedSlot(this.getCurPlayerSlot()) && matchesTemplate(qty, s));
            if (stack.isEmpty()) {
                this.info("放物品但身上没有, next");
                this.closeNext(Steps.NEXT);
            } else {
                BlockState state = this.mc.world.getBlockState(this.finishPos.getKitPos());
                if (state.isAir()) {
                    this.info("需要补空盒");
                    this.closeNext(Steps.PLACE_EMPTY_KIT);
                } else if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
                    this.closeScreen();
                    this.breakStep("<成品位置>被占用");
                } else if (this.notInOperationRange(this.finishPos)) {
                    this.closeScreen();
                    this.gotoBtnPos(this.finishPos, "<放" + qty.getItem().getName() + ">距离不够, 尝试移动", Steps.PUT_ITEM);
                } else {
                    this.openChest(this.finishPos.getKitPos(), container -> {
                        if (this.isContainerFull()) {
                            this.kitFull();
                        } else {
                            ItemStack slotStack = container.getSlot(this.templateIndex).getStack();
                            if (slotStack.isEmpty()) {
                                int slot = this.getCurPlayerSlot();
                                if (qty.getCount() == stack.getCount()) {
                                    InvUtils.shiftClick().slot(slot);
                                } else if (qty.getCount() == 1) {
                                    HeInvUtils.moveOneFromSlot(slot, this.templateIndex);
                                } else {
                                    HeInvUtils.moveHalfFromSlot(slot, this.templateIndex);
                                }
                                this.info("放入[%s]x[%s]", qty.getItem().getName(), qty.getCount());
                                this.setDelay();
                            } else if (!qty.equals(new ItemQty(slotStack))) {
                                this.kitFull();
                                return;
                            }
                            ++this.templateIndex;
                        }
                    });
                }
            }
        }
    }

    private void kitFull() {
        this.info("kit已满，挖掉存放");
        this.templateIndex = 0;
        this.breakPos = this.finishPos;
        this.delayCloseNext(Steps.BREAK_KIT);
    }

    private static boolean matchesTemplate(ItemQty qty, ItemStack stack) {
        return qty.getItem().isSameItem(stack) && stack.getCount() % qty.getCount() == 0;
    }

    private void clearKit() {
        this.warehouseHelper.clear();
        this.emptyPos = null;
        this.finishPos = null;
        this.miscPos = null;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        this.templateIndex = 0;
        this.currentItem = null;
        this.currentPos = null;
        HeInvUtils.closeCurScreen();
    }
}
