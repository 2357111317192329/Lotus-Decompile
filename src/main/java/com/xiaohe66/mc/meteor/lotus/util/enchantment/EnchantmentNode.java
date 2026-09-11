/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package com.xiaohe66.mc.meteor.lotus.util.enchantment;

import org.jetbrains.annotations.NotNull;

public interface EnchantmentNode
extends Comparable<EnchantmentNode> {
    public int getRepairCost();

    public int getCost();

    public int getCostSum();

    @Override
    default public int compareTo(@NotNull EnchantmentNode node) {
        return Integer.compare(node.getCost(), this.getCost());
    }
}
