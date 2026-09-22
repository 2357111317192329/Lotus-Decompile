package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.clearup.ClearUpMapping;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.ConfigUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import com.xiaohe66.mc.meteor.lotus.util.WarehouseHelper;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.ItemListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoClearUp extends WarehouseModule {
    private final Setting<Integer> scanRange;
    private final Setting<Integer> operationCount;
    private final Setting<Integer> takeEmptyCount;
    private final Setting<Item> emptyBoxMarker;
    private final Setting<Item> sortBoxMarker;
    private final Setting<Item> unsortableMarker;
    private final Setting<String> configPath;
    private final Setting<Boolean> initSetting;
    private final Map<Item, ClearUpMapping> mappings = new LinkedHashMap<>();
    private final WarehouseHelper warehouseHelper = new WarehouseHelper();
    private StoragePos emptyPos;
    private StoragePos sortPos;
    private StoragePos unsortablePos;
    private StoragePos currentKit;
    private ItemBo currentItem;
    private Set<Item> currentRelatedItems;
    private StoragePos currentBreakKit;
    private boolean allDone;
    private int operationCounter;

    public AutoClearUp() {
        super("S自动分类", "有价值物品打包、散装物品装箱, 需要在lotus简易仓库才可使用。由资深猎人<jiafog199>友情赞助开发");
        this.scanRange = sgGeneral.add(new IntSetting.Builder().name("扫描范围").description("检测展示框和箱子的范围").defaultValue(50).min(1).sliderMax(100).build());
        this.operationCount = sgGeneral.add(new IntSetting.Builder().name("操作数").description("每次操作的物品数量").min(1).sliderMax(27).defaultValue(18).build());
        this.takeEmptyCount = sgGeneral.add(new IntSetting.Builder().name("拿取空盒数").description("拿取空盒时, 需要拿的空盒数量").min(1).sliderMax(9).defaultValue(2).build());
        this.emptyBoxMarker = sgGeneral.add(new ItemSetting.Builder().name("空盒标识").description("空盒箱子展示框上的物品").filter(HeItemUtils::isShulkerBox).defaultValue(Items.SHULKER_BOX).build());
        this.sortBoxMarker = sgGeneral.add(new ItemSetting.Builder().name("待整理标识").description("待整理箱子展示框上的物品").filter(HeItemUtils::isShulkerBox).defaultValue(Items.WHITE_SHULKER_BOX).build());
        this.unsortableMarker = sgGeneral.add(new ItemSetting.Builder().name("无法整理标识").description("无法整理箱子展示框上的物品").filter(HeItemUtils::isShulkerBox).defaultValue(Items.BLUE_SHULKER_BOX).build());
        this.configPath = sgGeneral.add(new StringSetting.Builder().name("散装物品配置").description("散装物品分类配置文件路径(相对Lotus文件夹, 也可填绝对路径), 文件格式: {\"目标物品id\":[\"关联物品id\", ...]}, 修改后需重新初始化").defaultValue("warehouse/mappings.json").build());
        this.initSetting = sgGeneral.add(new BoolSetting.Builder().name("初始化").description("使用前需要先初始化，保存所有箱子位置").defaultValue(false).onChanged(this::doInit).build());
        this.operationCounter = 0;
        this.addStep(Steps.NEXT, this::dispatch);
        this.addStep(Steps.PUT_ITEM, this::putSortItem);
        this.addStep(Steps.PUT_ITEM2, this::putMappedItem);
        this.addStep(Steps.TAKE_KIT, this::takeKit);
        this.addStep(Steps.TAKE_ITEM, this::takeItem);
        this.addStep(Steps.PLACE_EMPTY_KIT, () -> this.placeEmptyKit(this.currentKit));
        this.addStep(Steps.TAKE_EMPTY_KIT, () -> this.takeEmptyKit(this.emptyPos, this.takeEmptyCount.get()));
        this.addStep(Steps.PUT_EMPTY_KIT, () -> this.putKit(this.emptyPos));
        this.addStep(Steps.PUT_KIT, this::putMappedKit);
        this.addStep(Steps.PUT_MISC_KIT, this::putMiscKit);
        this.addStep(Steps.PLACE_KIT, () -> this.placeKit(this.sortPos, box -> true));
        this.addStep(Steps.BREAK_KIT, () -> this.breakKit(this.currentBreakKit));
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
        this.allDone = false;
        this.dispatch();
    }

    private void doInit(Boolean enabled) {
        if (Boolean.TRUE.equals(enabled)) {
            this.initSetting.set(false);
            if (this.mc.player != null && this.mc.world != null) {
                boolean success = this.initWarehouse();
                if (!success) {
                    this.clearWarehouse();
                }
            } else {
                this.warning("玩家或世界未加载");
            }
        }
    }

    private boolean initWarehouse() {
        this.clearWarehouse();
        this.warehouseHelper.init(this.scanRange.get(), this.getEnabledMappings());
        Map<ItemBo, StoragePos> scanResult = HePosUtils.scanFrames(this.scanRange.get(), 4);
        this.sortPos = scanResult.remove(ItemBo.of(this.sortBoxMarker.get()));
        this.emptyPos = scanResult.remove(ItemBo.of(this.emptyBoxMarker.get()));
        this.unsortablePos = scanResult.remove(ItemBo.of(this.unsortableMarker.get()));
        if (this.emptyPos == null) {
            this.warning("未检测到<空盒位置>，请在对应展示框放置" + Names.get(this.emptyBoxMarker.get()));
            return false;
        } else if (this.sortPos == null) {
            this.warning("未检测到<待整理位置>，请在对应展示框放置" + Names.get(this.sortBoxMarker.get()));
            return false;
        } else if (this.unsortablePos == null) {
            this.warning("未检测到<无法整理位置>，请在对应展示框放置" + Names.get(this.unsortableMarker.get()));
            return false;
        } else {
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
                targetPos = HePosUtils.getBlockPos(itemEntity.getPos());
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
                int slot = this.getCurPlayerSlot();
                if (firstSlot == slot) {
                    break;
                }
                if (firstSlot == -1) {
                    firstSlot = slot;
                }
                if (box.isEmpty()) {
                    ++emptyCount;
                    if (emptyCount >= this.takeEmptyCount.get()) {
                        this.step = Steps.PUT_EMPTY_KIT;
                        return;
                    }
                } else {
                    if (box.isSameItem()) {
                        ItemBo itemBo = box.getFirstItemBo();
                        if (this.warehouseHelper.isMapped(itemBo)) {
                            this.step = Steps.PUT_KIT;
                        } else {
                            this.step = Steps.PUT_MISC_KIT;
                        }
                        return;
                    }
                    Map<ItemBo, Integer> itemMap = box.readItemQtyMap();
                    boolean allUnmapped = true;
                    for (ItemBo itemBo : itemMap.keySet()) {
                        if (this.warehouseHelper.isMapped(itemBo)) {
                            allUnmapped = false;
                            break;
                        }
                    }
                    if (allUnmapped) {
                        this.step = Steps.PUT_MISC_KIT;
                        return;
                    }
                }
            }
            Set<Integer> selectedSlots = Set.of(this.mc.player.getInventory().getSelectedSlot());
            Map<ItemBo, Integer> countResult = HeInvUtils.countItems(selectedSlots, null);
            ItemBo nearestMapped = this.warehouseHelper.findNearestMapped(countResult.keySet());
            if (nearestMapped != null) {
                this.currentKit = this.warehouseHelper.getMappedPosition(nearestMapped);
                this.currentRelatedItems = this.warehouseHelper.getRelatedItems(nearestMapped.getItem());
                this.step = Steps.PUT_ITEM2;
            } else {
                ItemBo nearestUnmapped = this.warehouseHelper.findNearestUnmapped(countResult.keySet());
                if (nearestUnmapped != null) {
                    this.currentKit = this.warehouseHelper.getUnmappedPosition(nearestUnmapped);
                    this.currentItem = nearestUnmapped;
                    this.step = Steps.PUT_ITEM;
                } else if (this.allDone) {
                    this.warning("分类完成");
                    this.toggle();
                } else {
                    this.operationCounter = 0;
                    this.step = Steps.TAKE_ITEM;
                }
            }
        }
    }

    private void putMappedKit() {
        ShulkerBoxReader box = this.findShulkerInPlayer(boxReader -> {
            if (boxReader.isSameItem()) {
                ItemBo itemBo = boxReader.getFirstItemBo();
                return this.warehouseHelper.isMapped(itemBo);
            }
            return false;
        });
        if (box == null) {
            this.closeNext(Steps.NEXT);
        } else {
            ItemBo itemBo = box.getFirstItemBo();
            StoragePos pos = this.warehouseHelper.getUnmappedPosition(itemBo);
            if (pos == null) {
                pos = this.warehouseHelper.getMappedPosition(itemBo);
            }
            if (this.notInOperationRange(pos)) {
                this.gotoTarget(pos.getBtnPos(), 0, Steps.PUT_KIT);
            } else {
                this.openChest(pos.getPutPos(), container -> {
                    if (this.isContainerFull()) {
                        this.breakStep("箱子满了");
                    } else {
                        InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                        this.setDelay();
                    }
                });
            }
        }
    }

    private void putMiscKit() {
        ShulkerBoxReader box = this.findShulkerInPlayer(boxReader -> {
            if (boxReader.isEmpty()) {
                return false;
            }
            if (boxReader.isSameItem()) {
                ItemBo itemBo = boxReader.getFirstItemBo();
                return !this.warehouseHelper.isMapped(itemBo);
            }
            Map<ItemBo, Integer> itemMap = boxReader.readItemQtyMap();
            for (ItemBo itemBo : itemMap.keySet()) {
                if (this.warehouseHelper.isMapped(itemBo)) {
                    return false;
                }
            }
            return true;
        });
        if (box == null) {
            this.closeNext(Steps.NEXT);
        } else if (this.notInOperationRange(this.unsortablePos)) {
            this.gotoTarget(this.unsortablePos.getBtnPos(), 0, Steps.PUT_MISC_KIT);
        } else {
            this.openChest(this.unsortablePos.getPutPos(), container -> {
                if (this.isContainerFull()) {
                    this.breakStep("箱子满了");
                } else {
                    InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                    this.setDelay();
                }
            });
        }
    }

    private void takeKit() {
        if (this.operationCounter >= this.operationCount.get()) {
            this.closeNext(Steps.NEXT);
        } else if (this.notInOperationRange(this.sortPos)) {
            this.gotoTarget(this.sortPos.getBtnPos(), 0, Steps.TAKE_KIT);
        } else {
            this.openChest(this.sortPos.getTakePos(), container -> this.takeMappedFromContainer());
        }
    }

    private void takeItem() {
        if (this.operationCounter >= this.operationCount.get()) {
            this.closeNext(Steps.NEXT);
        } else {
            BlockState state = this.mc.world.getBlockState(this.sortPos.getKitPos());
            if (state.isAir()) {
                this.closeNext(Steps.PLACE_KIT);
            } else if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
                this.breakStep("<kit位置>被占用");
            } else if (this.notInOperationRange(this.sortPos)) {
                this.gotoTarget(this.sortPos.getBtnPos(), 0, Steps.TAKE_ITEM);
            } else {
                this.openChest(this.sortPos.getKitPos(), container -> this.takeMappedFromContainer());
            }
        }
    }

    private void takeMappedFromContainer() {
        ItemStack stack = this.findScreenStack(item -> this.warehouseHelper.isMapped(new ItemBo(item)));
        if (!stack.isEmpty()) {
            this.info("拿取物品: " + Names.get(stack.getItem()));
            InvUtils.shiftClick().slotId(this.getCurScreenSlot());
            this.setDelay();
            ++this.operationCounter;
        } else {
            ShulkerBoxReader box = this.findShulkerInScreen(this::containsMapped);
            if (box == null) {
                this.allDone = true;
                this.closeNext(Steps.NEXT);
            } else {
                this.info("拿取kit");
                InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                this.delayCloseNext(Steps.PLACE_KIT);
            }
        }
    }

    private void putSortItem() {
        BlockState state = this.mc.world.getBlockState(this.currentKit.getKitPos());
        if (state.isAir()) {
            this.closeNext(Steps.PLACE_EMPTY_KIT);
        } else if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            this.breakStep("<kit位置>被占用");
        } else if (this.notInOperationRange(this.currentKit)) {
            this.gotoTarget(this.currentKit.getBtnPos(), 0, Steps.PUT_ITEM);
        } else {
            ItemStack stack = this.findPlayerStack(item -> this.isNotSelectedSlot() && new ItemBo(item).equals(this.currentItem));
            if (stack.isEmpty()) {
                this.closeNext(Steps.NEXT);
            } else {
                this.openChest(this.currentKit.getKitPos(), container -> {
                    if (this.isContainerFull()) {
                        this.info("[%s]盒子满了", this.currentItem.getName());
                        this.currentBreakKit = this.currentKit;
                        this.delayCloseNext(Steps.BREAK_KIT);
                    } else {
                        InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                        this.setDelay();
                    }
                });
            }
        }
    }

    private void putMappedItem() {
        if (this.notInOperationRange(this.currentKit)) {
            this.gotoTarget(this.currentKit.getBtnPos(), 0, Steps.PUT_ITEM2);
        } else {
            ItemStack stack = this.findPlayerStack(item -> this.isNotSelectedSlot() && this.currentRelatedItems.contains(item.getItem()));
            if (stack.isEmpty()) {
                this.closeNext(Steps.NEXT);
            } else {
                this.openChest(this.currentKit.getPutPos(), container -> {
                    if (this.isContainerFull()) {
                        this.warning("箱子满了, 等待一会……");
                        this.setDelay(this.delay.get() * 10);
                    } else {
                        InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                        this.setDelay();
                    }
                });
            }
        }
    }

    private boolean isNotSelectedSlot() {
        return this.getCurPlayerSlot() != this.mc.player.getInventory().getSelectedSlot();
    }

    private void clearWarehouse() {
        this.warehouseHelper.clear();
        this.sortPos = null;
        this.emptyPos = null;
        this.unsortablePos = null;
        this.currentKit = null;
        this.currentItem = null;
        this.currentRelatedItems = null;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        this.currentKit = null;
        this.operationCounter = 0;
        HeInvUtils.closeCurScreen();
    }

    public Map<Item, ClearUpMapping> getEnabledMappings() {
        HashMap<Item, ClearUpMapping> result = new HashMap<>();
        for (Map.Entry<Item, ClearUpMapping> entry : this.mappings.entrySet()) {
            if (entry.getValue().isEnabled()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        NbtList list = new NbtList();
        for (ClearUpMapping mapping : this.mappings.values()) {
            list.add(mapping.toTag());
        }
        tag.put("clearUpMappings", list);
        return tag;
    }

    @Override
    public AutoClearUp fromTag(NbtCompound tag) {
        super.fromTag(tag);
        if (tag.contains("clearUpMappings")) {
            NbtList list = tag.getListOrEmpty("clearUpMappings");
            this.mappings.clear();
            for (NbtElement element : list) {
                if (element.getType() == 10) {
                    ClearUpMapping mapping = new ClearUpMapping().fromTag((NbtCompound) element);
                    this.mappings.put(mapping.getTargetItem(), mapping);
                }
            }
        }
        return this;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        this.buildWidget(theme, list);
        return list;
    }

    private void buildWidget(GuiTheme theme, WVerticalList list) {
        WSection section = (WSection) list.add(theme.section("散装物品分类 (" + this.mappings.size() + ")")).expandX().widget();
        WTable table = (WTable) section.add(theme.table()).expandX().widget();
        for (ClearUpMapping mapping : new ArrayList<>(this.mappings.values())) {
            Item item = mapping.getTargetItem();
            ItemStack stack = mapping.getTargetItem().getDefaultStack();
            table.add(theme.item(stack));
            WButton itemButton = (WButton) table.add(theme.button(Names.get(mapping.getTargetItem()))).expandX().widget();
            itemButton.action = () -> {
                ItemSetting setting = new ItemSetting.Builder()
                    .name("target-item")
                    .description("")
                    .defaultValue(mapping.getTargetItem())
                    .onChanged(newItem -> {
                        this.mappings.remove(item);
                        mapping.setTargetItem(newItem);
                        this.mappings.put(newItem, mapping);
                        list.clear();
                        this.buildWidget(theme, list);
                    })
                    .build();
                this.mc.setScreen(new ItemSettingScreen(theme, setting));
            };
            String relatedText = mapping.getRelatedItems().isEmpty() ? "空" : mapping.getRelatedItems().size() + " 个物品";
            WButton relatedButton = (WButton) table.add(theme.button(relatedText)).expandX().widget();
            relatedButton.action = () -> {
                ItemListSetting setting = new ItemListSetting.Builder()
                    .name("related-items")
                    .description("")
                    .defaultValue(mapping.getRelatedItems().toArray(new Item[0]))
                    .onChanged(items -> mapping.setRelatedItems(new HashSet<>(items)))
                    .build();
                ItemListSettingScreen screen = new ItemListSettingScreen(theme, setting);
                screen.onClosed(() -> {
                    list.clear();
                    this.buildWidget(theme, list);
                });
                this.mc.setScreen(screen);
            };
            WCheckbox checkbox = (WCheckbox) table.add(theme.checkbox(mapping.isEnabled())).widget();
            checkbox.action = () -> mapping.setEnabled(checkbox.checked);
            WMinus minus = (WMinus) table.add(theme.minus()).widget();
            minus.action = () -> {
                this.mappings.remove(mapping.getTargetItem());
                list.clear();
                this.buildWidget(theme, list);
            };
            table.row();
        }
        WTable buttons = (WTable) list.add(theme.table()).expandX().widget();
        WButton addButton = (WButton) buttons.add(theme.button("新增分类")).expandX().widget();
        addButton.action = () -> {
            this.mappings.put(Items.AIR, new ClearUpMapping());
            list.clear();
            this.buildWidget(theme, list);
        };
        WButton importButton = (WButton) buttons.add(theme.button("导入配置")).expandX().widget();
        importButton.action = () -> {
            this.warning("导入配置");
            this.mappings.clear();
            Map<Item, ClearUpMapping> loaded = ConfigUtils.loadMappings(this.configPath.get());
            this.mappings.putAll(loaded);
            this.warning("已加载散装物品分类: " + loaded.size() + " 组");
            list.clear();
            this.buildWidget(theme, list);
        };
        WButton exportButton = (WButton) buttons.add(theme.button("导出配置")).expandX().widget();
        exportButton.action = () -> {
            this.warning("导出配置");
            ConfigUtils.saveMappings(this.configPath.get(), this.mappings);
        };
        WButton openFolderButton = (WButton) buttons.add(theme.button("打开配置文件文件夹")).expandX().widget();
        openFolderButton.action = () -> {
            File folder = ConfigUtils.resolveConfigPath(this.configPath.get()).getParent().toFile();
            LotusUtils.openFolder(folder);
        };
        buttons.row();
    }

    private boolean containsMapped(ShulkerBoxReader box) {
        Map<ItemBo, Integer> map = box.readItemQtyMap();
        for (Map.Entry<ItemBo, Integer> entry : map.entrySet()) {
            ItemBo itemBo = entry.getKey();
            return this.warehouseHelper.isMapped(itemBo);
        }
        return false;
    }
}
