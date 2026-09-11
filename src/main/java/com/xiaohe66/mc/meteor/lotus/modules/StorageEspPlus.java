/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.cache.Cache
 *  com.google.common.cache.CacheBuilder
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.settings.BlockListSetting$Builder
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.item.Item
 *  net.minecraft.item.Items
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.Block
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.block.entity.BlockEntity
 *  net.minecraft.block.BlockState
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xiaohe66.mc.meteor.lotus.event.StorageEspLineEvent;
import com.xiaohe66.mc.meteor.lotus.event.StorageFinderEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;

public class StorageEspPlus extends Module {
    private static final Cache<String, Boolean> storageStatusCache = CacheBuilder.newBuilder().maximumSize(1024L).expireAfterWrite(5L, TimeUnit.SECONDS).expireAfterAccess(5L, TimeUnit.SECONDS).build();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> storageEspEnable = sgGeneral.add(new BoolSetting.Builder()
        .name("存储透视增强")
        .description("减少存储透视的连接线")
        .defaultValue(true)
        .build());
    public final Setting<Boolean> stashFinderEnable = sgGeneral.add(new BoolSetting.Builder()
        .name("存储查找器增强")
        .description("减少存储查找器的提示")
        .defaultValue(true)
        .build());
    private final Setting<List<Block>> excludeBlock = sgGeneral.add(new BlockListSetting.Builder()
        .name("排除方块")
        .description("附近有指定方块时不显示连接线")
        .defaultValue(new Block[]{Blocks.WAXED_COPPER_BLOCK, Blocks.WAXED_OXIDIZED_COPPER, Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.NETHER_BRICKS, Blocks.BLACKSTONE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS})
        .build());
    private final Setting<Integer> excludeRange = sgGeneral.add(new IntSetting.Builder()
        .name("排除范围")
        .description("排除范围")
        .min(1)
        .sliderMax(5)
        .defaultValue(3)
        .build());
    private final Setting<Integer> excludeValue = sgGeneral.add(new IntSetting.Builder()
        .name("排除阈值")
        .description("达到阈值后不显示连接线")
        .min(1)
        .sliderMax(10)
        .defaultValue(5)
        .build());
    private final Setting<Integer> storageQty = sgGeneral.add(new IntSetting.Builder()
        .name("强制容器数")
        .description("当附近的容器达到指定数量时，强制显示连接线")
        .min(1)
        .sliderMax(10)
        .defaultValue(3)
        .build());

    public StorageEspPlus() {
        super(Const.CATEGORY, "存储查找增强", "给[存储透视]和[存储查找器]增加排除遗迹的选项");
    }

    @EventHandler
    private void onEvent(StorageEspLineEvent event) {
        if (this.storageEspEnable.get() && this.testInStructure(event.getBlockEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onEvent(StorageFinderEvent event) {
        if (this.stashFinderEnable.get() && this.testInStructure(event.getBlockEntity())) {
            event.setCancelled(true);
        }
    }

    private boolean testInStructure(BlockEntity blockEntity) {
        BlockPos originPos = blockEntity.getPos();
        String cacheKey = originPos.getX() + "_" + originPos.getY() + "_" + originPos.getZ();
        try {
            return storageStatusCache.get(cacheKey, () -> this.doTestInStructure(blockEntity));
        }
        catch (ExecutionException executionException) {
            executionException.printStackTrace();
            return false;
        }
    }

    private boolean doTestInStructure(BlockEntity blockEntity) {
        int range = this.excludeRange.get() - 1;
        List<Block> excludeBlocks = this.excludeBlock.get();
        Set<Item> excludeItemSet = excludeBlocks.stream().map(Block::asItem).collect(Collectors.toSet());
        int excludeCount = 0;
        int storageCount = 0;
        BlockPos originPos = blockEntity.getPos();
        int minX = originPos.getX() - range;
        int maxX = originPos.getX() + range;
        int minY = originPos.getY() - range;
        int maxY = originPos.getY() + range;
        int minZ = originPos.getZ() - range;
        int maxZ = originPos.getZ() + range;
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState blockState = MeteorClient.mc.world.getBlockState(blockPos);
                    Item item = blockState.getBlock().asItem();
                    if (item == Items.CHEST || item == Items.TRAPPED_CHEST || item == Items.HOPPER) {
                        ++storageCount;
                        continue;
                    }
                    if (!excludeItemSet.contains(item)) continue;
                    ++excludeCount;
                }
            }
        }
        return storageCount < this.storageQty.get() && excludeCount >= this.excludeValue.get();
    }
}

