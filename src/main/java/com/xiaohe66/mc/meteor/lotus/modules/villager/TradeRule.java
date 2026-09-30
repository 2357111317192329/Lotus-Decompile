/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.modules.villager;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class TradeRule {
    public enum Type {
        BUY("买"),
        SELL("卖"),
        ENCHANTED_BOOK("附魔书");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    private Type type;
    private Item item;
    private ResourceKey<Enchantment> enchantment;
    private int priceLimit;

    public TradeRule(Type type, Item item, ResourceKey<Enchantment> enchantment, int priceLimit) {
        this.type = type;
        this.item = item;
        this.enchantment = enchantment;
        this.priceLimit = priceLimit;
    }

    public Type getType() {
        return this.type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Item getItem() {
        return this.item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ResourceKey<Enchantment> getEnchantment() {
        return this.enchantment;
    }

    public void setEnchantment(ResourceKey<Enchantment> enchantment) {
        this.enchantment = enchantment;
    }

    public int getPriceLimit() {
        return this.priceLimit;
    }

    public void setPriceLimit(int priceLimit) {
        this.priceLimit = priceLimit;
    }

    public boolean isUsable() {
        if (this.type == Type.ENCHANTED_BOOK) {
            return this.enchantment != null;
        }
        return this.item != null && this.item != net.minecraft.world.item.Items.AIR;
    }
}
