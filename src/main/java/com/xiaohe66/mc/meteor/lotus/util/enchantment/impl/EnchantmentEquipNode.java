package com.xiaohe66.mc.meteor.lotus.util.enchantment.impl;

import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class EnchantmentEquipNode implements EnchantmentNode {
   private final ItemStack itemStack;

   public EnchantmentEquipNode(ItemStack itemStack) {
      this.itemStack = itemStack;
   }

   @Override
   public int getRepairCost() {
      return (Integer)this.itemStack.getOrDefault(DataComponents.REPAIR_COST, 0);
   }

   @Override
   public int getCost() {
      return 0;
   }

   @Override
   public int getCostSum() {
      return 0;
   }

   @Override
   public String toString() {
      return Names.get(this.itemStack) + "(" + this.getRepairCost() + ")";
   }
}
