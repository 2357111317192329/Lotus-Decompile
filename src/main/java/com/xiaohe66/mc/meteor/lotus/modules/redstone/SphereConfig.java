/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.BlockPosSetting$Builder
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.ColorSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.Settings
 *  meteordevelopment.meteorclient.settings.StringSetting$Builder
 *  meteordevelopment.meteorclient.utils.render.color.SettingColor
 *  net.minecraft.util.math.BlockPos
 */
package com.xiaohe66.mc.meteor.lotus.modules.redstone;


import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.core.BlockPos;

public class SphereConfig {
    public final Settings settings = new Settings();
    public final Setting<String> name;
    public final Setting<Boolean> enabled;
    public final Setting<BlockPos> center;
    public final Setting<Integer> radius;
    public final Setting<Integer> segments;
    public final Setting<SettingColor> wireframeColor;
    public final Setting<Boolean> showSpawnableBlocks;
    public final Setting<Integer> scanInterval;
    public final Setting<SettingColor> spawnableBlockColor;
    public final Setting<Integer> renderLimit;
    public final Setting<Boolean> seeThrough;
    public final Setting<Boolean> includeSmallMobs;

    public SphereConfig(String name, BlockPos center) {
        SettingGroup settingGroup = this.settings.getDefaultGroup();
        this.name = settingGroup.add(new StringSetting.Builder()
            .name("名称")
            .description("球体名称")
            .defaultValue(name)
            .build());
        this.enabled = settingGroup.add(new BoolSetting.Builder()
            .name("启用")
            .description("是否渲染此球体")
            .defaultValue(true)
            .build());
        this.center = settingGroup.add(new BlockPosSetting.Builder()
            .name("球心坐标")
            .description("球体中心位置")
            .defaultValue(center)
            .build());
        this.radius = settingGroup.add(new IntSetting.Builder()
            .name("半径")
            .description("球体半径(格)")
            .defaultValue(128)
            .min(1)
            .sliderMax(128)
            .build());
        this.segments = settingGroup.add(new IntSetting.Builder()
            .name("分段数")
            .description("球体经纬线的分段数量，数值越大线框越平滑")
            .defaultValue(128)
            .min(4)
            .sliderMax(256)
            .build());
        this.wireframeColor = settingGroup.add(new ColorSetting.Builder()
            .name("线框颜色")
            .description("球体线框的颜色")
            .defaultValue(new SettingColor(51, 153, 255))
            .build());
        this.showSpawnableBlocks = settingGroup.add(new BoolSetting.Builder()
            .name("显示可刷怪方块")
            .description("在球体内显示可刷怪方块的高亮")
            .defaultValue(false)
            .build());
        this.scanInterval = settingGroup.add(new IntSetting.Builder()
            .name("扫描间隔")
            .description("扫描可刷怪方块的间隔（tick），20 tick = 1秒")
            .defaultValue(40)
            .min(1)
            .sliderMax(100)
            .build());
        this.spawnableBlockColor = settingGroup.add(new ColorSetting.Builder()
            .name("可刷怪方块颜色")
            .description("可刷怪方块高亮的渲染颜色")
            .defaultValue(new SettingColor(255, 0, 255, 100))
            .build());
        this.renderLimit = settingGroup.add(new IntSetting.Builder()
            .name("渲染上限")
            .description("最多渲染的可刷怪方块数量，优先渲染离玩家近的")
            .defaultValue(6666)
            .min(1)
            .sliderMax(66666)
            .build());
        this.seeThrough = settingGroup.add(new BoolSetting.Builder()
            .name("透视渲染")
            .description("可刷怪方块高亮是否透视显示")
            .defaultValue(true)
            .build());
        this.includeSmallMobs = settingGroup.add(new BoolSetting.Builder()
            .name("含矮小生物")
            .description("对仅有1格净空的位置(蜘蛛/洞穴蜘蛛/蠹虫/末影螨)也显示为可刷怪方块")
            .defaultValue(false)
            .build());
    }
}
