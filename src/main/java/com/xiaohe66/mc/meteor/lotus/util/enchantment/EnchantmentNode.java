package com.xiaohe66.mc.meteor.lotus.util.enchantment;

import org.jetbrains.annotations.NotNull;

public interface EnchantmentNode extends Comparable<EnchantmentNode> {
   int getRepairCost();

   int getCost();

   int getCostSum();

   default int compareTo(@NotNull EnchantmentNode node) {
      return Integer.compare(node.getCost(), this.getCost());
   }
}
