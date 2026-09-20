/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  fi.dy.masa.litematica.data.DataManager
 *  fi.dy.masa.litematica.world.SchematicWorldHandler
 *  fi.dy.masa.litematica.world.WorldSchematic
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.events.world.TickEvent$Post
 *  meteordevelopment.meteorclient.renderer.ShapeMode
 *  meteordevelopment.meteorclient.settings.BlockListSetting$Builder
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.ColorSetting$Builder
 *  meteordevelopment.meteorclient.settings.DoubleSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.meteorclient.utils.render.color.SettingColor
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.item.BlockItem
 *  net.minecraft.item.Item
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.Block
 *  net.minecraft.block.PlantBlock
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.SlabBlock
 *  net.minecraft.block.StairsBlock
 *  net.minecraft.block.BlockState
 *  net.minecraft.state.property.Properties
 *  net.minecraft.state.property.EnumProperty
 *  net.minecraft.state.property.Property
 *  net.minecraft.block.enums.SlabType
 *  net.minecraft.util.Pair
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;
import com.xiaohe66.mc.meteor.lotus.modules.printer.PlaceBlockHelper;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import com.xiaohe66.mc.meteor.lotus.modules.printer.PrinterMode;
import com.xiaohe66.mc.meteor.lotus.modules.printer.SortAlgorithm;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Printer extends BaseModule {
    private static final Logger log = LoggerFactory.getLogger(Printer.class);
    private final SettingGroup sgPlatform = settings.createGroup("平台打印");
    private final SettingGroup sgAntiMob = settings.createGroup("防刷怪");
    private final SettingGroup sgRendering = settings.createGroup("渲染");
    private final Setting<Double> printingRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("放置范围(格)")
        .description("放置范围(格)")
        .defaultValue(4.1)
        .min(1.0)
        .sliderMin(1.0)
        .max(6.0)
        .sliderMax(6.0)
        .build());
    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("打印数量(每tick)")
        .description("打印数量(每tick)")
        .defaultValue(1)
        .min(1)
        .sliderMax(16)
        .build());
    private final Setting<List<Block>> blacklist = sgGeneral.add(new BlockListSetting.Builder()
        .name("黑名单")
        .description("不允许放置的方块")
        .build());
    private final Setting<Integer> placeCooldown = sgGeneral.add(new IntSetting.Builder()
        .name("放置超时")
        .description("同一位置失败后冷却 tick")
        .defaultValue(20)
        .min(0)
        .sliderMax(100)
        .build());
    private final Setting<Integer> maxRayCheckPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("面校验上限(每tick)")
        .description("每tick进行射线面校验的最大次数。射线校验开销大, 限制后可防止扫描大量不可达位置时掉帧; 调低可减少卡顿, 但打印响应会变慢")
        .defaultValue(16)
        .min(1)
        .sliderMax(64)
        .build());
    private final Setting<SortAlgorithm> sortAlgorithm = sgGeneral.add(new EnumSetting.Builder<SortAlgorithm>()
        .name("优先打印")
        .description("打印的优先级")
        .defaultValue(SortAlgorithm.近处)
        .build());
    private final Setting<PrinterMode> printerMode = sgGeneral.add(new EnumSetting.Builder<PrinterMode>()
        .name("打印模式")
        .description("打印模式")
        .defaultValue(PrinterMode.投影打印)
        .build());
    private final Setting<List<Block>> platformBlocks = sgPlatform.add(new BlockListSetting.Builder()
        .name("平台方块")
        .description("搭建平台时使用的方块")
        .defaultValue(new Block[]{Blocks.STONE_BRICKS, Blocks.STONE_BRICK_SLAB, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE_SLAB})
        .visible(() -> this.printerMode.get() == PrinterMode.平台打印)
        .build());
    private final Setting<Boolean> topSlab = sgPlatform.add(new BoolSetting.Builder()
        .name("是否上半砖")
        .description("在打印半砖时, 打印上半砖还是下半砖")
        .defaultValue(true)
        .visible(() -> this.printerMode.get() == PrinterMode.平台打印)
        .build());
    private final Setting<List<Block>> antiMobBlocks = sgAntiMob.add(new BlockListSetting.Builder()
        .name("防刷怪方块")
        .description("在做防刷怪时使用的方块")
        .defaultValue(new Block[]{Blocks.ACTIVATOR_RAIL, Blocks.COBBLESTONE_SLAB, Blocks.DETECTOR_RAIL, Blocks.POWERED_RAIL, Blocks.RAIL, Blocks.STONE_BRICK_SLAB, Blocks.SMOOTH_STONE_SLAB})
        .visible(() -> this.printerMode.get() == PrinterMode.防刷怪)
        .build());
    private final Setting<Boolean> antiSmallMobs = sgAntiMob.add(new BoolSetting.Builder()
        .name("防矮小生物")
        .description("对仅有1格净空的位置(蜘蛛/洞穴蜘蛛/蠹虫/末影螨)也放置防刷怪方块")
        .defaultValue(false)
        .visible(() -> this.printerMode.get() == PrinterMode.防刷怪)
        .build());
    private final Setting<Integer> fadeTime = sgRendering.add(new IntSetting.Builder()
        .name("渲染淡出时间(tick)")
        .description("渲染淡出时间")
        .defaultValue(5)
        .sliderRange(1, 20)
        .build());
    private final Setting<SettingColor> colour = sgRendering.add(new ColorSetting.Builder()
        .name("渲染颜色")
        .description("渲染颜色")
        .defaultValue(new SettingColor(95, 190, 255))
        .build());
    private final List<Tuple<Integer, BlockPos>> renderPosList;
    private final Map<BlockPos, Integer> placeCooldownMap;
    private final List<PlaceBlockHelper> needPlaceBlockList;
    private int nextBlockIndex;
    private WorldSchematic schematicWorld;
    private Supplier<List<BlockPos>> blockPosSupplier;

    public Printer() {
        super("A打印机", "3c专用。投影打印、平台打印、防刷怪。使用前请使用Via跨版本到1.20.6以下", 0);
        this.renderPosList = new ArrayList<Tuple<Integer, BlockPos>>();
        this.placeCooldownMap = new HashMap<BlockPos, Integer>();
        this.needPlaceBlockList = new ArrayList<PlaceBlockHelper>();
        this.nextBlockIndex = 0;
    }

    public void onActivate() {
        if (this.printerMode.get() == PrinterMode.投影打印) {
            this.schematicWorld = SchematicWorldHandler.getSchematicWorld();
            if (this.schematicWorld == null) {
                this.warning("未加载投影", new Object[0]);
                this.toggle();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!this.isReady()) {
            this.toggle();
            return;
        }
        this.renderPosList.forEach(s -> s.setA(s.getA() - 1));
        this.renderPosList.removeIf(s -> s.getA() <= 0);
        this.placeCooldownMap.replaceAll((pos, cooldown) -> cooldown - 1);
        this.placeCooldownMap.entrySet().removeIf(entry -> entry.getValue() <= 0);
        if (!this.checkAndDecrement()) {
            return;
        }
        try {
            List<PlaceBlockHelper> placeBlocks;
            if (this.printerMode.get() == PrinterMode.投影打印) {
                placeBlocks = this.getSchematicPlaceBlocks();
            } else if (this.printerMode.get() == PrinterMode.平台打印) {
                if (this.platformBlocks.get().isEmpty()) {
                    this.warning("平台方块至少选择1个", new Object[0]);
                    this.toggle();
                    return;
                }
                placeBlocks = this.getPlatformPlaceBlocks();
            } else if (this.printerMode.get() == PrinterMode.防刷怪) {
                if (this.antiMobBlocks.get().isEmpty()) {
                    this.warning("防刷怪方块至少选择1个", new Object[0]);
                    this.toggle();
                    return;
                }
                placeBlocks = this.getAntiMobPlaceBlocks();
            } else {
                return;
            }
            this.selectBlocksToPlace(placeBlocks);
            this.placeBlocks();
            this.setDelay();
        }
        catch (Exception e) {
            log.error("onTickPost error", (Throwable)e);
            this.error("onTickPost error:" + e.getMessage(), new Object[0]);
        }
    }

    private void placeBlocks() {
        HeInvUtils.stopSprinting();
        try {
            for (PlaceBlockHelper helper : this.needPlaceBlockList) {
                BlockPos blockPos = helper.getBlockPos();
                BlockState targetState = null;
                Block block = null;
                int slot = -1;
                for (BlockState candidateState : helper.getCandidateStates()) {
                    block = candidateState.getBlock();
                    Item item = block.asItem();
                    slot = HeInvUtils.findItemSlot(item);
                    if (slot == -1) continue;
                    targetState = candidateState;
                    break;
                }
                if (targetState == null) {
                    return;
                }
                HeInvUtils.swapToSelectedSlot(slot);
                Collection properties = targetState.getProperties();
                if (block instanceof SlabBlock) {
                    HeBlockUtils.placeSlab(blockPos, targetState);
                } else if (block instanceof StairBlock) {
                    HeBlockUtils.placeStairs(blockPos, targetState);
                } else if (helper.requiresSneaking()) {
                    Printer.clickPlace(helper, blockPos);
                } else if (HeBlockUtils.isTorch(block)) {
                    HeBlockUtils.placeTorch(blockPos, targetState);
                } else if (properties.contains(BlockStateProperties.FACING)) {
                    Printer.placeDirectional(targetState, block, blockPos, (EnumProperty<Direction>)BlockStateProperties.FACING, helper.getClickDirection());
                } else if (properties.contains(BlockStateProperties.FACING_HOPPER)) {
                    HeBlockUtils.placeHopper(blockPos, targetState);
                } else if (properties.contains(BlockStateProperties.HORIZONTAL_FACING)) {
                    Printer.placeDirectional(targetState, block, blockPos, (EnumProperty<Direction>)BlockStateProperties.HORIZONTAL_FACING, helper.getClickDirection());
                } else {
                    Printer.clickPlace(helper, blockPos);
                }
                this.renderPosList.add(new Tuple(this.fadeTime.get(), blockPos));
                this.placeCooldownMap.put(blockPos, this.placeCooldown.get());
                HeInvUtils.swapToSelectedSlot(slot);
                HeInvUtils.sendCloseScreenPacket();
            }
        } finally {
            HeInvUtils.startSprinting();
        }
        this.needPlaceBlockList.clear();
    }

    private static void clickPlace(PlaceBlockHelper helper, BlockPos blockPos) {
        Direction clickDirection = helper.getClickDirection();
        if (clickDirection != null) {
            HeBlockUtils.clickAdjacentBlock(blockPos, clickDirection);
        } else {
            HeBlockUtils.clickAdjacentBlock(blockPos);
        }
    }

    private static void placeDirectional(BlockState blockState, Block block, BlockPos blockPos, EnumProperty<Direction> property, Direction clickDirection) {
        Direction facing = (Direction)blockState.getValue(property);
        Direction placeDirection = HeBlockUtils.isObserverOrHopper(block) ? facing : facing.getOpposite();
        if (clickDirection != null) {
            HeBlockUtils.placeBlock(blockPos, placeDirection, clickDirection);
        } else {
            HeBlockUtils.placeBlock(blockPos, placeDirection);
        }
    }

    private void selectBlocksToPlace(List<PlaceBlockHelper> blocks) {
        if (!this.needPlaceBlockList.isEmpty() || blocks.isEmpty()) {
            return;
        }
        int remainingChecks = this.maxRayCheckPerTick.get();
        int blockCount = blocks.size();
        int checkedCount = 0;
        for (int i = 0; i < blockCount && remainingChecks > 0; ++i) {
            PlaceBlockHelper helper = blocks.get((this.nextBlockIndex + i) % blockCount);
            ++checkedCount;
            BlockPos blockPos = helper.getBlockPos();
            if (blockPos.getY() > DataManager.getRenderLayerRange().getLayerMax()) continue;
            BlockState currentState = helper.getTargetState();
            if (this.printerMode.get() == PrinterMode.防刷怪) {
                boolean isPlant = currentState.getBlock() instanceof VegetationBlock;
                if (!currentState.isAir() && !isPlant || this.antiMobBlocks.get().contains(currentState.getBlock())) {
                    continue;
                }
            } else if (HeBlockUtils.isSolid(currentState)) continue;
            if (this.placeCooldown.get() != 0 && this.placeCooldownMap.containsKey(blockPos) || !currentState.canBeReplaced()) continue;
            for (BlockState candidateState : helper.getCandidateStates()) {
                Item item;
                if (candidateState == null || candidateState.isAir()) continue;
                Block block = candidateState.getBlock();
                if (this.blacklist.get().contains(block) || !((item = block.asItem()) instanceof BlockItem)) continue;
                BlockItem blockItem = (BlockItem)item;
                if (!HeInvUtils.hasItem(item) || HePosUtils.isEntityInside(blockPos, candidateState)) continue;
                --remainingChecks;
                Direction clickDirection = HeBlockUtils.getPlaceSide(blockPos, this.printingRange.get(), 0.0);
                if (clickDirection == null) break;
                helper.setClickDirection(clickDirection);
                this.needPlaceBlockList.add(helper);
                break;
            }
            if (this.needPlaceBlockList.size() >= this.blocksPerTick.get()) break;
        }
        this.nextBlockIndex = (this.nextBlockIndex + checkedCount) % blockCount;
    }

    private List<PlaceBlockHelper> getAntiMobPlaceBlocks() {
        List<Block> blocks = this.antiMobBlocks.get();
        List slabAdjustedStates = blocks.stream().map(Block::defaultBlockState).map(state -> state.getBlock() instanceof SlabBlock ? state.setValue((Property)SlabBlock.TYPE, (Comparable)SlabType.BOTTOM) : state).toList();
        int rangeBlocks = (int)Math.ceil(this.printingRange.get());
        double maxDistanceSq = this.printingRange.get() * this.printingRange.get();
        ArrayList<PlaceBlockHelper> placeBlocks = new ArrayList<PlaceBlockHelper>();
        BlockPos playerPos = this.mc.player.blockPosition();
        for (int x = -rangeBlocks; x <= rangeBlocks; ++x) {
            for (int z = -rangeBlocks; z <= rangeBlocks; ++z) {
                BlockState downState;
                int foundY = Integer.MAX_VALUE;
                for (int y = 1; y >= -rangeBlocks; --y) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState blockState = this.mc.level.getBlockState(checkPos);
                    if (!HeBlockUtils.isReplaceable(blockState, checkPos) || !HeBlockUtils.isSolid(downState = this.mc.level.getBlockState(checkPos.below())) || !BlockUtils.isValidSpawnBlock(downState) || !HeBlockUtils.isAboveClear(checkPos) && !this.antiSmallMobs.get()) continue;
                    foundY = y;
                    break;
                }
                if (foundY == Integer.MAX_VALUE) continue;
                BlockPos placePos = playerPos.offset(x, foundY, z);
                double distanceSq = this.mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf((Vec3i)placePos));
                if (distanceSq > maxDistanceSq) continue;
                PlaceBlockHelper helper = new PlaceBlockHelper(placePos, distanceSq);
                helper.setCandidateStates(slabAdjustedStates);
                helper.setTargetState(this.mc.level.getBlockState(placePos));
                helper.setRequiresSneaking(true);
                placeBlocks.add(helper);
            }
        }
        ((SortAlgorithm)this.sortAlgorithm.get()).sort(placeBlocks);
        return placeBlocks;
    }

    private List<PlaceBlockHelper> getPlatformPlaceBlocks() {
        ArrayList<PlaceBlockHelper> placeBlocks = new ArrayList<PlaceBlockHelper>();
        BlockPos centerPos = this.getPlatformCenterPos();
        Vec3 eyePos = this.mc.player.getEyePosition();
        double maxDistanceSq = this.printingRange.get() * this.printingRange.get();
        List<Block> blocks = this.platformBlocks.get();
        SlabType slabType = this.topSlab.get() != false ? SlabType.TOP : SlabType.BOTTOM;
        List slabAdjustedStates = blocks.stream().map(Block::defaultBlockState).map(state -> state.getBlock() instanceof SlabBlock ? state.setValue((Property)SlabBlock.TYPE, (Comparable)slabType) : state).toList();
        int rangeBlocks = this.printingRange.get().intValue();
        int maxX = centerPos.getX() + rangeBlocks;
        for (int x = centerPos.getX() - rangeBlocks; x <= maxX; ++x) {
            int maxZ = centerPos.getZ() + rangeBlocks;
            for (int z = centerPos.getZ() - rangeBlocks; z <= maxZ; ++z) {
                BlockPos targetPos = new BlockPos(x, centerPos.getY(), z);
                double distanceSq = eyePos.distanceToSqr(targetPos.getCenter());
                if (distanceSq > maxDistanceSq) continue;
                PlaceBlockHelper helper = new PlaceBlockHelper(targetPos, distanceSq);
                BlockState currentState = this.mc.level.getBlockState(targetPos);
                helper.setCandidateStates(slabAdjustedStates);
                helper.setTargetState(currentState);
                helper.setRequiresSneaking(true);
                placeBlocks.add(helper);
            }
        }
        this.sortAlgorithm.get().sort(placeBlocks);
        return placeBlocks;
    }

    private BlockPos getPlatformCenterPos() {
        Vec3 eyePos = this.mc.player.position();
        BlockPos basePos = this.mc.player.blockPosition();
        if (eyePos.y() - (double)basePos.getY() >= 0.5) {
            basePos = basePos.above();
        }
        for (int i = 0; i < 3; ++i) {
            basePos = basePos.below();
            Vec3 checkCenter = basePos.getCenter();
            boolean west = eyePos.x() < checkCenter.x();
            boolean north = eyePos.z() < checkCenter.z();
            ArrayList<BlockPos> checkPositions = new ArrayList<BlockPos>(4);
            checkPositions.add(basePos);
            if (west && north) {
                checkPositions.add(basePos.west());
                checkPositions.add(basePos.north().west());
                checkPositions.add(basePos.north());
            } else if (!west && north) {
                checkPositions.add(basePos.east());
                checkPositions.add(basePos.north().east());
                checkPositions.add(basePos.north());
            } else if (west) {
                checkPositions.add(basePos.west());
                checkPositions.add(basePos.south().west());
                checkPositions.add(basePos.south());
            } else {
                checkPositions.add(basePos.east());
                checkPositions.add(basePos.south().east());
                checkPositions.add(basePos.south());
            }
            for (BlockPos checkPos : checkPositions) {
                BlockState blockState = this.mc.level.getBlockState(checkPos);
                if (!HeBlockUtils.isSolid(blockState)) continue;
                return basePos;
            }
        }
        return this.mc.player.blockPosition().below();
    }

    private List<PlaceBlockHelper> getSchematicPlaceBlocks() {
        List<BlockPos> blockPosList;
        int rangeBlocks = (int)Math.ceil(this.printingRange.get());
        double maxDistanceSq = this.printingRange.get() * this.printingRange.get();
        ArrayList<PlaceBlockHelper> placeBlocks = new ArrayList<PlaceBlockHelper>();
        if (this.blockPosSupplier != null) {
            blockPosList = this.blockPosSupplier.get();
        } else {
            int size = rangeBlocks * 2 + 1;
            BlockPos playerPos = this.mc.player.blockPosition();
            blockPosList = new ArrayList<BlockPos>(size * size * size);
            for (int y = -rangeBlocks; y < rangeBlocks; ++y) {
                for (int x = -rangeBlocks; x <= rangeBlocks; ++x) {
                    for (int z = -rangeBlocks; z <= rangeBlocks; ++z) {
                        blockPosList.add(playerPos.offset(x, y, z));
                    }
                }
            }
        }
        for (BlockPos schematicBlockPos : blockPosList) {
            double distanceSq;
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(schematicBlockPos) || (distanceSq = this.mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(schematicBlockPos))) > maxDistanceSq) continue;
            PlaceBlockHelper helper = new PlaceBlockHelper(schematicBlockPos, distanceSq);
            BlockState schematicBlockState = this.schematicWorld.getBlockState(schematicBlockPos);
            BlockState targetBlockState = this.mc.level.getBlockState(schematicBlockPos);
            helper.setCandidateStates(Collections.singletonList(schematicBlockState));
            helper.setTargetState(targetBlockState);
            placeBlocks.add(helper);
        }
        if (this.blockPosSupplier == null) {
            this.sortAlgorithm.get().sort(placeBlocks);
        }
        return placeBlocks;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        this.renderPosList.forEach(pair -> {
            Color color = new Color(this.colour.get().r, this.colour.get().g, this.colour.get().b, (int)((float)pair.getA() / (float)this.fadeTime.get() * (float)this.colour.get().a));
            event.renderer.box((BlockPos)pair.getB(), color, null, ShapeMode.Sides, 0);
        });
    }

    public void startPrinting() {
        if (this.printerMode.get() != PrinterMode.投影打印) {
            this.printerMode.set(PrinterMode.投影打印);
        }
        if (!this.isActive()) {
            this.toggle();
        }
    }

    public void setBlockPosSupplier(Supplier<List<BlockPos>> supplier) {
        this.blockPosSupplier = supplier;
    }

    public void reset() {
        this.renderPosList.clear();
        this.placeCooldownMap.clear();
        this.nextBlockIndex = 0;
    }

    public void onDeactivate() {
        this.reset();
        HeRotationUtils.clearKeptRotation();
        this.blockPosSupplier = null;
    }
}
