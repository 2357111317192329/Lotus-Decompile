/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.Settings
 *  net.minecraft.item.Item
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.registry.RegistryKey
 */
package com.xiaohe66.mc.meteor.lotus.modules.villager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class VillagerSettingWarp {
    private final Setting<Boolean> openSetting;
    private final SettingGroup itemGroup;
    private final Setting<VillagerType> typeSetting;
    private final Setting<List<TradeRule>> tradeRuleSetting;
    private List<String> lastTypeSignature;

    public VillagerSettingWarp(Settings settings, SettingGroup settingGroup, String name, VillagerType defaultType, Consumer<Boolean> onChange) {
        this.openSetting = settingGroup.add(new BoolSetting.Builder()
            .name("交易_" + name)
            .defaultValue(false)
            .onChanged(v -> onChange.accept(true))
            .build());
        this.itemGroup = settings.createGroup("交易_" + name);
        this.typeSetting = this.itemGroup.add(new EnumSetting.Builder<VillagerType>()
            .name(name + "_村民类型")
            .defaultValue(defaultType)
            .visible(() -> this.openSetting.get() != false && defaultType == VillagerType.石匠)
            .onChanged(v -> onChange.accept(true))
            .build());
        this.tradeRuleSetting = this.itemGroup.add(new TradeRuleListSetting.Builder()
            .name(name + "_交易对")
            .description("配置交易对: 型别(买/卖/附魔书), 物品或附魔, 价格上限。买/附魔书价格上限为买一个所需的绿宝石数, 卖价格上限为换1个绿宝石所需的物品数")
            .visible(() -> this.openSetting.get())
            .onChanged(v -> this.onTradeRulesChanged(onChange))
            .build());
        this.lastTypeSignature = this.getTypeSignature();
    }

    private void onTradeRulesChanged(Consumer<Boolean> onChange) {
        List<String> current = this.getTypeSignature();
        boolean typeChanged = !current.equals(this.lastTypeSignature);
        this.lastTypeSignature = current;
        onChange.accept(typeChanged);
    }

    private List<String> getTypeSignature() {
        List<String> list = new ArrayList<String>();
        for (TradeRule rule : this.tradeRuleSetting.get()) {
            list.add(rule.getType().name() + (rule.isUsable() ? ":1" : ":0"));
        }
        return list;
    }

    public boolean isOpen() {
        return this.openSetting.get();
    }

    public VillagerType getType() {
        return this.typeSetting.get();
    }

    public List<Item> getBuyItem() {
        List<Item> list = new ArrayList<Item>();
        for (TradeRule rule : this.tradeRuleSetting.get()) {
            if (rule.getType() != TradeRule.Type.BUY || !rule.isUsable()) continue;
            list.add(rule.getItem());
        }
        return list;
    }

    public List<Item> getSellItem() {
        List<Item> list = new ArrayList<Item>();
        for (TradeRule rule : this.tradeRuleSetting.get()) {
            if (rule.getType() != TradeRule.Type.SELL || !rule.isUsable()) continue;
            list.add(rule.getItem());
        }
        return list;
    }

    public Set<ResourceKey<Enchantment>> getBuyEnchantment() {
        if (this.getType() != VillagerType.图书管理员) {
            return Collections.emptySet();
        }
        Set<ResourceKey<Enchantment>> set = new HashSet<ResourceKey<Enchantment>>();
        for (TradeRule rule : this.tradeRuleSetting.get()) {
            if (rule.getType() != TradeRule.Type.ENCHANTED_BOOK || !rule.isUsable()) continue;
            set.add(rule.getEnchantment());
        }
        return set;
    }

    public int getBuyPriceLimit(Item item) {
        for (TradeRule rule : this.tradeRuleSetting.get()) {
            if (rule.getType() == TradeRule.Type.BUY && rule.getItem() == item) {
                return rule.getPriceLimit();
            }
        }
        return 0;
    }

    public int getSellPriceLimit(Item item) {
        for (TradeRule rule : this.tradeRuleSetting.get()) {
            if (rule.getType() == TradeRule.Type.SELL && rule.getItem() == item) {
                return rule.getPriceLimit();
            }
        }
        return 0;
    }

    public int getEnchantmentPriceLimit(ResourceKey<Enchantment> enchantment) {
        for (TradeRule rule : this.tradeRuleSetting.get()) {
            if (rule.getType() == TradeRule.Type.ENCHANTED_BOOK && rule.getEnchantment() == enchantment) {
                return rule.getPriceLimit();
            }
        }
        return 0;
    }
}
