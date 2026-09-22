/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.events.world.TickEvent$Pre
 *  meteordevelopment.meteorclient.gui.GuiTheme
 *  meteordevelopment.meteorclient.gui.renderer.GuiRenderer
 *  meteordevelopment.meteorclient.gui.widgets.WWidget
 *  meteordevelopment.meteorclient.gui.widgets.containers.WSection
 *  meteordevelopment.meteorclient.gui.widgets.containers.WTable
 *  meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList
 *  meteordevelopment.meteorclient.gui.widgets.pressable.WButton
 *  meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox
 *  meteordevelopment.meteorclient.gui.widgets.pressable.WMinus
 *  meteordevelopment.meteorclient.renderer.Renderer3D
 *  meteordevelopment.meteorclient.settings.BoolSetting
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.ColorSetting$Builder
 *  meteordevelopment.meteorclient.settings.DoubleSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  meteordevelopment.meteorclient.systems.modules.render.Freecam
 *  meteordevelopment.meteorclient.utils.misc.Pool
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.meteorclient.utils.render.color.SettingColor
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  meteordevelopment.meteorclient.utils.world.BlockUtils$MobSpawn
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.world.BlockView
 *  net.minecraft.world.LightType
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.BlockPos$Mutable
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Position
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.nbt.NbtCompound
 *  net.minecraft.block.SnowBlock
 *  net.minecraft.nbt.NbtList
 *  net.minecraft.nbt.NbtElement
 *  net.minecraft.block.BlockState
 *  net.minecraft.state.property.Property
 *  net.minecraft.client.gui.screen.Screen
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.redstone.Marker;
import com.xiaohe66.mc.meteor.lotus.modules.redstone.SphereConfig;
import com.xiaohe66.mc.meteor.lotus.modules.redstone.SphereEditScreen;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;

