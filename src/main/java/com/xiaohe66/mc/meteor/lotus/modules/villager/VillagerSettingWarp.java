/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnchantmentListSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.ItemListSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.Settings
 *  net.minecraft.item.Item
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.registry.RegistryKey
 */
package com.xiaohe66.mc.meteor.lotus.modules.villager;

import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerType;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;

public class VillagerSettingWarp {
    private final Setting<Boolean> openSetting;
    private final SettingGroup itemGroup;
    private final Setting<VillagerType> typeSetting;
    private final Setting<List<Item>> buyItemSetting;
    private final Setting<Integer> supplyQty;
    private final Setting<Integer> maxMoenySetting;
    private final Setting<Set<RegistryKey<Enchantment>>> buyEnchantmentSetting;

    public VillagerSettingWarp(Settings settings, SettingGroup settingGroup, String name, VillagerType defaultType, int defaultSupplyQty) {
        this.openSetting = settingGroup.add(new BoolSetting.Builder()
            .name("交易_" + name)
            .defaultValue(false)
            .build());
        this.itemGroup = settings.createGroup("交易_" + name);
        this.typeSetting = this.itemGroup.add(new EnumSetting.Builder<VillagerType>()
            .name(name + "_村民类型")
            .defaultValue(defaultType)
            .visible(() -> this.openSetting.get() != false && defaultType == VillagerType.石匠)
            .build());
        this.buyItemSetting = this.itemGroup.add(new ItemListSetting.Builder()
            .name(name + "_交易")
            .filter(item -> this.typeSetting.get().getCanVillagerItemSet().contains(item))
            .defaultValue(defaultType.getDefaultVillagerItemList().toArray(new Item[0]))
            .visible(() -> this.openSetting.get())
            .build());
        this.buyEnchantmentSetting = this.itemGroup.add(new EnchantmentListSetting.Builder()
            .name(name + "_附魔书")
            .visible(() -> this.openSetting.get() != false && this.typeSetting.get() == VillagerType.图书管理员)
            .build());
        this.supplyQty = this.itemGroup.add(new IntSetting.Builder()
            .name(name + "_补给数量(组)")
            .description("每次补给的数量")
            .range(1, 27)
            .sliderMax(27)
            .defaultValue(defaultSupplyQty)
            .visible(() -> this.openSetting.get())
            .build());
        this.maxMoenySetting = this.itemGroup.add(new IntSetting.Builder()
            .name(name + "_价格上限")
            .range(1, 64)
            .sliderRange(1, 64)
            .defaultValue(1)
            .visible(() -> this.openSetting.get())
            .build());
    }

    public VillagerSettingWarp(SettingGroup itemGroup, Setting<Boolean> openSetting, Setting<VillagerType> typeSetting, Setting<List<Item>> buyItemSetting, Setting<Integer> supplyQty, Setting<Integer> maxMoenySetting, Setting<Set<RegistryKey<Enchantment>>> buyEnchantmentSetting) {
        this.itemGroup = itemGroup;
        this.openSetting = openSetting;
        this.typeSetting = typeSetting;
        this.buyItemSetting = buyItemSetting;
        this.supplyQty = supplyQty;
        this.maxMoenySetting = maxMoenySetting;
        this.buyEnchantmentSetting = buyEnchantmentSetting;
    }

    public boolean needBuyEnchantment() {
        return this.isOpen() && !this.getBuyEnchantment().isEmpty();
    }

    public boolean isOpen() {
        return this.openSetting.get();
    }

    public VillagerType getType() {
        return this.typeSetting.get();
    }

    public List<Item> getBuyItem() {
        return this.buyItemSetting.get();
    }

    public int getSupplyQty() {
        return this.supplyQty.get();
    }

    public int getMaxMoney() {
        return this.maxMoenySetting.get();
    }

    public Set<RegistryKey<Enchantment>> getBuyEnchantment() {
        if (this.getType() == VillagerType.图书管理员) {
            return this.buyEnchantmentSetting.get();
        }
        return Collections.emptySet();
    }
}
