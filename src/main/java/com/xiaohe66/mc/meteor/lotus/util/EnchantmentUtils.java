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
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.DataComponentTypes;

public class EnchantmentUtils {
   public static final MinecraftClient mc = MinecraftClient.getInstance();

   private EnchantmentUtils() {
   }

   public static Set<RegistryKey<Enchantment>> getEnchantment(ItemStack itemStack) {
      return getEnchantment(itemStack, false);
   }

   public static Set<RegistryKey<Enchantment>> getEnchantment(ItemStack itemStack, boolean isMaxLevel) {
      ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(itemStack);
      if (enchantments.isEmpty()) {
         return Collections.emptySet();
      }

      Set<RegistryKey<Enchantment>> list = new HashSet<>(enchantments.getSize());

      for (Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
         RegistryEntry<Enchantment> registryEntry = (RegistryEntry<Enchantment>)entry.getKey();
         Optional<RegistryKey<Enchantment>> enchantmentKeyOptional = registryEntry.getKey();
         if (enchantmentKeyOptional.isPresent() && (!isMaxLevel || entry.getIntValue() >= ((Enchantment)registryEntry.value()).getMaxLevel())) {
            list.add(enchantmentKeyOptional.get());
         }
      }

      return list;
   }

   public static RegistryKey<Enchantment> getEnchantmentOne(ItemStack itemStack) {
      ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(itemStack);

      for (Entry<RegistryEntry<Enchantment>> enchantmentEntry : enchantments.getEnchantmentEntries()) {
         RegistryEntry<Enchantment> registryEntry = (RegistryEntry<Enchantment>)enchantmentEntry.getKey();
         Optional<RegistryKey<Enchantment>> enchantmentKeyOptional = registryEntry.getKey();
         if (enchantmentKeyOptional.isPresent()) {
            RegistryKey<Enchantment> enchantmentRegistryKey = enchantmentKeyOptional.get();
            Enchantment enchantment = (Enchantment)registryEntry.value();
            if (enchantmentEntry.getIntValue() == enchantment.getMaxLevel()) {
               return enchantmentRegistryKey;
            }
         }
      }

      return null;
   }

   public static boolean existAll(List<RegistryKey<Enchantment>> needEnchantmentList, ItemStack itemStack) {
      Set<RegistryKey<Enchantment>> existSet = getEnchantment(itemStack);
      return existAll(needEnchantmentList, existSet);
   }

   public static boolean existAll(List<RegistryKey<Enchantment>> needEnchantmentList, Set<RegistryKey<Enchantment>> checkSet) {
      for (RegistryKey<Enchantment> registryKey : needEnchantmentList) {
         if (!checkSet.contains(registryKey)) {
            return false;
         }
      }

      return true;
   }

   public static EnchantmentMargeNode bestStepSimple(ItemStack equipItemStack, Set<RegistryKey<Enchantment>> missEnchantmentSet) {
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

   public static EnchantmentNode bestStep(ItemStack equipItemStack, Set<RegistryKey<Enchantment>> missEnchantmentSet) {
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
      return (Integer)itemStack.getOrDefault(DataComponentTypes.REPAIR_COST, 0);
   }

   public static int getAnvilCost(RegistryKey<Enchantment> registryKey) {
      return getEnchantmentInstance(registryKey).<Integer>map(Enchantment::getAnvilCost).orElseThrow();
   }

   public static int getMaxLevel(RegistryKey<Enchantment> registryKey) {
      return getEnchantmentInstance(registryKey).<Integer>map(Enchantment::getMaxLevel).orElseThrow();
   }

   public static int getCost(RegistryKey<Enchantment> registryKey) {
      Enchantment enchantment = getEnchantmentInstance(registryKey).orElseThrow();
      return Math.max(1, enchantment.getAnvilCost() / 2) * enchantment.getMaxLevel();
   }

   public static int getCost(Set<RegistryKey<Enchantment>> enchantments) {
      int total = 0;

      for (RegistryKey<Enchantment> enchantment : enchantments) {
         total += getCost(enchantment);
      }

      return total;
   }

   public static Optional<Enchantment> getEnchantmentInstance(RegistryKey<Enchantment> registryKey) {
      return mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT).map(item -> (Enchantment)item.get(registryKey));
   }

   public static int penaltyToWork(int penalty) {
      return penalty <= 0 ? 0 : 32 - Integer.numberOfLeadingZeros(penalty);
   }

   public static int workToPenalty(int work) {
      return work <= 0 ? 0 : (1 << work) - 1;
   }
}
