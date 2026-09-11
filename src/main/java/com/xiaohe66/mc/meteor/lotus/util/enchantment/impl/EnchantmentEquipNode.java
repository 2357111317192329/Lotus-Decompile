/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  net.minecraft.item.ItemStack
 *  net.minecraft.component.DataComponentTypes
 */
package com.xiaohe66.mc.meteor.lotus.util.enchantment.impl;

import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;

public class EnchantmentEquipNode
implements EnchantmentNode {
    private final ItemStack itemStack;

    public EnchantmentEquipNode(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public int getRepairCost() {
        return (Integer)this.itemStack.getOrDefault(DataComponentTypes.REPAIR_COST, 0);
    }

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public int getCostSum() {
        return 0;
    }

    public String toString() {
        return Names.get(this.itemStack) + "(" + this.getRepairCost() + ")";
    }
}