public class RedstoneAssist extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup lightOverlayGroup = settings.createGroup("亮度显示");
    private final Setting<Boolean> lightOverlay = lightOverlayGroup.add(new BoolSetting.Builder()
        .name("亮度显示")
        .description("渲染亮度数值")
        .defaultValue(true)
        .onChanged(this::onLightOverlayChanged)
        .build());
    private final Setting<Integer> horizontalRange = lightOverlayGroup.add(new IntSetting.Builder()
        .name("水平范围")
        .description("水平扫描范围（格）")
        .defaultValue(32)
        .min(1)
        .sliderMax(64)
        .build());
    private final Setting<Integer> verticalRange = lightOverlayGroup.add(new IntSetting.Builder()
        .name("垂直范围")
        .description("垂直扫描范围（格）")
        .defaultValue(24)
        .min(1)
        .sliderMax(64)
        .build());
    private final Setting<Boolean> seeThroughBlocks = lightOverlayGroup.add(new BoolSetting.Builder()
        .name("透视显示")
        .description("允许透过方块看到显示")
        .defaultValue(false)
        .build());
    private final Setting<Integer> spawnThreshold = lightOverlayGroup.add(new IntSetting.Builder()
        .name("刷怪阈值")
        .description("低于此亮度可能刷怪（原版1.21为0, 旧版为7, 下界猪人为11）")
        .defaultValue(0)
        .min(0)
        .sliderMax(15)
        .build());
    private final Setting<Boolean> showBox = lightOverlayGroup.add(new BoolSetting.Builder()
        .name("显示方框")
        .description("危险区域显示方框")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> showNumber = lightOverlayGroup.add(new BoolSetting.Builder()
        .name("显示数字")
        .description("显示亮度数值")
        .defaultValue(true)
        .build());
    private final Setting<Double> numberScale = lightOverlayGroup.add(new DoubleSetting.Builder()
        .name("数字大小")
        .description("数字显示大小")
        .defaultValue(0.1)
        .min(0.01)
        .sliderMax(0.2)
        .decimalPlaces(2)
        .build());
    private final Setting<SettingColor> spawnColor = lightOverlayGroup.add(new ColorSetting.Builder()
        .name("刷怪颜色")
        .description("可能刷怪时的显示颜色")
        .defaultValue(new SettingColor(200, 0, 0))
        .build());
    private final Setting<SettingColor> safeColor = lightOverlayGroup.add(new ColorSetting.Builder()
        .name("安全颜色")
        .description("亮度安全时的显示颜色")
        .defaultValue(new SettingColor(0, 200, 0))
        .build());
    private final Setting<Boolean> includeSmallMobs = lightOverlayGroup.add(new BoolSetting.Builder()
        .name("含矮小生物")
        .description("对仅有1格净空的位置(蜘蛛/洞穴蜘蛛/蠹虫/末影螨)也显示为可刷怪")
        .defaultValue(false)
        .build());
    private final List<SphereConfig> sphereConfigs;
    private final Map<SphereConfig, List<BlockPos>> spawnableBlocksMap;
    private final Map<SphereConfig, Integer> scanCooldownMap;
    private final Pool<Marker> markerPool;
    private final List<Marker> markers;
    private static final boolean[][] DIGIT_SEGMENTS = new boolean[][]{{true, true, true, false, true, true, true}, {false, false, true, false, false, true, false}, {true, false, true, true, true, false, true}, {true, false, true, true, false, true, true}, {false, true, true, true, false, true, false}, {true, true, false, true, false, true, true}, {true, true, false, true, true, true, true}, {true, false, true, false, false, true, false}, {true, true, true, true, true, true, true}, {true, true, true, true, false, true, true}};
    private static final double[][][] SEGMENT_LINES = new double[10][7][4];
    private final double[] cornersCache;

    public RedstoneAssist() {
        super(Const.CATEGORY, "V生电辅助", "显示方块亮度等级和刷怪风险区域");
        this.sphereConfigs = new ArrayList<SphereConfig>();
        this.spawnableBlocksMap = new HashMap<SphereConfig, List<BlockPos>>();
        this.scanCooldownMap = new HashMap<SphereConfig, Integer>();
        this.markerPool = new Pool(Marker::new);
        this.markers = new ArrayList<Marker>();
        this.cornersCache = new double[8];
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.lightOverlay.get()) {
            this.markerPool.freeAll(this.markers);
            this.markers.clear();
            Vec3d cameraPos = HePosUtils.getCameraPos();
            int cameraX = (int)Math.floor(cameraPos.x);
            int cameraY = (int)Math.floor(cameraPos.y);
            int cameraZ = (int)Math.floor(cameraPos.z);
            int hRange = this.horizontalRange.get();
            int vRange = this.verticalRange.get();
            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int x = cameraX - hRange; x <= cameraX + hRange; ++x) {
                for (int z = cameraZ - hRange; z <= cameraZ + hRange; ++z) {
                    for (int y = Math.max(this.mc.world.getBottomY(), cameraY - vRange); y <= cameraY + vRange && y <= this.mc.world.getHeight(); ++y) {
                        BlockPos downPos;
                        pos.set(x, y, z);
                        BlockState blockState = this.mc.world.getBlockState(pos);
                        if (!blockState.isAir() && HeBlockUtils.isRail(blockState)) continue;
                        BlockUtils.MobSpawn mobSpawn = this.getMobSpawn(pos, blockState, this.spawnThreshold.get());
                        if (mobSpawn == BlockUtils.MobSpawn.Always || mobSpawn == BlockUtils.MobSpawn.Potential) {
                            if (!HeBlockUtils.isAboveClear(pos) && !this.includeSmallMobs.get()) continue;
                            int lightLevel = this.getLightLevel(pos);
                            this.markers.add(this.markerPool.get().set(pos, false, lightLevel));
                            continue;
                        }
                        if (!HeBlockUtils.isReplaceable(blockState, pos) || !this.isValidSpawnBase(downPos = pos.down()) || !HeBlockUtils.isAboveClear(pos) && !this.includeSmallMobs.get()) continue;
                        int lightLevel = this.getLightLevel(pos);
                        this.markers.add(this.markerPool.get().set(pos, true, lightLevel));
                    }
                }
            }
        }
        this.scanSpheres();
    }

    private void scanSpheres() {
        for (SphereConfig config : this.sphereConfigs) {
            if (!config.enabled.get() || !config.showSpawnableBlocks.get()) continue;
            Integer cooldown = this.scanCooldownMap.getOrDefault(config, 0);
            if (cooldown > 0) {
                this.scanCooldownMap.put(config, cooldown - 1);
                continue;
            }
            this.scanCooldownMap.put(config, config.scanInterval.get());
            BlockPos centerPos = config.center.get();
            int radius = config.radius.get();
            double centerX = (double)centerPos.getX() + 0.5;
            double centerY = (double)centerPos.getY() + 0.5;
            double centerZ = (double)centerPos.getZ() + 0.5;
            double radiusSq = (double)radius * (double)radius;
            ArrayList<BlockPos> spawnablePosList = new ArrayList<BlockPos>();
            BlockPos.Mutable mutablePos = new BlockPos.Mutable();
            int minX = centerPos.getX() - radius;
            int maxX = centerPos.getX() + radius;
            int minY = Math.max(this.mc.world.getBottomY(), centerPos.getY() - radius);
            int maxY = Math.min(this.mc.world.getHeight(), centerPos.getY() + radius);
            int minZ = centerPos.getZ() - radius;
            int maxZ = centerPos.getZ() + radius;
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    for (int y = minY; y <= maxY; ++y) {
                        double dx = (double)x + 0.5 - centerX;
                        double dy = (double)y + 0.5 - centerY;
                        double dz = (double)z + 0.5 - centerZ;
                        if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
                        mutablePos.set(x, y, z);
                        BlockState state = this.mc.world.getBlockState(mutablePos);
                        if (!this.isFullOpaqueCube(state, mutablePos)) continue;
                        mutablePos.set(x, y + 1, z);
                        BlockState aboveState = this.mc.world.getBlockState(mutablePos);
                        if (!HeBlockUtils.isReplaceable(aboveState, mutablePos) || HeBlockUtils.isRail(aboveState) || !HeBlockUtils.isAboveClear(mutablePos) && !config.includeSmallMobs.get()) continue;
                        spawnablePosList.add(new BlockPos(x, y, z));
                    }
                }
            }
            this.spawnableBlocksMap.put(config, spawnablePosList);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent render3DEvent) {
        Renderer3D renderer3D = this.seeThroughBlocks.get() != false ? render3DEvent.renderer : render3DEvent.depthRenderer;
        Direction facing = this.getCameraFacing();
        for (Marker marker : this.markers) {
            this.renderMarker(renderer3D, marker, facing);
        }
        for (SphereConfig config : this.sphereConfigs) {
            if (!config.enabled.get()) continue;
            this.renderSphereWireframe(renderer3D, config);
            if (!config.showSpawnableBlocks.get()) continue;
            Renderer3D sphereRenderer = config.seeThrough.get() != false ? render3DEvent.renderer : render3DEvent.depthRenderer;
            this.renderSpawnableBlocks(sphereRenderer, config);
        }
    }

    private void renderMarker(Renderer3D renderer3D, Marker marker, Direction facing) {
        Color color = marker.safe ? this.safeColor.get() : this.spawnColor.get();
        double renderY = marker.y + 0.01;
        if (this.showBox.get() && !marker.safe) {
            this.renderBox(renderer3D, marker, color, renderY);
        }
        if (this.showNumber.get()) {
            this.renderNumber(renderer3D, marker, color, renderY, facing);
        }
    }

    private void renderBox(Renderer3D renderer3D, Marker marker, Color color, double y) {
        double inset = 0.05;
        renderer3D.line(marker.x + inset, y, marker.z + inset, marker.x + 1.0 - inset, y, marker.z + inset, color);
        renderer3D.line(marker.x + 1.0 - inset, y, marker.z + inset, marker.x + 1.0 - inset, y, marker.z + 1.0 - inset, color);
        renderer3D.line(marker.x + 1.0 - inset, y, marker.z + 1.0 - inset, marker.x + inset, y, marker.z + 1.0 - inset, color);
        renderer3D.line(marker.x + inset, y, marker.z + 1.0 - inset, marker.x + inset, y, marker.z + inset, color);
    }

    private void renderNumber(Renderer3D renderer3D, Marker marker, Color color, double y, Direction facing) {
        double centerX = marker.x + 0.5;
        double centerZ = marker.z + 0.5;
        double scale = this.numberScale.get();
        if (marker.lightLevel >= 0 && marker.lightLevel <= 9) {
            this.calculateCorners(centerX, centerZ, scale, facing, this.cornersCache);
            this.renderDigit7Segment(renderer3D, color, this.cornersCache, y, marker.lightLevel);
        } else if (marker.lightLevel >= 10 && marker.lightLevel <= 15) {
            double digitZ2;
            double digitX1;
            double digitZ1;
            double digitX2;
            double gap = 0.05;
            double digitScale = scale * 0.9;
            double digitOffset = digitScale + gap;
            digitZ2 = switch (facing) {
                case Direction.NORTH -> {
                    digitX2 = centerX - digitOffset - 0.1;
                    digitZ1 = centerZ;
                    digitX1 = centerX + digitOffset - 0.1;
                    yield centerZ;
                }
                case Direction.SOUTH -> {
                    digitX2 = centerX - digitOffset + 0.1;
                    digitZ1 = centerZ;
                    digitX1 = centerX + digitOffset + 0.1;
                    yield centerZ;
                }
                case Direction.WEST -> {
                    digitX2 = centerX;
                    digitZ1 = centerZ - digitOffset + 0.1;
                    digitX1 = centerX;
                    yield centerZ + digitOffset + 0.1;
                }
                case Direction.EAST -> {
                    digitX2 = centerX;
                    digitZ1 = centerZ - digitOffset - 0.1;
                    digitX1 = centerX;
                    yield centerZ + digitOffset - 0.1;
                }
                default -> {
                    digitX2 = centerX - digitOffset;
                    digitZ1 = centerZ;
                    digitX1 = centerX + digitOffset;
                    yield centerZ;
                }
            };
            if (facing == Direction.NORTH || facing == Direction.EAST) {
                this.calculateCorners(digitX1, digitZ2, digitScale, facing, this.cornersCache);
                this.renderDigit7Segment(renderer3D, color, this.cornersCache, y, marker.lightLevel % 10);
                this.calculateCorners(digitX2, digitZ1, digitScale, facing, this.cornersCache);
                this.renderDigit7Segment(renderer3D, color, this.cornersCache, y, marker.lightLevel / 10);
            } else {
                this.calculateCorners(digitX1, digitZ2, digitScale, facing, this.cornersCache);
                this.renderDigit7Segment(renderer3D, color, this.cornersCache, y, marker.lightLevel / 10);
                this.calculateCorners(digitX2, digitZ1, digitScale, facing, this.cornersCache);
                this.renderDigit7Segment(renderer3D, color, this.cornersCache, y, marker.lightLevel % 10);
            }
        }
    }

    private void calculateCorners(double centerX, double centerZ, double halfWidth, Direction facing, double[] corners) {
        double halfLength = halfWidth * 2.0;
        switch (facing) {
            case NORTH: {
                corners[0] = centerX - halfWidth;
                corners[1] = centerZ - halfLength;
                corners[2] = centerX + halfWidth;
                corners[3] = centerZ - halfLength;
                corners[4] = centerX + halfWidth;
                corners[5] = centerZ + halfLength;
                corners[6] = centerX - halfWidth;
                corners[7] = centerZ + halfLength;
                break;
            }
            case SOUTH: {
                corners[0] = centerX + halfWidth;
                corners[1] = centerZ + halfLength;
                corners[2] = centerX - halfWidth;
                corners[3] = centerZ + halfLength;
                corners[4] = centerX - halfWidth;
                corners[5] = centerZ - halfLength;
                corners[6] = centerX + halfWidth;
                corners[7] = centerZ - halfLength;
                break;
            }
            case WEST: {
                corners[0] = centerX - halfLength;
                corners[1] = centerZ + halfWidth;
                corners[2] = centerX - halfLength;
                corners[3] = centerZ - halfWidth;
                corners[4] = centerX + halfLength;
                corners[5] = centerZ - halfWidth;
                corners[6] = centerX + halfLength;
                corners[7] = centerZ + halfWidth;
                break;
            }
            case EAST: {
                corners[0] = centerX + halfLength;
                corners[1] = centerZ - halfWidth;
                corners[2] = centerX + halfLength;
                corners[3] = centerZ + halfWidth;
                corners[4] = centerX - halfLength;
                corners[5] = centerZ + halfWidth;
                corners[6] = centerX - halfLength;
                corners[7] = centerZ - halfWidth;
                break;
            }
            default: {
                corners[0] = centerX - halfWidth;
                corners[1] = centerZ - halfLength;
                corners[2] = centerX + halfWidth;
                corners[3] = centerZ - halfLength;
                corners[4] = centerX + halfWidth;
                corners[5] = centerZ + halfLength;
                corners[6] = centerX - halfWidth;
                corners[7] = centerZ + halfLength;
            }
        }
    }

    private void renderDigit7Segment(Renderer3D renderer3D, Color color, double[] corners, double y, int digit) {
        double x1 = corners[0];
        double z1 = corners[1];
        double x2 = corners[2];
        double z2 = corners[3];
        double x3 = corners[6];
        double z3 = corners[7];
        double width = x2 - x1;
        double height = z2 - z1;
        double halfWidth = (x3 - x1) * 0.5;
        double halfHeight = (z3 - z1) * 0.5;
        boolean[] segments = DIGIT_SEGMENTS[digit];
        double[][] segmentLines = SEGMENT_LINES[digit];
        for (int i = 0; i < 7; ++i) {
            if (!segments[i]) continue;
            double[] line = segmentLines[i];
            double startX = x1 + line[0] * width + line[1] * halfWidth;
            double startZ = z1 + line[0] * height + line[1] * halfHeight;
            double endX = x1 + line[2] * width + line[3] * halfWidth;
            double endZ = z1 + line[2] * height + line[3] * halfHeight;
            this.drawThickLine(renderer3D, color, startX, y, startZ, endX, y, endZ);
        }
    }

    private void drawThickLine(Renderer3D renderer3D, Color color, double x1, double y, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) {
            return;
        }
        double dirX = dx / length;
        double dirZ = dz / length;
        double normalX = -dirZ * 0.025;
        double normalZ = dirX * 0.025;
        double p1x = x1 + normalX;
        double p1z = z1 + normalZ;
        double p2x = x1 - normalX;
        double p2z = z1 - normalZ;
        double p3x = x2 - normalX;
        double p3z = z2 - normalZ;
        double p4x = x2 + normalX;
        double p4z = z2 + normalZ;
        renderer3D.quad(p1x, y, p1z, p2x, y, p2z, p3x, y2, p3z, p4x, y2, p4z, color);
    }

    private void renderSpawnableBlocks(Renderer3D renderer3D, SphereConfig config) {
        List<BlockPos> spawnablePosList = this.spawnableBlocksMap.get(config);
        if (spawnablePosList == null || spawnablePosList.isEmpty()) {
            return;
        }
        Color color = config.spawnableBlockColor.get();
        Color lineColor = new Color(color.r, color.g, color.b, Math.min(255, color.a + 100));
        Vec3d cameraPos = HePosUtils.getCameraPos();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;
        int renderCount = Math.min(spawnablePosList.size(), config.renderLimit.get());
        ArrayList<BlockPos> sortedList = new ArrayList<BlockPos>(spawnablePosList);
        sortedList.sort((pos1, pos2) -> {
            double dist1 = pos1.getSquaredDistance(cameraX, cameraY, cameraZ);
            double dist2 = pos2.getSquaredDistance(cameraX, cameraY, cameraZ);
            return Double.compare(dist1, dist2);
        });
        for (int i = 0; i < renderCount; ++i) {
            BlockPos pos = sortedList.get(i);
            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();
            double topY = y + 1.005;
            renderer3D.quad(x, topY, z, x, topY, z + 1.0, x + 1.0, topY, z + 1.0, x + 1.0, topY, z, color);
            renderer3D.line(x, topY, z, x + 1.0, topY, z, lineColor);
            renderer3D.line(x + 1.0, topY, z, x + 1.0, topY, z + 1.0, lineColor);
            renderer3D.line(x + 1.0, topY, z + 1.0, x, topY, z + 1.0, lineColor);
            renderer3D.line(x, topY, z + 1.0, x, topY, z, lineColor);
        }
    }

    private void renderSphereWireframe(Renderer3D renderer3D, SphereConfig config) {
        double x2;
        double z2;
        double x1;
        double z1;
        double theta2;
        double theta1;
        double ringRadius;
        double ringY;
        double phi;
        int i;
        BlockPos centerPos = config.center.get();
        double centerX = (double)centerPos.getX() + 0.5;
        double centerY = (double)centerPos.getY() + 0.5;
        double centerZ = (double)centerPos.getZ() + 0.5;
        double radius = config.radius.get();
        int segments = config.segments.get();
        Color color = config.wireframeColor.get();
        int halfSegments = Math.max(2, segments / 2);
        for (i = 1; i < halfSegments; ++i) {
            phi = Math.PI * (double)i / (double)halfSegments;
            ringY = centerY + radius * Math.cos(phi);
            ringRadius = radius * Math.sin(phi);
            for (int j = 0; j < segments; ++j) {
                theta1 = Math.PI * 2 * (double)j / (double)segments;
                theta2 = Math.PI * 2 * (double)((j + 1) % segments) / (double)segments;
                x1 = centerX + ringRadius * Math.cos(theta1);
                z1 = centerZ + ringRadius * Math.sin(theta1);
                x2 = centerX + ringRadius * Math.cos(theta2);
                z2 = centerZ + ringRadius * Math.sin(theta2);
                renderer3D.line(x1, ringY, z1, x2, ringY, z2, color);
            }
        }
        for (i = 0; i < segments; ++i) {
            phi = Math.PI * 2 * (double)i / (double)segments;
            for (int j = 0; j < halfSegments; ++j) {
                double phi1 = Math.PI * (double)j / (double)halfSegments;
                double phi2 = Math.PI * (double)(j + 1) / (double)halfSegments;
                double x = centerX + radius * Math.sin(phi1) * Math.cos(phi);
                double y = centerY + radius * Math.cos(phi1);
                double z = centerZ + radius * Math.sin(phi1) * Math.sin(phi);
                double x2b = centerX + radius * Math.sin(phi2) * Math.cos(phi);
                double y2b = centerY + radius * Math.cos(phi2);
                double z2b = centerZ + radius * Math.sin(phi2) * Math.sin(phi);
                renderer3D.line(x, y, z, x2b, y2b, z2b, color);
            }
        }
    }

    private int getLightLevel(BlockPos pos) {
        return this.mc.world.getLightLevel(LightType.BLOCK, pos);
    }

    private BlockUtils.MobSpawn getMobSpawn(BlockPos pos, BlockState state, int threshold) {
        boolean isThinSnow = state.getBlock() instanceof SnowBlock && (Integer)state.get((Property)SnowBlock.LAYERS) == 1;
        if (!HeBlockUtils.isReplaceable(state, pos) && !isThinSnow) {
            return BlockUtils.MobSpawn.Never;
        }
        if (!BlockUtils.isValidSpawnBlock(this.mc.world.getBlockState(pos.down()))) {
            return BlockUtils.MobSpawn.Never;
        }
        if (this.mc.world.getLightLevel(LightType.BLOCK, pos) > threshold) {
            return BlockUtils.MobSpawn.Never;
        }
        if (this.mc.world.getLightLevel(LightType.SKY, pos) > threshold) {
            return BlockUtils.MobSpawn.Potential;
        }
        return BlockUtils.MobSpawn.Always;
    }

    private boolean isValidSpawnBase(BlockPos pos) {
        BlockState state = this.mc.world.getBlockState(pos);
        return state.isSolidBlock((BlockView)this.mc.world, pos);
    }

    private boolean isFullOpaqueCube(BlockState state, BlockPos pos) {
        return state.isSolidBlock((BlockView)this.mc.world, pos) && state.isOpaque() && state.isFullCube((BlockView)this.mc.world, pos);
    }

    private Direction getCameraFacing() {
        Freecam freecam = (Freecam)Modules.get().get(Freecam.class);
        if (freecam != null && freecam.isActive()) {
            return Direction.fromHorizontalDegrees((double)freecam.yaw);
        }
        return this.mc.player.getHorizontalFacing();
    }

    public WWidget getWidget(GuiTheme guiTheme) {
        WVerticalList wVerticalList = guiTheme.verticalList();
        this.fillSphereWidgets(guiTheme, wVerticalList);
        return wVerticalList;
    }

    private void fillSphereWidgets(GuiTheme guiTheme, WVerticalList wVerticalList) {
        WSection wSection = (WSection)wVerticalList.add((WWidget)guiTheme.section("球体渲染 (" + this.sphereConfigs.size() + ")")).expandX().widget();
        WTable wTable = (WTable)wSection.add((WWidget)guiTheme.table()).expandX().widget();
        for (SphereConfig config : this.sphereConfigs) {
            WCheckbox wCheckbox = (WCheckbox)wTable.add((WWidget)guiTheme.checkbox(config.enabled.get())).widget();
            wCheckbox.action = () -> config.enabled.set(wCheckbox.checked);
            String labelText = config.name.get() + " 半径=" + String.valueOf(config.radius.get());
            wTable.add((WWidget)guiTheme.label(labelText)).expandX().widget();
            WButton editButton = (WButton)wTable.add((WWidget)guiTheme.button(GuiRenderer.EDIT)).widget();
            editButton.action = () -> this.mc.setScreen(new SphereEditScreen(guiTheme, config));
            WButton centerButton = (WButton)wTable.add((WWidget)guiTheme.button("定位到玩家")).widget();
            centerButton.action = () -> {
                Vec3d cameraPos = HePosUtils.getCameraPos();
                config.center.set(BlockPos.ofFloored(cameraPos));
            };
            WMinus removeButton = (WMinus)wTable.add((WWidget)guiTheme.minus()).widget();
            removeButton.action = () -> {
                this.sphereConfigs.remove(config);
                this.spawnableBlocksMap.remove(config);
                this.scanCooldownMap.remove(config);
                wVerticalList.clear();
                this.fillSphereWidgets(guiTheme, wVerticalList);
            };
            wTable.row();
        }
        WTable addTable = (WTable)wVerticalList.add((WWidget)guiTheme.table()).expandX().widget();
        WButton addButton = (WButton)addTable.add((WWidget)guiTheme.button("添加球体")).expandX().widget();
        addButton.action = () -> {
            Vec3d cameraPos = HePosUtils.getCameraPos();
            BlockPos playerPos = BlockPos.ofFloored(cameraPos);
            this.sphereConfigs.add(new SphereConfig("球体" + (this.sphereConfigs.size() + 1), playerPos));
            wVerticalList.clear();
            this.fillSphereWidgets(guiTheme, wVerticalList);
        };
        addTable.row();
    }

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        NbtList sphereList = new NbtList();
        for (SphereConfig config : this.sphereConfigs) {
            sphereList.add(config.settings.toTag());
        }
        tag.put("spheres", sphereList);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);
        this.sphereConfigs.clear();
        this.spawnableBlocksMap.clear();
        this.scanCooldownMap.clear();
        if (tag.contains("spheres")) {
            NbtList sphereList = tag.getListOrEmpty("spheres");
            for (NbtElement element : sphereList) {
                if (element.getType() != 10) continue;
                SphereConfig config = new SphereConfig("", BlockPos.ORIGIN);
                config.settings.fromTag((NbtCompound)element);
                this.sphereConfigs.add(config);
            }
        }
        return this;
    }

    private void onLightOverlayChanged(boolean enabled) {
        if (!enabled) {
            this.markerPool.freeAll(this.markers);
            this.markers.clear();
        }
    }

    public void onDeactivate() {
        super.onDeactivate();
        this.onLightOverlayChanged(false);
        this.spawnableBlocksMap.clear();
        this.scanCooldownMap.clear();
    }

    public /* synthetic */ Object a(NbtCompound tag) {
        return this.fromTag(tag);
    }

    static {
        for (int i = 0; i <= 9; ++i) {
            boolean[] segments = DIGIT_SEGMENTS[i];
            double[][] lines = SEGMENT_LINES[i];
            if (segments[0]) {
                lines[0][0] = 0.0;
                lines[0][1] = 0.0;
                lines[0][2] = 1.0;
                lines[0][3] = 0.0;
            }
            if (segments[1]) {
                lines[1][0] = 0.0;
                lines[1][1] = 0.0;
                lines[1][2] = 0.0;
                lines[1][3] = 1.0;
            }
            if (segments[2]) {
                lines[2][0] = 1.0;
                lines[2][1] = 0.0;
                lines[2][2] = 1.0;
                lines[2][3] = 1.0;
            }
            if (segments[3]) {
                lines[3][0] = 0.0;
                lines[3][1] = 1.0;
                lines[3][2] = 1.0;
                lines[3][3] = 1.0;
            }
            if (segments[4]) {
                lines[4][0] = 0.0;
                lines[4][1] = 1.0;
                lines[4][2] = 0.0;
                lines[4][3] = 2.0;
            }
            if (segments[5]) {
                lines[5][0] = 1.0;
                lines[5][1] = 1.0;
                lines[5][2] = 1.0;
                lines[5][3] = 2.0;
            }
            if (!segments[6]) continue;
            lines[6][0] = 0.0;
            lines[6][1] = 2.0;
            lines[6][2] = 1.0;
            lines[6][3] = 2.0;
        }
    }

    static class RedstoneAssistDirectionSwitchMap {
        static final /* synthetic */ int[] a;

        static {
            a = new int[Direction.values().length];
            try {
                RedstoneAssistDirectionSwitchMap.a[Direction.NORTH.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                RedstoneAssistDirectionSwitchMap.a[Direction.SOUTH.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                RedstoneAssistDirectionSwitchMap.a[Direction.WEST.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                RedstoneAssistDirectionSwitchMap.a[Direction.EAST.ordinal()] = 4;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
        }
    }
}
