package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentBookNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentEquipNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentMargeNode;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class EnchantmentUtils {
   public static final Minecraft mc = Minecraft.getInstance();

   private EnchantmentUtils() {
   }

   public static Set<ResourceKey<Enchantment>> getEnchantment(ItemStack itemStack) {
      return getEnchantment(itemStack, false);
   }

   public static Set<ResourceKey<Enchantment>> getEnchantment(ItemStack itemStack, boolean isMaxLevel) {
      ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);
      if (enchantments.isEmpty()) {
         return Collections.emptySet();
      }

      Set<ResourceKey<Enchantment>> list = new HashSet<>(enchantments.size());

      for (Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
         Holder<Enchantment> registryEntry = (Holder<Enchantment>)entry.getKey();
         Optional<ResourceKey<Enchantment>> enchantmentKeyOptional = registryEntry.unwrapKey();
         if (enchantmentKeyOptional.isPresent() && (!isMaxLevel || entry.getIntValue() >= ((Enchantment)registryEntry.value()).getMaxLevel())) {
            list.add(enchantmentKeyOptional.get());
         }
      }

      return list;
   }

   public static ResourceKey<Enchantment> getEnchantmentOne(ItemStack itemStack) {
      ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);

      for (Entry<Holder<Enchantment>> enchantmentEntry : enchantments.entrySet()) {
         Holder<Enchantment> registryEntry = (Holder<Enchantment>)enchantmentEntry.getKey();
         Optional<ResourceKey<Enchantment>> enchantmentKeyOptional = registryEntry.unwrapKey();
         if (enchantmentKeyOptional.isPresent()) {
            ResourceKey<Enchantment> enchantmentRegistryKey = enchantmentKeyOptional.get();
            Enchantment enchantment = (Enchantment)registryEntry.value();
            if (enchantmentEntry.getIntValue() == enchantment.getMaxLevel()) {
               return enchantmentRegistryKey;
            }
         }
      }

      return null;
   }

   public static boolean existAll(List<ResourceKey<Enchantment>> needEnchantmentList, ItemStack itemStack) {
      Set<ResourceKey<Enchantment>> existSet = getEnchantment(itemStack);
      return existAll(needEnchantmentList, existSet);
   }

   public static boolean existAll(List<ResourceKey<Enchantment>> needEnchantmentList, Set<ResourceKey<Enchantment>> checkSet) {
      for (ResourceKey<Enchantment> registryKey : needEnchantmentList) {
         if (!checkSet.contains(registryKey)) {
            return false;
         }
      }

      return true;
   }

   public static EnchantmentMargeNode bestStepSimple(ItemStack equipItemStack, Set<ResourceKey<Enchantment>> missEnchantmentSet) {
      EnchantmentNode node = bestStep(equipItemStack, missEnchantmentSet);

      while (node instanceof EnchantmentMargeNode) {
         EnchantmentMargeNode margeNode = (EnchantmentMargeNode)node;
         if (margeNode.getLeft() instanceof EnchantmentEquipNode equipNode) {
            return margeNode;
         }

         node = margeNode.getLeft();
      }

      throw new IllegalStateException("不应该运行到这里");
   }

   public static EnchantmentNode bestStep(ItemStack equipItemStack, Set<ResourceKey<Enchantment>> missEnchantmentSet) {
      EnchantmentEquipNode equipNode = new EnchantmentEquipNode(equipItemStack);
      Set<EnchantmentNode> bookNodeSet = missEnchantmentSet.stream().map(EnchantmentBookNode::new).collect(Collectors.toSet());
      return bestStep(equipNode, bookNodeSet);
   }

   public static EnchantmentNode bestStep(EnchantmentNode originEquipNode, Set<EnchantmentNode> bookNodeSet) {
      return bestStep(originEquipNode, bookNodeSet, false);
   }

   public static EnchantmentNode bestStep(EnchantmentNode originEquipNode, Set<EnchantmentNode> bookNodeSet, boolean fast) {
      if (bookNodeSet.isEmpty()) {
         return null;
      }

      List<EnchantmentNode> bookList = new LinkedList<>(bookNodeSet);
      bookList.sort(null);
      Map<Integer, List<EnchantmentNode>> groups = new HashMap<>();
      groups.put(0, bookList);
      int sup = bookList.size();
      EnchantmentNode equipNode = originEquipNode;

      while (sup > 0) {
         List<EnchantmentNode> group = groups.computeIfAbsent(equipNode.getRepairCost(), k -> new LinkedList<>());
         if (!group.isEmpty()) {
            EnchantmentNode right = group.removeFirst();
            equipNode = new EnchantmentMargeNode(equipNode, right);
            sup--;
            if (fast) {
               return equipNode;
            }
         } else {
            int margeQty = 0;

            for (int i = 0; i < equipNode.getRepairCost(); i++) {
               List<EnchantmentNode> lessGroup = groups.computeIfAbsent(i, k -> new LinkedList<>());
               List<EnchantmentNode> nextGroup = groups.computeIfAbsent(equipNode.getRepairCost(), k -> new LinkedList<>());

               while (lessGroup.size() >= 2) {
                  EnchantmentNode left = lessGroup.removeFirst();
                  EnchantmentNode right = lessGroup.removeLast();
                  EnchantmentNode newNode = new EnchantmentMargeNode(left, right);
                  margeQty++;
                  nextGroup.add(newNode);
               }

               nextGroup.sort(null);
            }

            if (margeQty <= 0) {
               break;
            }

            sup -= margeQty;
         }
      }

      int repairCost = 0;
      EnchantmentNode right = null;

      while (sup > 1 && repairCost < 10) {
         List<EnchantmentNode> group = groups.get(repairCost);

         while (!group.isEmpty()) {
            if (right == null) {
               right = group.removeLast();
            } else {
               EnchantmentNode left = group.removeFirst();
               right = new EnchantmentMargeNode(left, right);
               sup--;
            }
         }

         repairCost++;
      }

      if (sup > 0) {
         if (right == null) {
            for (java.util.Map.Entry<Integer, List<EnchantmentNode>> entry : groups.entrySet()) {
               if (!entry.getValue().isEmpty()) {
                  right = entry.getValue().removeFirst();
                  break;
               }
            }

            if (right == null) {
               throw new IllegalStateException("未知状态");
            }
         }

         equipNode = new EnchantmentMargeNode(equipNode, right);
      }

      return equipNode;
   }

   public static int getRepairCost(ItemStack itemStack) {
      return (Integer)itemStack.getOrDefault(DataComponents.REPAIR_COST, 0);
   }

   public static int getAnvilCost(ResourceKey<Enchantment> registryKey) {
      return getEnchantmentInstance(registryKey).<Integer>map(Enchantment::getAnvilCost).orElseThrow();
   }

   public static int getMaxLevel(ResourceKey<Enchantment> registryKey) {
      return getEnchantmentInstance(registryKey).<Integer>map(Enchantment::getMaxLevel).orElseThrow();
   }

   public static int getCost(ResourceKey<Enchantment> registryKey) {
      Enchantment enchantment = getEnchantmentInstance(registryKey).orElseThrow();
      return Math.max(1, enchantment.getAnvilCost() / 2) * enchantment.getMaxLevel();
   }

   public static int getCost(Set<ResourceKey<Enchantment>> enchantments) {
      int total = 0;

      for (ResourceKey<Enchantment> enchantment : enchantments) {
         total += getCost(enchantment);
      }

      return total;
   }

   public static Optional<Enchantment> getEnchantmentInstance(ResourceKey<Enchantment> registryKey) {
      return mc.level.registryAccess().lookup(Registries.ENCHANTMENT).map(item -> (Enchantment)item.getValue(registryKey));
   }

   public static int penaltyToWork(int penalty) {
      return penalty <= 0 ? 0 : 32 - Integer.numberOfLeadingZeros(penalty);
   }

   public static int workToPenalty(int work) {
      return work <= 0 ? 0 : (1 << work) - 1;
   }
}
