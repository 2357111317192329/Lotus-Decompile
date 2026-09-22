package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.clearup.ClearUpMapping;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoStockUp extends WalkModule {
    private final Setting<Integer> scanRange;
    private final Setting<Integer> operationCount;
    private final Setting<Integer> wholeBoxThreshold;
    private final Setting<Item> emptyBoxMarker;
    private final Setting<Item> stockBoxMarker;
    private final Setting<Boolean> initSetting;
    private final Map<ItemBo, StoragePos> itemPositions = new HashMap<>();
    private final Map<Item, StoragePos> mappedItemPositions = new HashMap<>();
    private StoragePos emptyPos;
    private StoragePos stockPos;
    private final Map<ItemBo, Integer> needed = new LinkedHashMap<>();
    private final Map<ItemBo, Integer> taken = new HashMap<>();
    private final Set<ItemBo> skipped = new HashSet<>();
    private ItemBo currentItem;
    private StoragePos currentPos;
    private StoragePos breakPos;
    private int operationCounter;
    private int checkCounter;
    private int phaseCounter;

    public AutoStockUp() {
        super("S自动备货", "根据当前激活的投影取出所有需要的材料, 需要在<简易仓库>中使用。缺少的物品会跳过, 从需求量最少的物品开始备货。");
        this.scanRange = sgGeneral.add(new IntSetting.Builder().name("扫描范围").description("检测展示框和箱子的范围").defaultValue(50).min(1).sliderMax(100).build());
        this.operationCount = sgGeneral.add(new IntSetting.Builder().name("操作数").description("每次操作的物品数量").min(1).sliderMax(18).defaultValue(9).build());
        this.wholeBoxThreshold = sgGeneral.add(new IntSetting.Builder().name("整盒阈值(组)").description("当不满一盒部分超过多少组时, 备货直接拿整盒物品").defaultValue(9).min(1).sliderMax(27).build());
        this.emptyBoxMarker = sgGeneral.add(new ItemSetting.Builder().name("空盒标识").description("空盒箱子展示框上的物品").defaultValue(Items.SHULKER_BOX).build());
        this.stockBoxMarker = sgGeneral.add(new ItemSetting.Builder().name("备货标识").description("备货箱子展示框上的物品, 取出的物品会存放这里").defaultValue(Items.GREEN_SHULKER_BOX).build());
        this.initSetting = sgGeneral.add(new BoolSetting.Builder().name("初始化").description("使用前需要先初始化，扫描所有箱子位置并统计当前激活投影的材料").defaultValue(false).onChanged(this::doInit).build());
        this.operationCounter = 0;
        this.addStep(Steps.NEXT, this::dispatch);
        this.addStep(Steps.CHECK, this::checkPrepared);
        this.addStep(Steps.PUT_ITEM, this::putItem);
        this.addStep(Steps.PUT_KIT, this::putKit);
        this.addStep(Steps.TAKE_ITEM, this::takeItem);
        this.addStep(Steps.TAKE_KIT, this::takeKit);
        this.addStep(Steps.PLACE_KIT, this::placeKit);
        this.addStep(Steps.BREAK_KIT, this::breakKit);
        this.addStep(Steps.PLACE_EMPTY_KIT, this::placeEmptyKit);
        this.addStep(Steps.TAKE_EMPTY_KIT, this::takeEmptyKit);
    }

    @Override
    protected boolean useQuickStopKeybind() {
        return true;
    }

    @Override
    protected boolean allowQuickStop() {
        return this.step != Steps.WALKING;
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (this.stockPos == null || this.needed.isEmpty()) {
            this.warning("启动前没有初始化, 自动初始化");
            this.doInit(true);
            if (this.stockPos == null || this.needed.isEmpty()) {
                this.warning("自动初始化失败");
                this.toggle();
                return;
            }
        }
        this.currentItem = null;
        this.currentPos = null;
        this.breakPos = null;
        this.operationCounter = 0;
        this.phaseCounter = 0;
        if (this.checkCounter >= 2) {
            this.dispatch();
        } else {
            this.taken.clear();
            this.gotoTargetIfNeed(this.stockPos.getBtnPos(), 0, Steps.CHECK, "检查已备好的物品");
        }
    }

    private void doInit(Boolean enabled) {
        if (Boolean.TRUE.equals(enabled)) {
            this.initSetting.set(false);
            if (this.mc.player != null && this.mc.world != null) {
                this.clear();
                this.checkCounter = 0;
                Map<ItemBo, StoragePos> scanResult = HePosUtils.scanFramesPiston(this.scanRange.get(), 4);
                this.mappedItemPositions.clear();
                AutoClearUp autoClearUp = Modules.get().get(AutoClearUp.class);
                Map<Item, ClearUpMapping> mappings = autoClearUp.getEnabledMappings();
                for (Map.Entry<Item, ClearUpMapping> entry : mappings.entrySet()) {
                    ClearUpMapping mapping = entry.getValue();
                    StoragePos pos = scanResult.get(ItemBo.of(mapping.getTargetItem()));
                    if (pos != null) {
                        for (Item related : mapping.getRelatedItems()) {
                            this.mappedItemPositions.put(related, pos);
                        }
                    }
                }
                this.emptyPos = scanResult.remove(ItemBo.of(this.emptyBoxMarker.get()));
                this.stockPos = scanResult.remove(ItemBo.of(this.stockBoxMarker.get()));
                if (this.emptyPos == null) {
                    this.warning("未检测到<空盒>位置, 请在任意展示框放置" + Names.get(this.emptyBoxMarker.get()));
                } else if (this.stockPos == null) {
                    this.warning("未检测到<备货>位置, 请在任意展示框放置" + Names.get(this.stockBoxMarker.get()));
                } else {
                    this.itemPositions.clear();
                    for (Map.Entry<ItemBo, StoragePos> entry : scanResult.entrySet()) {
                        ItemBo itemBo = entry.getKey();
                        if (!mappings.containsKey(itemBo.getItem())) {
                            this.itemPositions.put(itemBo, entry.getValue());
                        }
                    }
                    if (this.itemPositions.isEmpty()) {
                        this.warning("未检测到<物品>位置");
                    } else {
                        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
                        if (placement != null && placement.isEnabled()) {
                            List<MaterialListEntry> materialList = this.getMaterialList(placement);
                            if (materialList == null) {
                                this.warning("投影世界未加载");
                            } else {
                                HashMap<ItemBo, Integer> materials = new HashMap<>();
                                for (MaterialListEntry entry : materialList) {
                                    ItemBo itemBo = new ItemBo(entry.getStack());
                                    materials.merge(itemBo, entry.getCountTotal(), Integer::sum);
                                }
                                if (materials.isEmpty()) {
                                    this.warning("投影<" + placement.getName() + ">没有需要的材料");
                                } else {
                                    this.needed.clear();
                                    materials.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).forEach(e -> this.needed.put(e.getKey(), e.getValue()));
                                    String list = this.needed.keySet().stream().map(itemBo -> !this.itemPositions.containsKey(itemBo) && !this.mappedItemPositions.containsKey(itemBo.getItem()) ? itemBo.getName() + "(缺位置)" : itemBo.getName()).collect(Collectors.joining(","));
                                    this.info("投影材料: " + list);
                                    this.info("初始化完毕！");
                                }
                            }
                        } else {
                            this.warning("没有激活的投影, 请先在投影中选中一个投影放置");
                        }
                    }
                }
            } else {
                this.warning("玩家或世界未加载");
            }
        }
    }

    private List<MaterialListEntry> getMaterialList(SchematicPlacement placement) {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (worldSchematic == null) {
            return null;
        }
        Object2IntOpenHashMap<BlockState> blockCounts = new Object2IntOpenHashMap<>();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (Box box : placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values()) {
            BlockPos pos1 = box.getPos1();
            BlockPos pos2 = box.getPos2();
            int minX = Math.min(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    for (int x = minX; x <= maxX; ++x) {
                        mutable.set(x, y, z);
                        BlockState state = worldSchematic.getBlockState(mutable);
                        if (!state.isAir()) {
                            blockCounts.addTo(state, 1);
                        }
                    }
                }
            }
        }
        return MaterialListUtils.getMaterialList(blockCounts, blockCounts.clone(), new Object2IntOpenHashMap<>(), this.mc.player);
    }

    private void checkPrepared() {
        if (this.checkCounter >= 2) {
            this.closeNext(Steps.NEXT);
        } else if (this.notInOperationRange(this.stockPos)) {
            this.gotoBtnPos(this.stockPos, "<检查备货>距离不够, 尝试移动", Steps.CHECK);
        } else {
            BlockPos pos;
            if (this.checkCounter == 0) {
                pos = this.stockPos.getTakePos();
            } else {
                pos = this.stockPos.getKitPos();
                BlockState state = this.mc.world.getBlockState(pos);
                if (!HeItemUtils.isShulkerBox(state.getBlock().asItem())) {
                    this.closeNext(Steps.NEXT);
                    return;
                }
            }
            this.openChest(pos, container -> {
                HashMap<ItemBo, Integer> found = new HashMap<>();
                for (int i = 0; i < this.getScreenMainSize(); ++i) {
                    ItemStack stack = container.getSlot(i).getStack();
                    if (!stack.isEmpty()) {
                        if (HeItemUtils.isShulkerBox(stack.getItem())) {
                            ShulkerBoxReader reader = new ShulkerBoxReader(stack);
                            for (Map.Entry<ItemBo, Integer> entry : reader.readItemQtyMap().entrySet()) {
                                found.merge(entry.getKey(), entry.getValue(), Integer::sum);
                            }
                        } else {
                            found.merge(new ItemBo(stack), stack.getCount(), Integer::sum);
                        }
                    }
                }
                if (!found.isEmpty()) {
                    this.needed.entrySet().removeIf(entry -> {
                        Integer have = found.get(entry.getKey());
                        if (have == null) {
                            return false;
                        }
                        int remain = entry.getValue() - have;
                        if (remain <= 0) {
                            this.info("已备好: " + entry.getKey().getName());
                            return true;
                        }
                        entry.setValue(remain);
                        this.info("已备好部分: " + entry.getKey().getName() + ", 还差 x" + remain);
                        return false;
                    });
                }
                ++this.checkCounter;
                this.delayCloseNext(Steps.CHECK);
            });
        }
    }

    private void dispatch() {
        List<ItemEntity> itemEntities = this.mc.world.getEntitiesByClass(ItemEntity.class, this.mc.player.getBoundingBox().expand(5.0F), entity -> HeItemUtils.isShulkerBox(entity.getStack().getItem()));
        if (!itemEntities.isEmpty()) {
            BlockPos targetPos = itemEntities.getFirst().getBlockPos();
            if (targetPos.getY() != this.mc.player.getBlockY()) {
                BlockPos atPlayerY = new BlockPos(targetPos.getX(), this.mc.player.getBlockY(), targetPos.getZ());
                BlockPos standable = HeBlockUtils.getCanStandPos(atPlayerY, 1);
                if (standable != null) {
                    targetPos = standable;
                }
            }
            this.closeScreen();
            this.gotoTargetIfNeed(targetPos, 0, Steps.PUT_KIT, "捡kit");
        } else {
            if (this.operationCounter >= this.operationCount.get()) {
                this.operationCounter = 0;
                ItemStack stack = this.findPlayerStack(s -> !HeItemUtils.isShulkerBox(s.getItem()) && this.needed.containsKey(new ItemBo(s)));
                if (!stack.isEmpty()) {
                    this.closeNext(Steps.PUT_ITEM);
                    return;
                }
                ShulkerBoxReader box = this.findShulkerInPlayer(this::containsNeeded);
                if (box != null) {
                    this.closeNext(Steps.PUT_KIT);
                    return;
                }
            }
            if (!this.needed.isEmpty() && !this.skipped.containsAll(this.needed.keySet())) {
                StoragePos pos = null;
                Map.Entry<ItemBo, Integer> entry = null;
                for (Map.Entry<ItemBo, Integer> e : this.needed.entrySet()) {
                    this.currentItem = e.getKey();
                    if (this.skipped.contains(this.currentItem)) {
                        continue;
                    }
                    int groups = this.remainingGroups();
                    if (groups > 0) {
                        pos = this.itemPositions.get(this.currentItem);
                        if (pos != null) {
                            entry = e;
                            break;
                        }
                        pos = this.mappedItemPositions.get(this.currentItem.getItem());
                        if (pos != null) {
                            entry = e;
                            break;
                        }
                        this.warning("未找到<" + this.currentItem.getName() + ">的存储位置, 跳过");
                        this.skipped.add(this.currentItem);
                    } else {
                        this.skipped.add(this.currentItem);
                    }
                }
                if (entry != null) {
                    this.phaseCounter = 0;
                    if (pos == this.currentPos) {
                        this.step = Steps.TAKE_ITEM;
                    } else {
                        this.currentPos = pos;
                        this.closeScreen();
                        this.gotoTargetIfNeed(pos.getBtnPos(), 0, Steps.TAKE_ITEM, "取货: " + this.currentItem.getName() + " x" + entry.getValue());
                    }
                }
            } else if (this.operationCounter > 0) {
                this.operationCounter = this.operationCount.get();
            } else {
                this.warning("备货完毕");
                this.toggle();
            }
        }
    }

    private void putItem() {
        BlockState state = this.mc.world.getBlockState(this.stockPos.getKitPos());
        if (state.isAir()) {
            this.closeNext(Steps.PLACE_EMPTY_KIT);
        } else if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            this.closeScreen();
            this.breakStep("<成品位置>被占用");
        } else if (this.notInOperationRange(this.stockPos)) {
            this.gotoBtnPos(this.stockPos, "<存放备货物品>距离不够, 尝试移动", Steps.PUT_ITEM);
        } else {
            ItemStack stack = this.findPlayerStack(s -> !HeItemUtils.isShulkerBox(s.getItem()) && this.needed.containsKey(new ItemBo(s)));
            if (!stack.isEmpty()) {
                this.openChest(this.stockPos.getKitPos(), container -> {
                    if (this.isContainerFull()) {
                        this.breakPos = this.stockPos;
                        this.delayCloseNext(Steps.BREAK_KIT);
                    } else {
                        InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                        this.setDelay();
                    }
                });
            } else {
                this.closeNext(Steps.PUT_KIT);
            }
        }
    }

    private void putKit() {
        ShulkerBoxReader box = this.findShulkerInPlayer(this::containsNeeded);
        if (box != null) {
            if (this.notInOperationRange(this.stockPos)) {
                this.closeScreen();
                this.gotoBtnPos(this.stockPos, "<存放备货>距离不够, 尝试移动", Steps.PUT_KIT);
            } else {
                this.openChest(this.stockPos.getPutPos(), container -> {
                    InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                    for (Map.Entry<ItemBo, Integer> entry : box.readItemQtyMap().entrySet()) {
                        this.subtractNeeded(entry.getKey(), entry.getValue());
                    }
                    this.setDelay();
                });
            }
        } else {
            ShulkerBoxReader emptyBox = this.findShulkerInPlayer(ShulkerBoxReader::isEmpty);
            if (emptyBox == null) {
                this.closeNext(Steps.NEXT);
            } else if (this.notInOperationRange(this.emptyPos)) {
                this.gotoBtnPos(this.emptyPos, "<存放空盒>距离不够, 尝试移动", Steps.PUT_KIT);
            } else {
                this.openChest(this.emptyPos.getPutPos(), container -> {
                    InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                    this.setDelay();
                });
            }
        }
    }

    private void takeKit() {
        int groups = this.remainingGroups();
        if (groups <= 0) {
            this.closeNext(Steps.NEXT);
        } else if (this.operationCounter >= this.operationCount.get()) {
            this.closeNext(Steps.PUT_ITEM);
        } else {
            if (groups < this.wholeBoxThreshold.get()) {
                BlockState state = this.mc.world.getBlockState(this.currentPos.getKitPos());
                if (!state.isAir()) {
                    this.closeNext(Steps.TAKE_ITEM);
                    return;
                }
                if (this.phaseCounter >= 2) {
                    this.closeNext(Steps.PLACE_KIT);
                    return;
                }
                if (this.phaseCounter == 0) {
                    this.phaseCounter = 1;
                }
            }
            if (this.notInOperationRange(this.currentPos)) {
                this.gotoBtnPos(this.currentPos, "<取货>距离不够, 尝试移动", Steps.TAKE_KIT);
            } else {
                this.openChest(this.currentPos.getTakePos(), container -> {
                    ShulkerBoxReader box = this.findShulkerInScreen(b -> this.currentItem.equals(b.getFirstItemBo()));
                    if (box == null) {
                        this.warning("缺少: %s", this.currentItem.getName());
                        this.skipped.add(this.currentItem);
                        this.delayCloseNext(Steps.NEXT);
                    } else {
                        InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                        Map.Entry<ItemBo, Integer> first = box.readItemQtyMap().entrySet().iterator().next();
                        if (this.phaseCounter == 0) {
                            this.taken.merge(this.currentItem, first.getValue(), Integer::sum);
                            ++this.operationCounter;
                        } else if (this.phaseCounter == 1) {
                            this.phaseCounter = 2;
                        }
                        this.setDelay();
                    }
                });
            }
        }
    }

    private void placeKit() {
        BlockState state = this.mc.world.getBlockState(this.currentPos.getKitPos());
        if (!state.isAir()) {
            if (state.getBlock() instanceof ShulkerBoxBlock) {
                this.step = Steps.TAKE_ITEM;
            } else {
                this.breakStep("<取货位置>被占用");
            }
        } else {
            ShulkerBoxReader box = this.findShulkerInPlayer(b -> b.readItemQtyMap().containsKey(this.currentItem));
            if (box == null) {
                this.step = Steps.TAKE_KIT;
            } else if (this.notInOperationRange(this.currentPos)) {
                this.gotoBtnPos(this.currentPos, "<放置盒子>距离不够, 尝试移动", Steps.PLACE_KIT);
            } else {
                this.info("放置盒子: " + this.currentItem.getName());
                HeInvUtils.withItemInHand(this.getCurPlayerSlot(), () -> HeBlockUtils.clickAdjacentBlock(this.currentPos.getKitPos()));
                this.delayNext(Steps.TAKE_ITEM);
            }
        }
    }

    private void takeItem() {
        int groups = this.remainingGroups();
        if (groups <= 0) {
            this.dispatch();
        } else if (this.operationCounter >= this.operationCount.get()) {
            this.closeNext(Steps.PUT_ITEM);
        } else {
            if (this.itemPositions.containsKey(this.currentItem)) {
                if (groups >= this.wholeBoxThreshold.get()) {
                    this.closeNext(Steps.TAKE_KIT);
                    return;
                }
                this.takeFromKit();
            } else {
                this.takeFromChest();
            }
        }
    }

    private void takeFromChest() {
        if (this.notInOperationRange(this.currentPos)) {
            this.gotoBtnPos(this.currentPos, "<取物品>距离不够, 尝试移动", Steps.TAKE_ITEM);
        } else {
            this.openChest(this.currentPos.getTakePos(), container -> {
                ItemStack stack = this.findScreenStack(this.currentItem::isSameItem);
                if (stack.isEmpty()) {
                    this.warning("缺少: %s", this.currentItem.getName());
                    this.skipped.add(this.currentItem);
                    this.dispatch();
                } else {
                    InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                    this.taken.merge(this.currentItem, stack.getCount(), Integer::sum);
                    ++this.operationCounter;
                    this.setDelay();
                }
            });
        }
    }

    private void takeFromKit() {
        BlockState state = this.mc.world.getBlockState(this.currentPos.getKitPos());
        if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            this.closeNext(Steps.PLACE_KIT);
        } else if (this.notInOperationRange(this.currentPos)) {
            this.gotoBtnPos(this.currentPos, "<取物品>距离不够, 尝试移动", Steps.TAKE_ITEM);
        } else {
            this.openChest(this.currentPos.getKitPos(), container -> {
                ItemStack stack = this.findScreenStack(this.currentItem::isSameItem);
                if (stack.isEmpty()) {
                    this.info("盒子已空");
                    this.breakPos = this.currentPos;
                    this.delayCloseNext(Steps.BREAK_KIT);
                } else {
                    InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                    this.taken.merge(this.currentItem, stack.getCount(), Integer::sum);
                    ++this.operationCounter;
                    this.setDelay();
                }
            });
        }
    }

    private void takeEmptyKit() {
        if (this.notInOperationRange(this.emptyPos)) {
            this.gotoBtnPos(this.emptyPos, "<拿空盒>距离不够, 尝试移动", Steps.TAKE_EMPTY_KIT);
        } else {
            this.openChest(this.emptyPos.getTakePos(), container -> {
                ItemStack stack = this.findScreenStack(s -> HeItemUtils.isShulkerBox(s.getItem()) && new ShulkerBoxReader(s).isEmpty());
                if (!stack.isEmpty()) {
                    InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                    this.delayCloseNext(Steps.PLACE_EMPTY_KIT);
                } else {
                    this.breakStep("<空盒箱>缺少盒子");
                }
            });
        }
    }

    private void placeEmptyKit() {
        BlockState state = this.mc.world.getBlockState(this.stockPos.getKitPos());
        if (state.getBlock() instanceof ShulkerBoxBlock) {
            this.step = Steps.PUT_ITEM;
        } else if (!state.isAir()) {
            this.breakStep("<位置>被占用: " + Names.get(this.stockPos.getItem().getItem()));
        } else {
            ItemStack stack = this.findPlayerStack(s -> HeItemUtils.isShulkerBox(s.getItem()) && new ShulkerBoxReader(s).isEmpty());
            if (stack.isEmpty()) {
                this.step = Steps.TAKE_EMPTY_KIT;
            } else if (this.notInOperationRange(this.stockPos)) {
                this.gotoTarget(this.stockPos.getBtnPos(), 0, Steps.PLACE_EMPTY_KIT);
            } else {
                this.info("放置空盒");
                HeInvUtils.withItemInHand(this.getCurPlayerSlot(), () -> HeBlockUtils.clickAdjacentBlock(this.stockPos.getKitPos()));
                this.delayNext(Steps.PUT_ITEM);
            }
        }
    }

    private void breakKit() {
        if (this.notInOperationRange(this.breakPos)) {
            this.gotoBtnPos(this.breakPos, "<挖盒子>距离不够, 尝试移动", Steps.BREAK_KIT);
        } else {
            Vec3d center = Vec3d.ofCenter(this.breakPos.getBtnPos()).add(0.0, -0.5, 0.0);
            BlockHitResult hitResult = new BlockHitResult(center, Direction.UP, this.breakPos.getBtnPos(), false);
            PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0);
            this.mc.getNetworkHandler().sendPacket(packet);
            this.step = Steps.NEXT;
            this.setDelay(40);
        }
    }

    private boolean containsNeeded(ShulkerBoxReader box) {
        return box.readItemQtyMap().keySet().stream().anyMatch(this.needed::containsKey);
    }

    private int remainingGroups() {
        int remaining = this.remainingCount();
        return remaining <= 0 ? 0 : (remaining - 1) / this.currentItem.getItem().getMaxCount() + 1;
    }

    private int remainingCount() {
        int need = this.needed.get(this.currentItem);
        int have = this.taken.getOrDefault(this.currentItem, 0);
        return need - have;
    }

    private void subtractNeeded(ItemBo itemBo, int count) {
        Integer need = this.needed.get(itemBo);
        if (need != null) {
            if (need <= count) {
                this.needed.remove(itemBo);
            } else {
                this.needed.put(itemBo, need - count);
            }
        }
    }

    private void clear() {
        this.itemPositions.clear();
        this.needed.clear();
        this.taken.clear();
        this.skipped.clear();
        this.stockPos = null;
        this.currentItem = null;
        this.currentPos = null;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        this.currentItem = null;
        this.currentPos = null;
        HeInvUtils.closeCurScreen();
    }
}
