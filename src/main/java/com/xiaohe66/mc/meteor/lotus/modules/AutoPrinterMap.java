/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  fi.dy.masa.litematica.data.DataManager
 *  fi.dy.masa.litematica.data.SchematicHolder
 *  fi.dy.masa.litematica.schematic.LitematicaSchematic
 *  fi.dy.masa.litematica.schematic.placement.SchematicPlacement
 *  fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager
 *  fi.dy.masa.litematica.world.SchematicWorldHandler
 *  fi.dy.masa.litematica.world.WorldSchematic
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.StringSetting$Builder
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.meteorclient.utils.player.SlotUtils
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  net.minecraft.util.Hand
 *  net.minecraft.entity.decoration.ItemFrameEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.entity.player.PlayerInventory
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.PlayerScreenHandler
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.item.FilledMapItem
 *  net.minecraft.world.World
 *  net.minecraft.item.map.MapState
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.Block
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.BlockState
 *  net.minecraft.screen.CartographyTableScreenHandler
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.screen.ingame.InventoryScreen
 *  org.apache.commons.io.FileUtils
 *  org.apache.commons.lang3.StringUtils
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;
import com.xiaohe66.mc.meteor.lotus.modules.ItemClearUp;
import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;
import com.xiaohe66.mc.meteor.lotus.util.NumberNameComparator;
import com.xiaohe66.mc.meteor.lotus.modules.Printer;
import com.xiaohe66.mc.meteor.lotus.modules.WalkModule;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class AutoPrinterMap extends WalkModule {
    public final Setting<Integer> printRange = sgGeneral.add(new IntSetting.Builder()
        .name("打印范围")
        .description("打印范围")
        .min(1)
        .sliderMax(3)
        .defaultValue(2)
        .build());
    public final Setting<Integer> extraSupplyAmount = sgGeneral.add(new IntSetting.Builder()
        .name("额外补货量")
        .description("补货时, 至少多补的数量")
        .min(0)
        .sliderMax(64)
        .defaultValue(32)
        .build());
    private final Setting<Boolean> autoNaming = sgGeneral.add(new BoolSetting.Builder()
        .name("自动命名")
        .description("自动将成品地图画按投影名称命名")
        .defaultValue(true)
        .build());
    private final Setting<Integer> pauseTicks = sgGeneral.add(new IntSetting.Builder()
        .name("暂停时间(tick)")
        .description("画完一张后的暂停时间")
        .min(0)
        .sliderMax(2000)
        .defaultValue(200)
        .build());
    private final Setting<String> schematicPath = sgGeneral.add(new StringSetting.Builder()
        .name("投影路径(相对)")
        .description("投影文件相对schematics文件夹的路径, 修改后会自动获取目录下的投影")
        .defaultValue("lotus/map")
        .onChanged(this::updateSchematicNames)
        .build());
    private final Setting<String> schematicName = sgGeneral.add(new StringSetting.Builder()
        .name("投影名称")
        .description("需要打印投影的文件名称, 不要带扩展名, 多个使用英文逗号分开")
        .build());
    private final Setting<Boolean> initSetting = sgGeneral.add(new BoolSetting.Builder()
        .name("初始化")
        .description("使用前需要先初始化")
        .defaultValue(false)
        .onChanged(this::initScan)
        .build());
    private static final SchematicPlacementManager placementManager = DataManager.getSchematicPlacementManager();
    private static final int MAP_SIZE = 128;
    private BlockPos mapOrigin;
    private BlockPos mapCenter;
    private BlockPos supplyChestPos;
    private BlockPos outputChestPos;
    private BlockPos cartographyTablePos;
    private BlockPos anvilPos;
    private String mapName;
    private final Map<Item, List<BlockPos>> itemChestPosMap;
    private final List<List<BlockPos>> printRows;
    private final Map<Item, Integer> rowNeedCount;
    private final Map<Item, Integer> restockList;
    private Item restockItem;
    private int restockAmount;
    private BlockPos restockPos;
    private final List<BlockPos> breakQueue;
    private BlockPos currentBreakPos;
    private boolean sortingInProgress;
    private WorldSchematic schematicWorld;
    private int currentRow;
    private int rowIndex;
    private BlockPos currentPrintPos;

    public AutoPrinterMap() {
        super("M制图师", "自动打印地图画(仅地毯), 需要搭配<Lotus-制图平台>使用, 投影原点必须是左上角。补给箱需提供空地图、玻璃板、铁砧和经验瓶。由<1n8>友情赞助开发。");
        this.itemChestPosMap = new HashMap<Item, List<BlockPos>>();
        this.printRows = new LinkedList<List<BlockPos>>();
        this.rowNeedCount = new HashMap<Item, Integer>();
        this.restockList = new HashMap<Item, Integer>();
        this.breakQueue = new ArrayList<BlockPos>();
        this.addStep(Steps.LOAD_SCHEMATIC, this::loadSchematic);
        this.addStep(Steps.CHECK_BACKPACK, this::checkBackpack);
        this.addStep(Steps.RESTOCK, this::restock);
        this.addStep(Steps.PREPARE, this::prepare);
        this.addStep(Steps.MOVEMENT, this::movement);
        this.addStep(Steps.BREAK, this::breakWrongBlocks);
        this.addStep(Steps.OPEN_PRINTER, this::openPrinter);
        this.addStep(Steps.SUPPLY, this::supply);
        this.addStep(Steps.DRAW, this::drawMap);
        this.addStep(Steps.LOCK, this::lockMap);
        this.addStep(Steps.USE, this::anvilNaming);
        this.addStep(Steps.PUT, this::putMap);
        this.addStep(Steps.SORT, this::sortInventory);
    }

    @Override
    protected boolean useQuickStopKeybind() {
        return true;
    }

    @Override
    protected boolean allowQuickStop() {
        return this.step != Steps.WALKING && this.step != Steps.MOVEMENT;
    }

    @Override
    public void onActivate() {
        if (this.printRows.isEmpty()) {
            this.warning("启动前没有初始化, 自动初始化", new Object[0]);
            this.initScan(true);
            if (this.printRows.isEmpty()) {
                this.warning("自动初始化失败", new Object[0]);
                this.toggle();
                return;
            }
        }
        super.onActivate();
        LotusUtils.enableFreeLook();
        this.warning("延迟启动...", new Object[0]);
        this.delayStart(Steps.LOAD_SCHEMATIC);
    }

    private void prepare() {
        this.resetState();
        this.currentRow = 0;
        this.rowIndex = 0;
        this.schematicWorld = SchematicWorldHandler.getSchematicWorld();
        this.preparePrinter();
        this.countRowNeeds();
    }

    private void movement() {
        List<BlockPos> toPlaceList;
        while (true) {
            List<BlockPos> rowPosList = this.printRows.get(this.currentRow);
            this.currentPrintPos = rowPosList.get(this.rowIndex);
            List<BlockPos> scanPosList = this.getScanPositions(this.currentPrintPos);
            toPlaceList = new ArrayList<BlockPos>(scanPosList.size());
            for (BlockPos pos : scanPosList) {
                BlockState schematicState = this.schematicWorld.getBlockState(pos);
                if (schematicState.isAir()) continue;
                BlockState worldState = this.mc.world.getBlockState(pos);
                Item item = schematicState.getBlock().asItem();
                if (item == worldState.getBlock().asItem()) continue;
                if (!worldState.isAir() && worldState.getBlock() != Blocks.WATER) {
                    this.breakQueue.add(pos);
                    continue;
                }
                toPlaceList.add(pos);
            }
            if (!this.breakQueue.isEmpty()) {
                this.step = Steps.BREAK;
                return;
            }
            if (!toPlaceList.isEmpty()) break;
            ++this.rowIndex;
            if (this.rowIndex >= rowPosList.size()) {
                this.countRowNeeds();
                return;
            }
        }
        Box playerBox = this.mc.player.getBoundingBox();
        if (!playerBox.intersects(new Box(this.currentPrintPos))) {
            this.gotoTarget(this.currentPrintPos, 0, Steps.MOVEMENT);
            return;
        }
        boolean blocked = true;
        BlockPos moveTarget = null;
        Iterator<BlockPos> iterator = toPlaceList.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState schematicState = this.schematicWorld.getBlockState(pos);
            Item item = schematicState.getBlock().asItem();
            ItemStack foundStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == item);
            if (foundStack.isEmpty()) {
                this.info("缺少物品, 重新检查物品量", new Object[0]);
                this.countRowNeeds();
                return;
            }
            if (playerBox.intersects(new Box(pos))) continue;
            blocked = false;
            if (this.currentRow % 2 == 0) {
                if (pos.getX() >= this.currentPrintPos.getX() - this.printRange.get()) continue;
                moveTarget = pos;
                continue;
            }
            if (pos.getX() <= this.currentPrintPos.getX() + this.printRange.get()) continue;
            moveTarget = pos;
        }
        if (blocked) {
            int[] offsets;
            this.info("可能挡住脚下了, 尝试移动", new Object[0]);
            if (this.currentRow % 2 == 0) {
                int[] offsets2 = new int[4];
                offsets2[0] = -1;
                offsets2[1] = -2;
                offsets2[2] = 1;
                offsets = offsets2;
                offsets2[3] = 2;
            } else {
                int[] offsets3 = new int[3];
                offsets3[0] = 1;
                offsets3[1] = 1;
                offsets = offsets3;
                offsets3[2] = -2;
            }
            for (int offset : offsets) {
                BlockPos checkPos = this.currentPrintPos.add(offset, 0, 0);
                BlockState schematicState = this.schematicWorld.getBlockState(checkPos);
                if (!schematicState.isAir() && !HeItemUtils.isCarpet(this.mc.world.getBlockState(checkPos).getBlock().asItem())) continue;
                this.gotoTarget(checkPos, 0, Steps.MOVEMENT);
                return;
            }
            this.warning("找不到可以移动的位置...", new Object[0]);
        } else if (moveTarget != null) {
            this.info("检测到前面出现错误, 回去修复", new Object[0]);
            --this.rowIndex;
        }
    }

    private void putMap() {
        ItemStack filledMapStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.FILLED_MAP);
        if (filledMapStack.isEmpty()) {
            this.closeScreen();
            this.removeCurrentSchematic();
            this.loadSchematic();
            this.info("暂停一会, 等待水收回...", new Object[0]);
            this.setDelay(this.pauseTicks.get());
            return;
        }
        MapState mapState = FilledMapItem.getMapState(filledMapStack, (World)this.mc.world);
        if (!mapState.locked) {
            this.step = Steps.LOCK;
            return;
        }
        if (this.autoNaming.get()) {
            Text customName = filledMapStack.getName();
            if (customName == null || !this.mapName.equals(customName.getString())) {
                this.step = Steps.USE;
                return;
            }
        }
        BlockPos targetPos = this.atPlayerY(this.outputChestPos);
        if (this.isTooFar(targetPos, 1.0)) {
            this.info("前往放入成品", new Object[0]);
            this.gotoTarget(targetPos, 0, Steps.PUT);
            return;
        }
        this.openChest(this.outputChestPos, screenHandler -> {
            this.info("放入成品地图", new Object[0]);
            InvUtils.shiftClick().slot(this.getCurPlayerSlot());
            this.setDelay();
        });
    }

    private void lockMap() {
        if (this.mc.world.getBlockState(this.cartographyTablePos).getBlock() != Blocks.CARTOGRAPHY_TABLE) {
            this.warning("制图台位置错误, 稍后重启", new Object[0]);
            this.restartFrom(Steps.LOCK);
            return;
        }
        BlockPos targetPos = this.atPlayerY(this.cartographyTablePos);
        if (this.isTooFar(targetPos, 1.0)) {
            this.info("前往制图台", new Object[0]);
            this.gotoTarget(targetPos, 0, Steps.LOCK);
            return;
        }
        if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            if (targetPos != this.cartographyTablePos) {
                HeBlockUtils.open(this.cartographyTablePos, Direction.UP);
            } else {
                HeBlockUtils.open(this.cartographyTablePos);
            }
            this.setDelay();
        } else {
            ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
            if (screenHandler instanceof CartographyTableScreenHandler) {
                CartographyTableScreenHandler cartographyHandler = (CartographyTableScreenHandler)screenHandler;
                if (cartographyHandler.getSlot(0).getStack().isEmpty()) {
                    ItemStack filledMapStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.FILLED_MAP);
                    if (filledMapStack.isEmpty()) {
                        this.warning("身上没有已绘制的地图, 重新绘制", new Object[0]);
                        this.closeNext(Steps.SUPPLY);
                        return;
                    }
                    MapState mapState = FilledMapItem.getMapState(filledMapStack, (World)this.mc.world);
                    if (mapState.locked) {
                        this.info("已锁定, 不需要锁定", new Object[0]);
                        this.closeNext(Steps.USE);
                        return;
                    }
                    InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                } else if (cartographyHandler.getSlot(1).getStack().isEmpty()) {
                    ItemStack glassPaneStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.GLASS_PANE);
                    if (glassPaneStack.isEmpty()) {
                        this.warning("身上没有玻璃板, 去补给", new Object[0]);
                        this.closeNext(Steps.SUPPLY);
                        return;
                    }
                    InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                } else if (!cartographyHandler.getSlot(2).getStack().isEmpty()) {
                    this.info("锁定地图", new Object[0]);
                    InvUtils.shiftClick().slotId(2);
                    this.closeNext(this.autoNaming.get() ? Steps.USE : Steps.PUT);
                }
                this.setDelay();
            }
        }
    }

    private void anvilNaming() {
        if (!this.autoNaming.get()) {
            this.putMap();
            return;
        }
        ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
        if (screenHandler instanceof AnvilScreenHandler) {
            AnvilScreenHandler anvilHandler = (AnvilScreenHandler)screenHandler;
            if (anvilHandler.getSlot(0).getStack().isEmpty()) {
                ItemStack mapStack = this.nextPlayerStack(this::needsNaming);
                if (mapStack.isEmpty()) {
                    this.closeNext(Steps.PUT);
                    return;
                }
                InvUtils.shiftClick().slot(this.getCurPlayerSlot());
            } else {
                ItemStack resultStack = anvilHandler.getSlot(2).getStack();
                Text resultName = resultStack.getName();
                if (resultName != null && this.mapName.equals(resultName.getString())) {
                    this.info("命名完成: %s", new Object[]{this.mapName});
                    InvUtils.shiftClick().slotId(2);
                    this.closeNext(Steps.PUT);
                    return;
                }
                this.info("命名地图: %s", new Object[]{this.mapName});
                anvilHandler.setNewItemName(this.mapName);
                this.mc.getNetworkHandler().sendPacket(new RenameItemC2SPacket(this.mapName));
            }
            this.setDelay();
        } else if (this.mc.player.experienceLevel < 1) {
            ItemStack xpStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.EXPERIENCE_BOTTLE);
            if (xpStack.isEmpty()) {
                this.info("缺少XP, 前往补给", new Object[0]);
                this.step = Steps.SUPPLY;
            } else {
                HeInvUtils.swapTo(this.getCurPlayerSlot());
                Rotations.rotate(this.mc.player.getYaw(), 90.0, () -> this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND));
                this.setDelay(20);
            }
        } else {
            BlockPos targetPos = this.atPlayerY(this.anvilPos);
            if (this.isTooFar(targetPos, 1.0)) {
                this.info("前往铁砧", new Object[0]);
                this.gotoTarget(targetPos, 0, Steps.USE);
            } else if (!(this.mc.world.getBlockState(this.anvilPos).getBlock() instanceof AnvilBlock)) {
                ItemStack anvilStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.ANVIL);
                if (anvilStack.isEmpty()) {
                    this.info("铁砧损坏且背包没有铁砧, 前往补给", new Object[0]);
                    this.step = Steps.SUPPLY;
                } else {
                    this.info("补放铁砧", new Object[0]);
                    HeInvUtils.swapTo(this.getCurPlayerSlot());
                    HeBlockUtils.clickAdjacentBlock(this.anvilPos);
                    this.setDelay();
                }
            } else if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
                if (targetPos != this.anvilPos) {
                    HeBlockUtils.open(this.anvilPos, Direction.UP);
                } else {
                    HeBlockUtils.open(this.anvilPos);
                }
                this.setDelay();
            }
        }
    }

    private boolean needsNaming(ItemStack stack) {
        if (stack.getItem() != Items.FILLED_MAP) {
            return false;
        }
        Text customName = stack.getName();
        return customName == null || !this.mapName.equals(customName.getString());
    }

    private void drawMap() {
        ItemStack filledMapStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.FILLED_MAP);
        if (!filledMapStack.isEmpty()) {
            this.info("地图绘制完成", new Object[0]);
            this.step = Steps.LOCK;
            return;
        }
        if (!(this.mc.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            this.closeNext(Steps.DRAW);
            return;
        }
        ItemStack emptyMapStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.MAP);
        if (emptyMapStack.isEmpty()) {
            this.step = Steps.SUPPLY;
            return;
        }
        if (this.isTooFar(this.mapCenter, 2.0)) {
            this.info("前往地图中心", new Object[0]);
            this.gotoTarget(this.mapCenter, 1, Steps.DRAW);
            return;
        }
        int slot = this.getCurPlayerSlot();
        if (slot != this.mc.player.getInventory().getSelectedSlot()) {
            HeInvUtils.swapToSelectedSlot(slot);
            this.setDelay();
            return;
        }
        this.warning("绘制地图, 并等待加载...", new Object[0]);
        this.mc.interactionManager.interactItem((PlayerEntity)this.mc.player, Hand.MAIN_HAND);
        this.setDelay(this.pauseTicks.get());
    }

    private void supply() {
        PlayerInventory playerInventory = this.mc.player.getInventory();
        boolean hasEmptyMaps = false;
        boolean hasGlassPanes = false;
        boolean hasAnvils = false;
        boolean hasXpBottles = false;
        for (int i = 0; i < 36; ++i) {
            Item item = playerInventory.getStack(i).getItem();
            if (item == Items.GLASS_PANE) {
                hasGlassPanes = true;
                continue;
            }
            if (item == Items.MAP) {
                hasEmptyMaps = true;
                continue;
            }
            if (item == Items.ANVIL) {
                hasAnvils = true;
                continue;
            }
            if (item == Items.EXPERIENCE_BOTTLE) {
                hasXpBottles = true;
            }
        }
        boolean needAnvil = !hasAnvils && !(this.mc.world.getBlockState(this.anvilPos).getBlock() instanceof AnvilBlock);
        boolean needXp = !hasXpBottles && this.mc.player.experienceLevel < 1;
        if (hasEmptyMaps && hasGlassPanes && !needAnvil && !needXp) {
            this.closeNext(Steps.DRAW);
            return;
        }
        BlockPos supplyTargetPos = this.atPlayerY(this.supplyChestPos);
        if (this.isTooFar(supplyTargetPos, 1.0)) {
            this.info("前往补给", new Object[0]);
            this.gotoTarget(supplyTargetPos, 0, Steps.SUPPLY);
            return;
        }
        boolean needsEmptyMaps = !hasEmptyMaps;
        boolean needsGlassPanes = !hasGlassPanes;
        Direction openDirection = supplyTargetPos != this.supplyChestPos ? Direction.UP : null;
        this.openKit(this.supplyChestPos, openDirection, screenHandler -> {
            ItemStack supplyStack = this.nextScreenStack(itemStack -> {
                Item item = itemStack.getItem();
                return needsEmptyMaps && item == Items.MAP || needsGlassPanes && item == Items.GLASS_PANE || needAnvil && item == Items.ANVIL || needXp && item == Items.EXPERIENCE_BOTTLE;
            });
            if (supplyStack.isEmpty()) {
                this.warning("无法补给, 稍后重启", new Object[0]);
                this.restartFrom(Steps.SUPPLY);
                return;
            }
            int emptySlot = this.getFirstEmptySlot();
            if (emptySlot != -1) {
                this.info("补给: %s", new Object[]{Names.get(supplyStack)});
                if (supplyStack.getItem() == Items.EXPERIENCE_BOTTLE) {
                    InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                } else {
                    int slotId = SlotUtils.indexToId((int)emptySlot);
                    HeInvUtils.moveOneFromSlot(this.getCurScreenSlot(), slotId);
                }
                this.setDelay();
            } else {
                ItemStack carpetStack = this.nextPlayerStack(itemStack -> HeItemUtils.isCarpet(itemStack.getItem()));
                if (!carpetStack.isEmpty()) {
                    this.info("背包没有空位, 丢弃地毯: %s", new Object[]{Names.get(carpetStack)});
                    InvUtils.drop().slot(this.getCurPlayerSlot());
                    this.setDelay();
                    return;
                }
                this.warning("身上没有空位, 且没有可丢弃的地毯, 稍后重启", new Object[0]);
                this.restartFrom(Steps.SUPPLY);
            }
        });
    }

    private void breakWrongBlocks() {
        while (true) {
            if (this.currentBreakPos == null) {
                if (this.breakQueue.isEmpty()) {
                    this.step = Steps.OPEN_PRINTER;
                    return;
                }
                this.currentBreakPos = (BlockPos)this.breakQueue.removeFirst();
            }
            BlockState schematicState = this.schematicWorld.getBlockState(this.currentBreakPos);
            BlockState worldState = this.mc.world.getBlockState(this.currentBreakPos);
            Item item = schematicState.getBlock().asItem();
            if (item != worldState.getBlock().asItem() && !worldState.isAir()) break;
            this.currentBreakPos = null;
        }
        if (this.isEyeTooFar(this.currentBreakPos, 4.1)) {
            BlockPos walkPos = new BlockPos(this.currentBreakPos.getX(), this.currentPrintPos.getY(), this.currentPrintPos.getZ());
            this.gotoTarget(walkPos, 1, Steps.BREAK);
            return;
        }
        LotusUtils.stopPrinter();
        HeRotationUtils.rotate(this.currentBreakPos, () -> BlockUtils.breakBlock(this.currentBreakPos, true));
    }

    private void restock() {
        ItemClearUp itemClearUp;
        if (this.restockItem == null) {
            if (this.restockList.isEmpty()) {
                this.checkBackpack();
                return;
            }
            List<BlockPos> rowPosList = this.printRows.get(this.currentRow);
            int index = Math.clamp((long)this.rowIndex, (int)0, (int)(rowPosList.size() - 1));
            Vec3d targetCenter = rowPosList.get(index).toCenterPos();
            Vec3d playerPos = this.mc.player.getPos();
            double nearestDistance = Double.MAX_VALUE;
            BlockPos nearestPos = null;
            Item nearestItem = null;
            for (Item item : this.restockList.keySet()) {
                List<BlockPos> chestPosList = this.itemChestPosMap.get(item);
                for (BlockPos chestPos : chestPosList) {
                    double playerDistance;
                    Vec3d chestCenter = chestPos.toCenterPos();
                    double distance = targetCenter.distanceTo(chestCenter);
                    double minDistance = Math.min(distance, playerDistance = playerPos.distanceTo(chestCenter));
                    if (!(minDistance < nearestDistance)) continue;
                    nearestDistance = minDistance;
                    nearestPos = chestPos;
                    nearestItem = item;
                }
            }
            if (nearestPos == null) {
                String missingItems = this.restockList.keySet().stream().map(Names::get).collect(Collectors.joining(","));
                this.info("无法补货, 缺少补给点: " + missingItems);
                return;
            }
            this.restockPos = nearestPos;
            this.restockItem = nearestItem;
            this.restockAmount = this.restockList.remove(nearestItem) + this.extraSupplyAmount.get();
        }
        if (this.isTooFar(this.restockPos, 3.0)) {
            this.info("前往补货: %s = %s", new Object[]{Names.get(this.restockItem), this.restockAmount});
            this.gotoTarget(this.restockPos, 2, Steps.RESTOCK);
            return;
        }
        itemClearUp = (ItemClearUp)Modules.get().get(ItemClearUp.class);
        if (itemClearUp.isSortingInProgress() || ((BaseModule)itemClearUp).getDelayTimer() > 0) {
            return;
        }
        this.openChest(this.restockPos, (ScreenHandler screenHandler) -> {
            if (this.isInvFull()) {
                ItemStack foundStack;
                this.info("背包满了, 尝试清理", new Object[0]);
                Map<Item, Integer> inventoryCounts = this.countInventoryCarpets();
                Item dropItem = this.findItemToDrop(inventoryCounts);
                if (dropItem != Items.AIR && !(foundStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == dropItem)).isEmpty()) {
                    this.info("丢弃无用地毯: %s", new Object[]{Names.get(dropItem)});
                    InvUtils.drop().slot(this.getCurPlayerSlot());
                    this.setDelay();
                    return;
                }
                this.warning("无法清理背包, 尝试整理", new Object[0]);
                this.delayCloseNext(Steps.SORT);
                return;
            }
            ItemStack supplyStack = this.nextScreenStack(itemStack -> itemStack.getItem() == this.restockItem);
            if (supplyStack.isEmpty()) {
                this.warning("无法补货: %s, 稍后重启", new Object[]{Names.get(this.restockItem)});
                this.restartFrom(Steps.CHECK_BACKPACK);
                return;
            }
            int count = supplyStack.getCount();
            InvUtils.shiftClick().slotId(this.getCurScreenSlot());
            if (this.restockAmount <= count) {
                this.restockItem = null;
                this.closeNext(Steps.RESTOCK);
            } else {
                this.restockAmount -= count;
            }
            this.setDelay();
        });
    }

    private Item findItemToDrop(Map<Item, Integer> inventoryCounts) {
        for (Map.Entry<Item, Integer> entry : inventoryCounts.entrySet()) {
            Item item = entry.getKey();
            if (this.rowNeedCount.containsKey(item)) {
                if (entry.getValue() <= this.rowNeedCount.get(item) + 64) continue;
                return item;
            }
            return item;
        }
        return Items.AIR;
    }

    private void checkBackpack() {
        Map<Item, Integer> inventoryCounts = this.countInventoryCarpets();
        this.restockList.clear();
        for (Map.Entry<Item, Integer> entry : this.rowNeedCount.entrySet()) {
            Item item = entry.getKey();
            int needCount = entry.getValue();
            Integer haveCount = inventoryCounts.get(item);
            if (haveCount == null) {
                this.restockList.put(item, needCount);
                continue;
            }
            if (haveCount >= needCount) continue;
            this.restockList.put(item, needCount - haveCount);
        }
        if (this.restockList.isEmpty()) {
            this.step = Steps.MOVEMENT;
        } else {
            this.restockItem = null;
            this.step = Steps.RESTOCK;
        }
    }

    private void countRowNeeds() {
        int range = this.printRange.get();
        int rowWidth = range * 2 + 1;
        int currentRowAtStart = this.currentRow;
        do {
            if (this.currentRow >= this.printRows.size()) {
                this.finishPrinting();
                return;
            }
            this.rowNeedCount.clear();
            int startIndex = rowWidth * this.currentRow;
            int endIndex = startIndex + rowWidth - 1;
            endIndex = Math.min(endIndex, 127);
            for (int x = startIndex = Math.clamp((long)startIndex, (int)0, (int)(endIndex - range)); x <= endIndex; ++x) {
                for (int z = 0; z < 128; ++z) {
                    BlockPos pos = this.mapOrigin.add(z, 0, x);
                    BlockState schematicState = this.schematicWorld.getBlockState(pos);
                    if (schematicState.isAir()) continue;
                    BlockState worldState = this.mc.world.getBlockState(pos);
                    Item item = schematicState.getBlock().asItem();
                    if (item == worldState.getBlock().asItem()) continue;
                    if (worldState.getBlock() == Blocks.VOID_AIR) {
                        this.warning("区块未加载, 稍后重启", new Object[0]);
                        this.restartFrom(Steps.MOVEMENT);
                        return;
                    }
                    this.rowNeedCount.merge(item, 1, Integer::sum);
                }
            }
            if (!this.rowNeedCount.isEmpty()) {
                this.info("开始打印第[%s]行, 共[%s]行", new Object[]{this.currentRow + 1, this.printRows.size()});
                this.rowIndex = 0;
                this.checkBackpack();
                return;
            }
            ++this.currentRow;
        } while (this.currentRow < this.printRows.size());
        if (currentRowAtStart == 0) {
            this.finishPrinting();
        } else {
            this.currentRow = 0;
            this.rowIndex = 0;
        }
    }

    private Map<Item, Integer> countInventoryCarpets() {
        PlayerInventory playerInventory = this.mc.player.getInventory();
        HashMap<Item, Integer> carpetCountMap = new HashMap<Item, Integer>();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = playerInventory.getStack(i);
            Item item = stack.getItem();
            if (!HeItemUtils.isCarpet(item)) continue;
            carpetCountMap.merge(item, stack.getCount(), Integer::sum);
        }
        return carpetCountMap;
    }

    private void loadSchematic() {
        Object schematicFile;
        String schematicNames = this.schematicName.get();
        if (StringUtils.isBlank((CharSequence)schematicNames)) {
            this.warning("所有投影都打印完毕, 结束", new Object[0]);
            this.toggle();
            return;
        }
        String[] nameArray = schematicNames.split(",");
        SchematicPlacement selectedPlacement = placementManager.getSelectedSchematicPlacement();
        if (selectedPlacement != null && selectedPlacement.isEnabled()) {
            schematicFile = selectedPlacement.getSchematicFile();
            if (schematicFile != null) {
                for (String schematicName : nameArray) {
                    if (!schematicName.equals(selectedPlacement.getName())) continue;
                    this.info("继续打印", new Object[0]);
                    this.mapName = schematicName;
                    this.prepare();
                    return;
                }
            }
            selectedPlacement.setEnabled(false);
        }
        schematicFile = this.schematicPath.get() + File.separator + nameArray[0] + ".litematic";
        Path path = DataManager.getSchematicsBaseDirectory().resolve((String)schematicFile);
        LitematicaSchematic litematicaSchematic = SchematicHolder.getInstance().getOrLoad(path);
        if (litematicaSchematic == null) {
            this.warning("投影文件加载失败: %s", new Object[]{path});
            return;
        }
        SchematicPlacement newPlacement = SchematicPlacement.createFor((LitematicaSchematic)litematicaSchematic, this.mapOrigin, nameArray[0], true, true);
        placementManager.addSchematicPlacement(newPlacement, false);
        placementManager.setSelectedSchematicPlacement(newPlacement);
        this.mapName = nameArray[0];
        this.info("加载投影: %s", new Object[]{path.getFileName()});
        this.delayNext(Steps.PREPARE);
    }

    private void removeCurrentSchematic() {
        SchematicPlacement selectedPlacement = placementManager.getSelectedSchematicPlacement();
        if (selectedPlacement != null && selectedPlacement.isEnabled()) {
            this.warning("移除投影 : %s", new Object[]{selectedPlacement.getName()});
            selectedPlacement.setEnabled(false);
            String remainingNames = Stream.of(this.schematicName.get().split(",")).filter(name -> !name.equals(selectedPlacement.getName())).collect(Collectors.joining(","));
            this.schematicName.set(remainingNames);
            placementManager.removeSchematicPlacement(selectedPlacement);
        }
    }

    private void initScan(Boolean enable) {
        if (!Boolean.TRUE.equals(enable)) {
            return;
        }
        this.initSetting.set(false);
        if (this.mc.player == null || this.mc.world == null) {
            this.warning("玩家或世界未加载", new Object[0]);
            return;
        }
        this.resetState();
        BlockPos playerPos = this.mc.player.getBlockPos();
        int originX = Math.floorDiv(playerPos.getX() + 64, 128) * 128 - 64;
        int originZ = Math.floorDiv(playerPos.getZ() + 64, 128) * 128 - 64;
        this.mapOrigin = new BlockPos(originX, playerPos.getY(), originZ);
        int searchRange = 128;
        Box searchBox = new Box((double)(playerPos.getX() - searchRange), (double)(playerPos.getY() - 2), (double)(playerPos.getZ() - searchRange), (double)(playerPos.getX() + searchRange), (double)(playerPos.getY() + 4), (double)(playerPos.getZ() + searchRange));
        List<ItemFrameEntity> itemFrames = this.mc.world.getEntitiesByClass(ItemFrameEntity.class, searchBox, itemFrame -> !itemFrame.getHeldItemStack().isEmpty());
        this.itemChestPosMap.clear();
        for (ItemFrameEntity itemFrame : itemFrames) {
            BlockPos attachedPos = itemFrame.getAttachedBlockPos();
            if (attachedPos == null) continue;
            BlockPos chestPos = attachedPos.offset(itemFrame.getFacing().getOpposite());
            Block chestBlock = this.mc.world.getBlockState(chestPos).getBlock();
            if (chestBlock != Blocks.CHEST && chestBlock != Blocks.TRAPPED_CHEST) continue;
            ItemStack heldStack = itemFrame.getHeldItemStack();
            Item item = heldStack.getItem();
            if (item == Items.REDSTONE) {
                this.supplyChestPos = chestPos;
                continue;
            }
            if (item == Items.REDSTONE_BLOCK) {
                this.outputChestPos = chestPos;
                continue;
            }
            if (!HeItemUtils.isCarpet(item)) continue;
            List<BlockPos> chestPosList = this.itemChestPosMap.computeIfAbsent(item, key -> new ArrayList<BlockPos>());
            chestPosList.add(chestPos);
        }
        if (this.supplyChestPos == null) {
            this.warning("未识别到<补给>容器", new Object[0]);
            return;
        }
        if (this.outputChestPos == null) {
            this.warning("未识别到<输出>容器", new Object[0]);
            return;
        }
        this.mapCenter = this.mapOrigin.add(64, 0, 64);
        this.cartographyTablePos = this.findCartographyTable();
        if (this.cartographyTablePos == null) {
            this.warning("未识别到<制图台>", new Object[0]);
            return;
        }
        this.anvilPos = this.findAnvil();
        if (this.anvilPos == null) {
            this.warning("未识别到<铁砧>", new Object[0]);
            return;
        }
        if (this.itemChestPosMap.size() < HeItemUtils.CARPETS.size()) {
            ArrayList<Item> remainingCarpets = new ArrayList<Item>(HeItemUtils.CARPETS);
            for (Item item : this.itemChestPosMap.keySet()) {
                remainingCarpets.remove(item);
            }
            String missingItems = remainingCarpets.stream().map(Names::get).collect(Collectors.joining(","));
            this.warning("缺少地毯容器: %s", new Object[]{missingItems});
            return;
        }
        this.printRows.clear();
        int range = this.printRange.get();
        int step = 2;
        int rowStride = range * 2 + 1;
        int startX = range;
        int endX = 128 - range;
        int z = range;
        do {
            List<BlockPos> row = new ArrayList<BlockPos>();
            for (int x = startX; x <= endX; x += step) {
                row.add(this.mapOrigin.add(x, 0, z));
            }
            row.add(this.mapOrigin.add(127, 0, z));
            this.printRows.add(row);
            if ((z += rowStride) >= 128) {
                if (z < 128 + range) {
                    ArrayList<BlockPos> edgeRow = new ArrayList<BlockPos>();
                    for (int x = endX; x >= 0; x -= step) {
                        edgeRow.add(this.mapOrigin.add(x, 0, endX));
                    }
                    edgeRow.add(this.mapOrigin.add(0, 0, z));
                    this.printRows.add(edgeRow);
                }
                break;
            }
            List<BlockPos> returnRow = new ArrayList<BlockPos>();
            for (int x = endX; x >= 0; x -= step) {
                returnRow.add(this.mapOrigin.add(x, 0, z));
            }
            returnRow.add(this.mapOrigin.add(0, 0, z));
            this.printRows.add(returnRow);
        } while ((z += rowStride) < 128);
        if (z < 128 + range) {
            ArrayList<BlockPos> edgeRow = new ArrayList<BlockPos>();
            for (int x = startX; x < 128; x += step) {
                edgeRow.add(this.mapOrigin.add(x, 0, endX));
            }
            edgeRow.add(this.mapOrigin.add(127, 0, z));
            this.printRows.add(edgeRow);
        }
        int supplyPointCount = this.itemChestPosMap.values().stream().mapToInt(List::size).sum();
        this.warning("初始化成功, 共[%s]个补给点, 共需打印[%s]行", new Object[]{supplyPointCount, this.printRows.size() + 1});
    }

    private BlockPos findCartographyTable() {
        int searchRange = 8;
        for (int y = -2; y <= 1; ++y) {
            for (int x = -searchRange; x < searchRange; ++x) {
                for (int z = -searchRange; z < searchRange; ++z) {
                    BlockPos pos = this.mapCenter.add(x, y, z);
                    Block block = this.mc.world.getBlockState(pos).getBlock();
                    if (block != Blocks.CARTOGRAPHY_TABLE) continue;
                    return pos;
                }
            }
        }
        return null;
    }

    private BlockPos findAnvil() {
        int searchRange = 8;
        for (int y = -2; y <= 1; ++y) {
            for (int x = -searchRange; x < searchRange; ++x) {
                for (int z = -searchRange; z < searchRange; ++z) {
                    BlockPos pos = this.mapCenter.add(x, y, z);
                    Block block = this.mc.world.getBlockState(pos).getBlock();
                    if (!(block instanceof AnvilBlock)) continue;
                    return pos;
                }
            }
        }
        return null;
    }

    private boolean isBlockAreaComplete(BlockPos pos) {
        List<BlockPos> neededPositions = this.getNeededPositions(pos);
        return neededPositions.isEmpty();
    }

    private List<BlockPos> getScanPositions(BlockPos pos) {
        int range = this.printRange.get();
        int rowWidth = range * 2 + 1;
        ArrayList<BlockPos> scanPositions = new ArrayList<BlockPos>((range + rowWidth + 1) * rowWidth);
        if (this.currentRow % 2 == 0) {
            int startX;
            for (int x = startX = Math.max(-range - rowWidth, this.mapOrigin.getX() - pos.getX()); x <= 1; ++x) {
                for (int z = -range; z <= range; ++z) {
                    scanPositions.add(pos.add(x, 0, z));
                }
            }
        } else {
            int startX;
            for (int x = startX = Math.min(range + rowWidth, this.mapOrigin.getX() + 127 - pos.getX()); x >= -1; --x) {
                for (int z = -range; z <= range; ++z) {
                    scanPositions.add(pos.add(x, 0, z));
                }
            }
        }
        return scanPositions;
    }

    private List<BlockPos> getNeededPositions(BlockPos pos) {
        ArrayList<BlockPos> neededPositions = new ArrayList<BlockPos>();
        int range = this.printRange.get();
        if (this.currentRow % 2 == 0) {
            for (int z = -range; z <= range; ++z) {
                this.collectNeededPositions(pos, range, z, neededPositions);
            }
        } else {
            for (int z = range; z >= -range; --z) {
                this.collectNeededPositions(pos, range, z, neededPositions);
            }
        }
        return neededPositions;
    }

    private void collectNeededPositions(BlockPos pos, int range, int z, List<BlockPos> outList) {
        int[] offsets = new int[range * 2 + 1];
        int writeIndex = 1;
        int value = 1;
        while (writeIndex <= range) {
            offsets[value] = -writeIndex;
            offsets[++value] = writeIndex++;
            ++value;
        }
        for (int offset : offsets) {
            BlockPos checkPos = pos.add(z, 0, offset);
            BlockState schematicState = this.schematicWorld.getBlockState(checkPos);
            if (schematicState.isAir()) continue;
            BlockState worldState = this.mc.world.getBlockState(checkPos);
            Item item = schematicState.getBlock().asItem();
            Block worldBlock = worldState.getBlock();
            if (item == worldBlock.asItem() || !worldState.isAir() && worldBlock != Blocks.WATER) continue;
            outList.add(checkPos);
        }
    }

    private void sortInventory() {
        ItemClearUp itemClearUp = (ItemClearUp)Modules.get().get(ItemClearUp.class);
        if (!itemClearUp.isActive()) {
            this.warning("快捷物品操作模块未开启, 跳过排序", new Object[0]);
            itemClearUp.toggle();
            return;
        }
        if (!(this.mc.currentScreen instanceof InventoryScreen)) {
            this.mc.setScreen((Screen)new InventoryScreen((PlayerEntity)this.mc.player));
            this.setDelay();
            return;
        }
        if (!this.sortingInProgress) {
            itemClearUp.startSort();
            this.sortingInProgress = true;
            this.setDelay();
            return;
        }
        if (itemClearUp.isSortingInProgress()) {
            this.setDelay();
            return;
        }
        this.sortingInProgress = false;
        this.mc.setScreen(null);
        this.delayNext(Steps.RESTOCK);
    }

    private void openPrinter() {
        this.preparePrinter();
        this.delayNext(Steps.MOVEMENT);
    }

    private void preparePrinter() {
        this.setupPrinterSupplier();
        LotusUtils.startPrinter();
    }

    private void setupPrinterSupplier() {
        Printer printer = (Printer)Modules.get().get(Printer.class);
        printer.setBlockPosSupplier(() -> {
            if (this.currentPrintPos == null) {
                return Collections.emptyList();
            }
            if (this.mc.player.getPos().distanceTo(this.currentPrintPos.toCenterPos()) > 10.0) {
                return Collections.emptyList();
            }
            return this.getNeededPositions(this.currentPrintPos);
        });
    }

    private void finishPrinting() {
        this.info("地毯打印完毕, 开始绘制地图", new Object[0]);
        LotusUtils.stopPrinter();
        this.step = Steps.SUPPLY;
    }

    private void resetState() {
        this.rowNeedCount.clear();
        this.restockList.clear();
        this.breakQueue.clear();
        this.restockItem = null;
        this.restockPos = null;
        this.currentBreakPos = null;
        this.sortingInProgress = false;
        this.currentPrintPos = null;
    }

    private void restartFrom(Step nextStep) {
        HeInvUtils.closeCurScreen();
        this.stop();
        this.delayStart(nextStep);
    }

    private void updateSchematicNames(String pathValue) {
        Collection<File> files = this.listLitematicFiles();
        String names = files.stream().map(File::getName).map(name -> {
            int dotIndex = name.indexOf(46);
            return dotIndex > 0 ? name.substring(0, dotIndex) : name;
        }).sorted(NumberNameComparator.INSTANCE).collect(Collectors.joining(","));
        if (!names.isEmpty()) {
            this.info("投影文件已更新, 请重新打开界面 : %s", new Object[]{names});
            this.schematicName.set(names);
        }
    }

    private Collection<File> listLitematicFiles() {
        String pathString = this.schematicPath.get();
        Path path = DataManager.getSchematicsBaseDirectory().resolve(pathString);
        File file = path.toFile();
        return file.exists() && file.isDirectory() ? FileUtils.listFiles(file, new String[]{".litematic"}, false) : Collections.emptyList();
    }

    private BlockPos atPlayerY(BlockPos pos) {
        int playerY = this.mc.player.getBlockY();
        return pos.getY() < playerY ? new BlockPos(pos.getX(), playerY, pos.getZ()) : pos;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        LotusUtils.stopPrinter();
        LotusUtils.disableFreeLook();
        this.resetState();
    }
}
