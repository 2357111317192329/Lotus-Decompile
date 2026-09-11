/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  net.minecraft.item.ItemStack
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.registry.RegistryKey
 *  net.minecraft.registry.entry.RegistryEntry
 *  net.minecraft.registry.RegistryKeys
 *  net.minecraft.component.type.ItemEnchantmentsComponent
 *  net.minecraft.component.DataComponentTypes
 *  */
package com.xiaohe66.mc.meteor.lotus.util;


import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentBookNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentEquipNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentMargeNode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
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
        return EnchantmentUtils.getEnchantment(itemStack, false);
    }

    public static Set<ResourceKey<Enchantment>> getEnchantment(ItemStack itemStack, boolean isMaxLevel) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting((ItemStack)itemStack);
        if (enchantments.isEmpty()) {
            return Collections.emptySet();
        }
        Set<ResourceKey<Enchantment>> list = new HashSet<ResourceKey<Enchantment>>(enchantments.size());
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> registryEntry = (Holder<Enchantment>)entry.getKey();
            Optional<ResourceKey<Enchantment>> enchantmentKeyOptional = registryEntry.unwrapKey();
            if (!enchantmentKeyOptional.isPresent() || isMaxLevel && entry.getIntValue() < ((Enchantment)registryEntry.value()).getMaxLevel()) continue;
            list.add((ResourceKey<Enchantment>)((ResourceKey)enchantmentKeyOptional.get()));
        }
        return list;
    }

    public static ResourceKey<Enchantment> getEnchantmentOne(ItemStack itemStack) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting((ItemStack)itemStack);
        for (Object2IntMap.Entry<Holder<Enchantment>> enchantmentEntry : enchantments.entrySet()) {
            Holder<Enchantment> registryEntry = (Holder<Enchantment>)enchantmentEntry.getKey();
            Optional<ResourceKey<Enchantment>> enchantmentKeyOptional = registryEntry.unwrapKey();
            if (!enchantmentKeyOptional.isPresent()) continue;
            ResourceKey<Enchantment> enchantmentRegistryKey = (ResourceKey<Enchantment>)enchantmentKeyOptional.get();
            Enchantment enchantment = (Enchantment)registryEntry.value();
            if (enchantmentEntry.getIntValue() != enchantment.getMaxLevel()) continue;
            return enchantmentRegistryKey;
        }
        return null;
    }

    public static boolean existAll(List<ResourceKey<Enchantment>> needEnchantmentList, ItemStack itemStack) {
        Set<ResourceKey<Enchantment>> existSet = EnchantmentUtils.getEnchantment(itemStack);
        return EnchantmentUtils.existAll(needEnchantmentList, existSet);
    }

    public static boolean existAll(List<ResourceKey<Enchantment>> needEnchantmentList, Set<ResourceKey<Enchantment>> checkSet) {
        for (ResourceKey<Enchantment> registryKey : needEnchantmentList) {
            if (checkSet.contains(registryKey)) continue;
            return false;
        }
        return true;
    }

    public static EnchantmentMargeNode bestStepSimple(ItemStack equipItemStack, Set<ResourceKey<Enchantment>> missEnchantmentSet) {
        EnchantmentNode node = EnchantmentUtils.bestStep(equipItemStack, missEnchantmentSet);
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
        return EnchantmentUtils.bestStep(equipNode, bookNodeSet);
    }

    public static EnchantmentNode bestStep(EnchantmentNode originEquipNode, Set<EnchantmentNode> bookNodeSet) {
        return EnchantmentUtils.bestStep(originEquipNode, bookNodeSet, false);
    }

    public static EnchantmentNode bestStep(EnchantmentNode originEquipNode, Set<EnchantmentNode> bookNodeSet, boolean fast) {
        List<EnchantmentNode> lessGroup;
        if (bookNodeSet.isEmpty()) {
            return null;
        }
        List<EnchantmentNode> bookList = new LinkedList<EnchantmentNode>(bookNodeSet);
        bookList.sort(null);
        Map<Integer, List<EnchantmentNode>> groups = new HashMap<Integer, List<EnchantmentNode>>();
        groups.put(0, bookList);
        int sup = bookList.size();
        EnchantmentNode equipNode = originEquipNode;
        while (sup > 0) {
            List<EnchantmentNode> group = groups.computeIfAbsent(equipNode.getRepairCost(), k -> new LinkedList());
            if (!group.isEmpty()) {
                EnchantmentNode right = group.removeFirst();
                equipNode = new EnchantmentMargeNode(equipNode, right);
                --sup;
                if (!fast) continue;
                return equipNode;
            }
            int margeQty = 0;
            for (int i = 0; i < equipNode.getRepairCost(); ++i) {
                lessGroup = groups.computeIfAbsent(i, k -> new LinkedList());
                List<EnchantmentNode> nextGroup = groups.computeIfAbsent(equipNode.getRepairCost(), k -> new LinkedList());
                while (lessGroup.size() >= 2) {
                    EnchantmentNode left = lessGroup.removeFirst();
                    EnchantmentNode right = lessGroup.removeLast();
                    EnchantmentMargeNode newNode = new EnchantmentMargeNode(left, right);
                    ++margeQty;
                    nextGroup.add(newNode);
                }
                nextGroup.sort(null);
            }
            if (margeQty <= 0) break;
            sup -= margeQty;
        }
        EnchantmentNode right = null;
        int repairCost = 0;
        while (sup > 1 && repairCost < 10) {
            List<EnchantmentNode> group = groups.get(repairCost);
            while (!group.isEmpty()) {
                if (right == null) {
                    right = group.removeLast();
                    continue;
                }
                EnchantmentNode left = group.removeFirst();
                right = new EnchantmentMargeNode(left, right);
                --sup;
            }
            ++repairCost;
        }
        if (sup > 0) {
            if (right == null) {
                for (Map.Entry<Integer, List<EnchantmentNode>> entry : groups.entrySet()) {
                    if (((List)entry.getValue()).isEmpty()) continue;
                    right = (EnchantmentNode)((List)entry.getValue()).removeFirst();
                    break;
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
        return EnchantmentUtils.getEnchantmentInstance(registryKey).map(Enchantment::getAnvilCost).orElseThrow();
    }

    public static int getMaxLevel(ResourceKey<Enchantment> registryKey) {
        return EnchantmentUtils.getEnchantmentInstance(registryKey).map(Enchantment::getMaxLevel).orElseThrow();
    }

    public static int getCost(ResourceKey<Enchantment> registryKey) {
        Enchantment enchantment = EnchantmentUtils.getEnchantmentInstance(registryKey).orElseThrow();
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
        if (penalty <= 0) {
            return 0;
        }
        return 32 - Integer.numberOfLeadingZeros(penalty);
    }

    public static int workToPenalty(int work) {
        if (work <= 0) {
            return 0;
        }
        return (1 << work) - 1;
    }
}
