package com.xiaohe66.mc.meteor.lotus.util.enchantment.impl;

import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentMargeNode implements EnchantmentNode {
   private final EnchantmentNode left;
   private final EnchantmentNode right;
   private final int repairCost;
   private final int cost;
   private int costSum = -1;
   private Set<ResourceKey<Enchantment>> allEnchantmentKey;

   public EnchantmentMargeNode(EnchantmentNode left, EnchantmentNode right) {
      this.left = left;
      this.right = right;
      this.repairCost = Math.max(left.getRepairCost(), right.getRepairCost()) + 1;
      this.cost = left.getCost() + right.getCost();
   }

   @Override
   public int getRepairCost() {
      return this.repairCost;
   }

   @Override
   public int getCost() {
      return this.cost;
   }

   @Override
   public int getCostSum() {
      if (this.costSum < 0) {
         this.costSum = this.left.getRepairCost() + this.left.getCostSum() + this.right.getRepairCost() + this.right.getCostSum() + this.right.getCost();
      }

      return this.costSum;
   }

   public EnchantmentNode getLeft() {
      return this.left;
   }

   public EnchantmentNode getRight() {
      return this.right;
   }

   public Set<ResourceKey<Enchantment>> getAllEnchantmentKey() {
      if (this.allEnchantmentKey == null) {
         Set<ResourceKey<Enchantment>> all = new HashSet<>();
         if (this.left instanceof EnchantmentMargeNode margeNode) {
            all.addAll(margeNode.getAllEnchantmentKey());
         } else if (this.left instanceof EnchantmentBookNode bookNode) {
            all.add(bookNode.getEnchantmentKey());
         }

         if (this.right instanceof EnchantmentMargeNode margeNode) {
            all.addAll(margeNode.getAllEnchantmentKey());
         } else if (this.right instanceof EnchantmentBookNode bookNode) {
            all.add(bookNode.getEnchantmentKey());
         }

         this.allEnchantmentKey = all;
      }

      return this.allEnchantmentKey;
   }

   @Override
   public String toString() {
      String s = "";
      if (this.left instanceof EnchantmentMargeNode) {
         s=s+"("+this.left.toString()+"="+this.left.getRepairCost()+","+this.left.getCost()+","+this.left.getCostSum()+")";
      } else {
         s=s+this.left.toString();
      }
      s=s+"+";
      if (this.right instanceof EnchantmentMargeNode) {
         s=s+"("+this.right.toString()+"="+this.right.getRepairCost()+","+this.right.getCost()+","+this.right.getCostSum()+")";
      } else {
         s=s+this.right.toString()+"("+this.right.getCost()+")";
      }

      return s;
   }
}
