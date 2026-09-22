package com.xiaohe66.mc.meteor.lotus.modules;

import com.google.common.reflect.TypeToken;
import com.xiaohe66.mc.meteor.lotus.bo.BlockPosBo;
import com.xiaohe66.mc.meteor.lotus.bo.OffsetRegion;
import com.xiaohe66.mc.meteor.lotus.bo.SpawnerType;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.GsonUtils;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.ChunkIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.block.enums.TrialSpawnerState;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.WorldChunk;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ActivatedSpawnerDetector extends Module {
    private final SettingGroup sgStructure = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");
    private final SettingGroup sgStructureToggles = settings.createGroup("结构开关");
    private final SettingGroup sgRecord = settings.createGroup("位置记录");

    private final Setting<Boolean> trialSpawnerDetection;
    private final Setting<Boolean> showMoreStructureToggles;
    private final Setting<Boolean> dungeonEnabled;
    private final Setting<Boolean> mineshaftEnabled;
    private final Setting<Boolean> bastionEnabled;
    private final Setting<Boolean> mansionEnabled;
    private final Setting<Boolean> netherFortressEnabled;
    private final Setting<Boolean> strongholdEnabled;
    private final Setting<Boolean> chatFeedback;
    private final Setting<Boolean> showCoordinates;
    private final Setting<Boolean> stashHint;
    private final Setting<Boolean> reduceStashSpam;
    private final Setting<Boolean> airDisturbanceDetection;
    private final Setting<Boolean> ignoreAmethystGeodes;
    private final Setting<List<net.minecraft.block.Block>> storageBlocks;
    private final Setting<Set<EntityType<?>>> storageEntities;
    private final Setting<Boolean> disabledSpawnerDetection;
    public final Setting<Integer> torchScanDistance;
    private final Setting<Boolean> reduceRenderSpam;
    public final Setting<Integer> renderDistance;
    private final Setting<Boolean> removeOutOfRangeCache;
    private final Setting<Boolean> traceLines;
    private final Setting<Boolean> nearestOnly;
    private final Setting<ShapeMode> shapeMode;
    private final Setting<SettingColor> spawnerSideColor;
    private final Setting<SettingColor> spawnerLineColor;
    private final Setting<SettingColor> trialSideColor;
    private final Setting<SettingColor> trialLineColor;
    private final Setting<SettingColor> disabledSideColor;
    private final Setting<SettingColor> disabledLineColor;
    private final Setting<Boolean> renderRange;
    private final Setting<SettingColor> rangeSideColor;
    private final Setting<SettingColor> rangeLineColor;
    private final Setting<SettingColor> trialRangeSideColor;
    private final Setting<SettingColor> trialRangeLineColor;
    private final Setting<Boolean> enablePositionRecord;

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final Set<net.minecraft.block.Block> AMETHYST_BLOCKS;
    private static final Set<net.minecraft.block.Block> LIGHT_BLOCKS;
    private final Set<BlockPos> airDisturbanceSet = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> detectedSpawners = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> disabledSpawners = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> trialSpawners = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> noStorageSpawners = Collections.synchronizedSet(new HashSet<>());
    private BlockPos nearestSpawner;
    private final List<BlockPosBo> recordedPositions = new ArrayList<>();
    private final Set<BlockPos> recordedPositionSet = new HashSet<>();
    private static final Map<EntityType<?>, SpawnerType> ENTITY_TYPE_MAP;

    public ActivatedSpawnerDetector() {
        super(Const.CATEGORY, "H刷怪笼激活检测", "检测曾被玩家接近过的刷怪笼，可用于寻找地牢、废弃矿井等地方的玩家藏匿点。");

        this.trialSpawnerDetection = sgStructureToggles.add(new BoolSetting.Builder().name("试炼刷怪笼检测").description("检测已激活的试炼刷怪笼").defaultValue(true).build());
        this.showMoreStructureToggles = sgStructureToggles.add(new BoolSetting.Builder().name("显示更多结构开关").description("展开或收起更多结构开关").defaultValue(false).build());
        this.dungeonEnabled = sgStructureToggles.add(new BoolSetting.Builder().name("地牢").description("启用对地牢的检测").defaultValue(true).visible(this.showMoreStructureToggles::get).build());
        this.mineshaftEnabled = sgStructureToggles.add(new BoolSetting.Builder().name("废弃矿井").description("启用对废弃矿井的检测").defaultValue(true).visible(this.showMoreStructureToggles::get).build());
        this.bastionEnabled = sgStructureToggles.add(new BoolSetting.Builder().name("堡垒遗迹").description("启用对堡垒遗迹的检测").defaultValue(true).visible(this.showMoreStructureToggles::get).build());
        this.mansionEnabled = sgStructureToggles.add(new BoolSetting.Builder().name("林地府邸").description("启用对林地府邸的检测").defaultValue(true).visible(this.showMoreStructureToggles::get).build());
        this.netherFortressEnabled = sgStructureToggles.add(new BoolSetting.Builder().name("下界要塞").description("启用对下界要塞的检测").defaultValue(true).visible(this.showMoreStructureToggles::get).build());
        this.strongholdEnabled = sgStructureToggles.add(new BoolSetting.Builder().name("要塞").description("启用对要塞的检测").defaultValue(true).visible(this.showMoreStructureToggles::get).build());

        this.chatFeedback = sgStructure.add(new BoolSetting.Builder().name("聊天反馈").description("在聊天栏显示刷怪笼信息").defaultValue(true).build());
        this.showCoordinates = sgStructure.add(new BoolSetting.Builder().name("显示坐标").description("在聊天栏显示激活刷怪笼的坐标").defaultValue(true).build());
        this.stashHint = sgStructure.add(new BoolSetting.Builder().name("藏匿点提示").description("开启提醒你附近可能有藏匿物品的提示消息").defaultValue(true).build());
        this.reduceStashSpam = sgStructure.add(new BoolSetting.Builder().name("减少藏匿点刷屏").description("刷怪笼16格内没有储物方块时，不显示藏匿点提示").defaultValue(true).visible(this.stashHint::get).build());
        this.airDisturbanceDetection = sgStructure.add(new BoolSetting.Builder().name("空气扰动检测").description("当刷怪笼周围空气有扰动时也判定为已激活（例如火把被放置后移除）。可能存在误报！").defaultValue(false).build());
        this.ignoreAmethystGeodes = sgStructure.add(new BoolSetting.Builder().name("忽略紫晶洞").description("刷怪笼附近有紫晶洞方块时跳过空气检测，以减少误报").defaultValue(true).visible(this.airDisturbanceDetection::get).build());
        this.storageBlocks = sgStructure.add(new BlockListSetting.Builder().name("储物方块").description("判断是否在聊天提示和渲染时检查的储物方块").defaultValue(Blocks.CHEST, Blocks.BARREL, Blocks.HOPPER, Blocks.DISPENSER).build());
        this.storageEntities = sgStructure.add(new EntityTypeListSetting.Builder().name("储物实体").description("判断是否在聊天提示和渲染时检查的储物实体").defaultValue(EntityType.CHEST_MINECART).build());
        this.disabledSpawnerDetection = sgStructure.add(new BoolSetting.Builder().name("停用刷怪笼检测").description("检测被插上火把等发光方块的刷怪笼").defaultValue(true).build());
        this.torchScanDistance = sgStructure.add(new IntSetting.Builder().name("火把扫描距离").description("以刷怪笼为中心扫描发光方块的距离").defaultValue(1).min(1).sliderRange(1, 10).visible(this.disabledSpawnerDetection::get).build());

        this.reduceRenderSpam = sgRender.add(new BoolSetting.Builder().name("减少渲染刷屏").description("刷怪笼范围内没有储物方块时，不渲染大框").defaultValue(true).build());
        this.renderDistance = sgRender.add(new IntSetting.Builder().name("渲染距离(区块)").description("渲染检测到的刷怪笼时，距离玩家的区块数").defaultValue(32).min(6).sliderRange(6, 1024).build());
        this.removeOutOfRangeCache = sgRender.add(new BoolSetting.Builder().name("移除超出渲染距离的缓存").description("缓存的方块坐标超出渲染距离时将其移除").defaultValue(true).build());
        this.traceLines = sgRender.add(new BoolSetting.Builder().name("追踪线").description("显示指向刷怪笼的追踪线").defaultValue(true).build());
        this.nearestOnly = sgRender.add(new BoolSetting.Builder().name("仅最近刷怪笼追踪线").description("只显示一条指向最近刷怪笼的追踪线").defaultValue(false).build());
        this.shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("形状模式").description("形状的渲染方式").defaultValue(ShapeMode.Both).build());
        this.spawnerSideColor = sgRender.add(new ColorSetting.Builder().name("刷怪笼侧面颜色").description("激活刷怪笼的颜色").defaultValue(new SettingColor(251, 5, 5, 70)).visible(() -> this.shapeMode.get() == ShapeMode.Sides || this.shapeMode.get() == ShapeMode.Both).build());
        this.spawnerLineColor = sgRender.add(new ColorSetting.Builder().name("刷怪笼线条颜色").description("激活刷怪笼的颜色").defaultValue(new SettingColor(251, 5, 5, 235)).visible(() -> this.shapeMode.get() == ShapeMode.Lines || this.shapeMode.get() == ShapeMode.Both || this.traceLines.get()).build());
        this.trialSideColor = sgRender.add(new ColorSetting.Builder().name("试炼刷怪笼侧面颜色").description("激活试炼刷怪笼的颜色").defaultValue(new SettingColor(255, 100, 0, 70)).visible(() -> this.trialSpawnerDetection.get() && (this.shapeMode.get() == ShapeMode.Sides || this.shapeMode.get() == ShapeMode.Both)).build());
        this.trialLineColor = sgRender.add(new ColorSetting.Builder().name("试炼刷怪笼线条颜色").description("激活试炼刷怪笼的颜色").defaultValue(new SettingColor(255, 100, 0, 235)).visible(() -> this.trialSpawnerDetection.get() && (this.shapeMode.get() == ShapeMode.Lines || this.shapeMode.get() == ShapeMode.Both || this.traceLines.get())).build());
        this.disabledSideColor = sgRender.add(new ColorSetting.Builder().name("停用刷怪笼侧面颜色").description("被插上火把的刷怪笼的颜色").defaultValue(new SettingColor(251, 5, 251, 70)).visible(() -> this.disabledSpawnerDetection.get() && (this.shapeMode.get() == ShapeMode.Sides || this.shapeMode.get() == ShapeMode.Both)).build());
        this.disabledLineColor = sgRender.add(new ColorSetting.Builder().name("停用刷怪笼线条颜色").description("被插上火把的刷怪笼的颜色").defaultValue(new SettingColor(251, 5, 251, 235)).visible(() -> this.disabledSpawnerDetection.get() && (this.shapeMode.get() == ShapeMode.Lines || this.shapeMode.get() == ShapeMode.Both)).build());
        this.renderRange = sgRender.add(new BoolSetting.Builder().name("刷怪笼范围渲染").description("渲染刷怪笼的大致激活范围").defaultValue(true).build());
        this.rangeSideColor = sgRender.add(new ColorSetting.Builder().name("刷怪笼范围侧面颜色").description("刷怪笼激活范围的颜色").defaultValue(new SettingColor(5, 178, 251, 30)).visible(() -> this.renderRange.get() && (this.shapeMode.get() == ShapeMode.Sides || this.shapeMode.get() == ShapeMode.Both)).build());
        this.rangeLineColor = sgRender.add(new ColorSetting.Builder().name("刷怪笼范围线条颜色").description("刷怪笼激活范围的颜色").defaultValue(new SettingColor(5, 178, 251, 155)).visible(() -> this.renderRange.get() && (this.shapeMode.get() == ShapeMode.Lines || this.shapeMode.get() == ShapeMode.Both)).build());
        this.trialRangeSideColor = sgRender.add(new ColorSetting.Builder().name("试炼刷怪笼范围侧面颜色").description("试炼刷怪笼激活范围的颜色").defaultValue(new SettingColor(150, 178, 251, 30)).visible(() -> this.trialSpawnerDetection.get() && this.renderRange.get() && (this.shapeMode.get() == ShapeMode.Sides || this.shapeMode.get() == ShapeMode.Both)).build());
        this.trialRangeLineColor = sgRender.add(new ColorSetting.Builder().name("试炼刷怪笼范围线条颜色").description("试炼刷怪笼激活范围的颜色").defaultValue(new SettingColor(150, 178, 251, 155)).visible(() -> this.trialSpawnerDetection.get() && this.renderRange.get() && (this.shapeMode.get() == ShapeMode.Lines || this.shapeMode.get() == ShapeMode.Both)).build());

        this.enablePositionRecord = sgRecord.add(new BoolSetting.Builder().name("启用位置记录").description("将检测到的刷怪笼位置记录到 csv 文件以及本设置菜单的表格中").defaultValue(false).build());
    }

    @Override
    public void onActivate() {
        this.clearAll();
        this.loadRecords();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (this.mc.world != null && this.mc.player != null) {
            Set<WorldChunk> chunks = this.getChunks();
            for (WorldChunk chunk : chunks) {
                for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (blockEntity instanceof MobSpawnerBlockEntity spawnerBlockEntity) {
                        this.checkSpawner(chunk, spawnerBlockEntity);
                    } else if (blockEntity instanceof TrialSpawnerBlockEntity trialSpawnerBlockEntity) {
                        this.checkTrialSpawner(trialSpawnerBlockEntity);
                    }
                }
            }
            if (this.nearestOnly.get()) {
                this.nearestSpawner = this.getNearestSpawner();
            }
            if (this.removeOutOfRangeCache.get()) {
                this.cleanUp(chunks);
            }
        }
    }

    private EntityType<?> getEntityType(net.minecraft.block.spawner.MobSpawnerLogic spawner) {
        MobSpawnerEntry spawnData = spawner.spawnEntry;
        if (spawnData == null) {
            return null;
        }
        NbtCompound tag = spawnData.getNbt();
        NbtElement idTag = tag == null ? null : tag.get("id");
        if (idTag == null) {
            return null;
        }
        String id = idTag.asString().orElse(null);
        if (id == null) {
            return null;
        }
        return EntityType.get(id).orElse(null);
    }

    private String getStructureName(SpawnerType spawnerType, EntityType<?> entityType, BlockPos pos) {
        switch (spawnerType) {
            case DUNGEON:
                if (entityType == EntityType.SPIDER) {
                    if (this.mc.world.getBlockState(pos.up()).getBlock() == Blocks.BIRCH_PLANKS && this.mansionEnabled.get()) {
                        return "林地府邸";
                    }
                }
                if (this.dungeonEnabled.get()) {
                    return spawnerType.getDisplayName();
                }
                return null;
            case ABANDONED_MINESHAFT:
                return this.mineshaftEnabled.get() ? spawnerType.getDisplayName() : "";
            case STRONGHOLD:
                return this.strongholdEnabled.get() ? spawnerType.getDisplayName() : "";
            case NETHER_FORTRESS:
                return this.netherFortressEnabled.get() ? spawnerType.getDisplayName() : "";
            case BASTION:
                return this.bastionEnabled.get() ? spawnerType.getDisplayName() : "";
            default:
                return "";
        }
    }

    private Set<WorldChunk> getChunks() {
        HashSet<WorldChunk> chunks = new HashSet<>();
        ChunkIterator iterator = new ChunkIterator(false);
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            if (chunk instanceof WorldChunk levelChunk) {
                chunks.add(levelChunk);
            }
        }
        return chunks;
    }

    private boolean isDetected(BlockPos pos) {
        return this.detectedSpawners.contains(pos) || this.trialSpawners.contains(pos) || this.disabledSpawners.contains(pos) || this.noStorageSpawners.contains(pos);
    }

    private void checkSpawner(WorldChunk chunk, MobSpawnerBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getPos();
        if (!this.isDetected(pos)) {
            net.minecraft.block.spawner.MobSpawnerLogic spawner = blockEntity.getLogic();
            EntityType<?> entityType = this.getEntityType(spawner);
            SpawnerType spawnerType = entityType == null ? null : ENTITY_TYPE_MAP.getOrDefault(entityType, SpawnerType.SPAWNER);
            boolean reported = false;
            if (this.airDisturbanceDetection.get() && (spawner.spawnDelay == 20 || spawner.spawnDelay == 0)) {
                if (spawnerType != null && spawnerType.getRegion() != null && !this.airDisturbanceSet.contains(pos) && !this.isAmethystArea(chunk, pos) && this.hasAirMix(pos, spawnerType.getRegion())) {
                    reported = this.reportActivated(spawnerType, entityType, pos);
                }
                this.airDisturbanceSet.add(pos);
            } else if (spawner.spawnDelay != 20) {
                if (this.mc.world.getRegistryKey() == World.NETHER && spawner.spawnDelay == 0) {
                    return;
                }
                reported = this.reportActivated(spawnerType == null ? SpawnerType.SPAWNER : spawnerType, entityType, pos);
            }
            if (reported) {
                this.onActivated(pos);
            }
        }
    }

    private void checkTrialSpawner(TrialSpawnerBlockEntity blockEntity) {
        if (this.trialSpawnerDetection.get()) {
            BlockPos pos = blockEntity.getPos();
            if (!this.isDetected(pos)) {
                if (blockEntity.getSpawnerState() != TrialSpawnerState.WAITING_FOR_PLAYERS) {
                    if (this.chatFeedback.get()) {
                        if (this.showCoordinates.get()) {
                            this.info("§cASD§r | 检测到激活的§c试炼§r刷怪笼！坐标: " + pos.toShortString(), new Object[0]);
                        } else {
                            this.info("§cASD§r | 检测到激活的§c试炼§r刷怪笼！", new Object[0]);
                        }
                    }
                    this.trialSpawners.add(pos);
                    boolean hasStorage = this.hasStorageNearby(pos, 14);
                    if (!hasStorage && this.reduceRenderSpam.get()) {
                        this.noStorageSpawners.add(pos);
                    }
                    this.stashMessage(hasStorage);
                    if (this.enablePositionRecord.get()) {
                        this.recordPosition(pos);
                    }
                }
            }
        }
    }

    private boolean reportActivated(SpawnerType spawnerType, EntityType<?> entityType, BlockPos pos) {
        if (!this.chatFeedback.get()) {
            return false;
        }
        String structureName = this.getStructureName(spawnerType, entityType, pos);
        boolean reported = false;
        if (structureName != null) {
            this.detectedSpawners.add(pos);
            String name = structureName.isEmpty() ? "刷怪笼" : "§c" + structureName + "§r刷怪笼";
            if (this.showCoordinates.get()) {
                this.info("§cASD§r | 检测到激活的" + name + "！坐标: " + pos.toShortString(), new Object[0]);
            } else {
                this.info("§cASD§r | 检测到激活的" + name + "！", new Object[0]);
            }
            reported = true;
        }
        if (this.enablePositionRecord.get()) {
            this.recordPosition(pos);
        }
        return reported;
    }

    private void onActivated(BlockPos pos) {
        if (this.disabledSpawnerDetection.get()) {
            this.torchScan(pos);
        }
        boolean hasStorage = this.hasStorageNearby(pos, 16);
        if (!hasStorage && this.reduceRenderSpam.get()) {
            this.noStorageSpawners.add(pos);
        }
        this.stashMessage(hasStorage);
    }

    private void stashMessage(boolean hasStorage) {
        if (this.chatFeedback.get() && this.stashHint.get() && (hasStorage || !this.reduceStashSpam.get())) {
            this.error("刷怪笼附近的储物容器中可能藏有物品！", new Object[0]);
        }
    }

    private void torchScan(BlockPos pos) {
        int distance = this.torchScanDistance.get();
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-distance, -distance, -distance), pos.add(distance, distance, distance))) {
            if (LIGHT_BLOCKS.contains(this.mc.world.getBlockState(blockPos).getBlock())) {
                this.disabledSpawners.add(pos);
                if (this.chatFeedback.get()) {
                    this.warning("该刷怪笼附近有火把或其他发光方块！", new Object[0]);
                }
                return;
            }
        }
    }

    private boolean hasAirMix(BlockPos pos, OffsetRegion region) {
        boolean hasAir = false;
        boolean hasCaveAir = false;
        BlockPos min = pos.add(region.getMinX(), region.getMinY(), region.getMinZ());
        BlockPos max = pos.add(region.getMaxX(), region.getMaxY(), region.getMaxZ());
        for (BlockPos blockPos : BlockPos.iterate(min, max)) {
            net.minecraft.block.Block block = this.mc.world.getBlockState(blockPos).getBlock();
            if (block == Blocks.AIR) {
                hasAir = true;
            } else if (block == Blocks.CAVE_AIR) {
                hasCaveAir = true;
            }
            if (hasAir && hasCaveAir) {
                return true;
            }
        }
        return false;
    }

    private boolean isAmethystArea(WorldChunk chunk, BlockPos pos) {
        if (!this.ignoreAmethystGeodes.get()) {
            return false;
        }
        if (!this.chunkHasAmethyst(chunk, Math.min(chunk.getSectionArray().length, 20))) {
            return false;
        }
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-5, -5, -5), pos.add(5, 5, 5))) {
            if (AMETHYST_BLOCKS.contains(this.mc.world.getBlockState(blockPos).getBlock())) {
                return true;
            }
        }
        return false;
    }

    private boolean chunkHasAmethyst(WorldChunk chunk, int sectionCount) {
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sectionCount; ++i) {
            ChunkSection section = sections[i];
            if (!section.isEmpty()) {
                Palette<BlockState> palette = section.getBlockStateContainer().data.palette();
                for (int j = 0; j < palette.getSize(); ++j) {
                    if (AMETHYST_BLOCKS.contains(palette.get(j).getBlock())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasStorageNearby(BlockPos pos, int distance) {
        Set<EntityType<?>> entityTypes = this.storageEntities.get();
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-distance, -distance, -distance), pos.add(distance, distance, distance))) {
            if (this.storageBlocks.get().contains(this.mc.world.getBlockState(blockPos).getBlock())) {
                return true;
            }
            if (!entityTypes.isEmpty() && !this.mc.world.getOtherEntities((Entity)null, new Box(blockPos), entity -> entityTypes.contains(entity.getType())).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private BlockPos getNearestSpawner() {
        try {
            HashSet<BlockPos> all = new HashSet<>();
            all.addAll(this.detectedSpawners);
            all.addAll(this.disabledSpawners);
            all.addAll(this.trialSpawners);
            BlockPos nearest = null;
            double minDistance = Double.MAX_VALUE;
            for (BlockPos pos : all) {
                double dx = pos.getX() - this.mc.player.getBlockX();
                double dz = pos.getZ() - this.mc.player.getBlockZ();
                double distance = dx * dx + dz * dz;
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = pos;
                }
            }
            return nearest;
        } catch (Exception e) {
            return null;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.mc.player != null) {
            if (this.spawnerSideColor.get().a > 5 || this.spawnerLineColor.get().a > 5 || this.rangeSideColor.get().a > 5 || this.rangeLineColor.get().a > 5) {
                this.renderSet(event, this.detectedSpawners, 16, true, this.spawnerSideColor.get(), this.spawnerLineColor.get(), this.rangeSideColor.get(), this.rangeLineColor.get());
                this.renderSet(event, this.trialSpawners, 14, this.trialSpawnerDetection.get(), this.trialSideColor.get(), this.trialLineColor.get(), this.trialRangeSideColor.get(), this.trialRangeLineColor.get());
            }
        }
    }

    private void renderSet(Render3DEvent event, Set<BlockPos> positions, int range, boolean renderRangeBox, SettingColor sideColor, SettingColor lineColor, SettingColor rangeSideColor, SettingColor rangeLineColor) {
        synchronized (positions) {
            for (BlockPos pos : positions) {
                if (this.isInRenderRange(pos)) {
                    boolean shouldRenderRange = this.renderRange.get() && renderRangeBox && (!this.reduceRenderSpam.get() || !this.noStorageSpawners.contains(pos));
                    if (shouldRenderRange) {
                        Box box = new Box(pos.getX() - range, pos.getY() - range, pos.getZ() - range, pos.getX() + range + 1, pos.getY() + range + 1, pos.getZ() + range + 1);
                        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, rangeSideColor, rangeLineColor, this.shapeMode.get(), 0);
                    }
                    boolean isDisabled = this.disabledSpawners.contains(pos);
                    Color side = isDisabled ? this.disabledSideColor.get() : sideColor;
                    Color line = isDisabled ? this.disabledLineColor.get() : lineColor;
                    this.renderBox(event, new Box(pos), side, line);
                }
            }
        }
        if (this.nearestOnly.get() && this.nearestSpawner != null) {
            this.renderNearest(event, sideColor, lineColor);
        }
    }

    private void renderBox(Render3DEvent event, Box box, Color sideColor, Color lineColor) {
        if (this.traceLines.get() && !this.nearestOnly.get()) {
            this.renderLine(event, box, lineColor);
        }
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sideColor, TRANSPARENT, this.shapeMode.get(), 0);
    }

    private void renderNearest(Render3DEvent event, Color sideColor, Color lineColor) {
        Box box = new Box(this.nearestSpawner.getX(), this.nearestSpawner.getY(), this.nearestSpawner.getZ(), this.nearestSpawner.getX(), this.nearestSpawner.getY(), this.nearestSpawner.getZ());
        if (this.traceLines.get()) {
            this.renderLine(event, box, lineColor);
        }
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sideColor, TRANSPARENT, ShapeMode.Sides, 0);
    }

    private void renderLine(Render3DEvent event, Box box, Color color) {
        event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX + 0.5, (box.minY + box.maxY) / 2.0, box.minZ + 0.5, color);
    }

    private void cleanUp(Set<WorldChunk> chunks) {
        this.airDisturbanceSet.removeIf(pos -> this.isOutOfChunks(pos, chunks));
        this.detectedSpawners.removeIf(pos -> this.isOutOfChunks(pos, chunks));
        this.disabledSpawners.removeIf(pos -> this.isOutOfChunks(pos, chunks));
        this.trialSpawners.removeIf(pos -> this.isOutOfChunks(pos, chunks));
        this.noStorageSpawners.removeIf(pos -> this.isOutOfChunks(pos, chunks));
    }

    private boolean isOutOfChunks(BlockPos pos, Set<WorldChunk> chunks) {
        return !chunks.contains(this.mc.world.getChunk(pos));
    }

    private boolean isInRenderRange(BlockPos pos) {
        BlockPos playerPos = new BlockPos(this.mc.player.getBlockX(), pos.getY(), this.mc.player.getBlockZ());
        return playerPos.isWithinDistance(pos, this.renderDistance.get() * 16);
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        this.recordedPositions.sort(Comparator.comparingInt(pos -> pos.y));
        WVerticalList list = theme.verticalList();
        WButton clearButton = list.add(theme.button("清除已记录的坐标")).widget();
        WTable table = new WTable();
        if (!this.recordedPositions.isEmpty()) {
            list.add(table);
        }
        clearButton.action = () -> {
            this.recordedPositions.clear();
            this.recordedPositionSet.clear();
            table.clear();
            this.saveJson();
            this.saveCsv();
        };
        this.buildTable(theme, table);
        return list;
    }

    private void buildTable(GuiTheme theme, WTable table) {
        ArrayList<BlockPosBo> unique = new ArrayList<>();
        for (BlockPosBo pos : this.recordedPositions) {
            if (!unique.contains(pos)) {
                unique.add(pos);
                table.add(theme.label("坐标: " + pos.x + ", " + pos.y + ", " + pos.z));
                WButton goButton = table.add(theme.button("前往")).widget();
                goButton.action = () -> PathManagers.get().moveTo(new BlockPos(pos.x, pos.y, pos.z), true);
                WMinus minus = table.add(theme.minus()).widget();
                minus.action = () -> {
                    this.recordedPositions.remove(pos);
                    this.recordedPositionSet.remove(new BlockPos(pos.x, pos.y, pos.z));
                    table.clear();
                    this.buildTable(theme, table);
                    this.saveJson();
                    this.saveCsv();
                };
                table.row();
            }
        }
    }

    private void loadRecords() {
        Path file = this.getRecordFolder().resolve("spawners.json");
        boolean loaded = false;
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                List<BlockPosBo> list = GsonUtils.GSON.fromJson(reader, new SpawnerRecordListType().getType());
                if (list != null) {
                    this.recordedPositions.addAll(list);
                    for (BlockPosBo pos : list) {
                        this.recordedPositionSet.add(new BlockPos(pos.x, pos.y, pos.z));
                    }
                    loaded = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!loaded) {
            file = this.getRecordFolder().resolve("spawners.csv");
            if (Files.exists(file)) {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        BlockPosBo pos = new BlockPosBo(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        this.recordedPositions.add(pos);
                        this.recordedPositionSet.add(new BlockPos(pos.x, pos.y, pos.z));
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void recordPosition(BlockPos pos) {
        if (!this.recordedPositionSet.contains(pos)) {
            this.recordedPositionSet.add(pos);
            this.recordedPositions.add(new BlockPosBo(pos.getX(), pos.getY(), pos.getZ()));
            this.saveJson();
            this.saveCsv();
        }
    }

    private void saveCsv() {
        try {
            Path file = this.getRecordFolder().resolve("spawners.csv");
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("X,Y,Z\n");
                for (BlockPosBo pos : this.recordedPositions) {
                    pos.write(writer);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveJson() {
        try {
            Path file = this.getRecordFolder().resolve("spawners.json");
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GsonUtils.GSON.toJson(this.recordedPositions, writer);
            }
        } catch (Exception ignored) {
        }
    }

    private Path getRecordFolder() {
        return Const.LOTUS_DIR.resolve("spawner").resolve(sanitize(LotusUtils.getServerId())).resolve(sanitize(LotusUtils.getWorldId()));
    }

    private static String sanitize(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @Override
    public void onDeactivate() {
        this.clearAll();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen || event.screen instanceof LevelLoadingScreen) {
            this.clearAll();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        this.clearAll();
    }

    private void clearAll() {
        this.airDisturbanceSet.clear();
        this.detectedSpawners.clear();
        this.disabledSpawners.clear();
        this.noStorageSpawners.clear();
        this.trialSpawners.clear();
        this.nearestSpawner = null;
    }

    private static class SpawnerRecordListType extends TypeToken<List<BlockPosBo>> {
    }

    static {
        AMETHYST_BLOCKS = Set.of(Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.CALCITE, Blocks.SMOOTH_BASALT, Blocks.AMETHYST_CLUSTER, Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD);
        LIGHT_BLOCKS = Set.of(Blocks.TORCH, Blocks.SOUL_TORCH, Blocks.REDSTONE_TORCH, Blocks.JACK_O_LANTERN, Blocks.GLOWSTONE, Blocks.SHROOMLIGHT, Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.SEA_LANTERN, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
        ENTITY_TYPE_MAP = new HashMap<>();
        for (SpawnerType spawnerType : SpawnerType.values()) {
            for (EntityType<?> entityType : spawnerType.getEntityTypes()) {
                ENTITY_TYPE_MAP.put(entityType, spawnerType);
            }
        }
    }
}
