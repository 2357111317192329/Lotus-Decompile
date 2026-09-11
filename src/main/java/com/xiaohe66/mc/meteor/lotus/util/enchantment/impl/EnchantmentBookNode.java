/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.registry.RegistryKey
 */
package com.xiaohe66.mc.meteor.lotus.util.enchantment.impl;

import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentBookNode
implements EnchantmentNode {
    private final ResourceKey<Enchantment> registryKey;
    private final int cost;

    public EnchantmentBookNode(ResourceKey<Enchantment> registryKey) {
        this.registryKey = registryKey;
        this.cost = EnchantmentUtils.getCost(registryKey);
    }

    @Override
    public int getRepairCost() {
        return 0;
    }

    @Override
    public int getCost() {
        return this.cost;
    }

    @Override
    public int getCostSum() {
        return 0;
    }

    public ResourceKey<Enchantment> getEnchantmentKey() {
        return this.registryKey;
    }

    public String toString() {
        return Names.get(this.registryKey);
    }
}
