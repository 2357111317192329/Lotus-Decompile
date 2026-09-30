/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.modules.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class TradeRuleListSetting extends Setting<List<TradeRule>> {
    public TradeRuleListSetting(String name, String description, List<TradeRule> defaultValue, Consumer<List<TradeRule>> onChanged, Consumer<Setting<List<TradeRule>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected List<TradeRule> parseImpl(String str) {
        List<TradeRule> list = new ArrayList<TradeRule>();
        try {
            for (String part : str.split(",")) {
                String[] seg = part.trim().split("\\|");
                TradeRule.Type type = TradeRule.Type.valueOf(seg[0]);
                int price = Integer.parseInt(seg[2]);
                if (type == TradeRule.Type.ENCHANTED_BOOK) {
                    list.add(new TradeRule(type, Items.AIR, ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(seg[1])), price));
                } else {
                    list.add(new TradeRule(type, BuiltInRegistries.ITEM.getValue(Identifier.parse(seg[1])), null, price));
                }
            }
        }
        catch (Exception exception) {
            // ignore
        }
        return list;
    }

    @Override
    protected boolean isValueValid(List<TradeRule> value) {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (TradeRule rule : get()) {
            CompoundTag ruleTag = new CompoundTag();
            ruleTag.putString("type", rule.getType().name());
            ruleTag.putString("item", BuiltInRegistries.ITEM.getKey(rule.getItem()).toString());
            ruleTag.putString("enchantment", rule.getEnchantment() != null ? rule.getEnchantment().identifier().toString() : "");
            ruleTag.putInt("price", rule.getPriceLimit());
            valueTag.add(ruleTag);
        }
        tag.put("value", valueTag);
        return tag;
    }

    @Override
    public List<TradeRule> load(CompoundTag tag) {
        get().clear();
        ListTag valueTag = tag.getListOrEmpty("value");
        for (Tag tagI : valueTag) {
            CompoundTag ruleTag = (CompoundTag)tagI;
            TradeRule.Type type = TradeRule.Type.valueOf(ruleTag.getStringOr("type", TradeRule.Type.BUY.name()));
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(ruleTag.getStringOr("item", "minecraft:air")));
            String enchStr = ruleTag.getStringOr("enchantment", "");
            ResourceKey<Enchantment> enchantment = enchStr.isEmpty() ? null : ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(enchStr));
            get().add(new TradeRule(type, item, enchantment, ruleTag.getIntOr("price", 1)));
        }
        return get();
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<TradeRule>(defaultValue);
    }

    public static class Builder extends Setting.SettingBuilder<Builder, List<TradeRule>, TradeRuleListSetting> {
        public Builder() {
            super(new ArrayList<TradeRule>(0));
        }

        @Override
        public TradeRuleListSetting build() {
            return new TradeRuleListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
